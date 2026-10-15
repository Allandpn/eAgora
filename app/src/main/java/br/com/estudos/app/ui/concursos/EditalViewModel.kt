package br.com.estudos.app.ui.concursos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.estudos.app.data.ProvedorApi
import br.com.estudos.app.data.api.AnaliseEditalDto
import br.com.estudos.app.data.api.AssuntoConfirmadoDto
import br.com.estudos.app.data.api.ConfirmacaoEditalDto
import br.com.estudos.app.data.api.DisciplinaConfirmadaDto
import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.EscolhaCargoDto
import br.com.estudos.app.data.api.StatusAnalise
import br.com.estudos.app.data.api.TipoPeso
import br.com.estudos.app.data.api.chamar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Um assunto da proposta em revisão. */
data class AssuntoRevisao(
    val nome: String,
    val assuntoExistenteId: Long?,
    val assuntoExistenteNome: String?,
    val disciplinaAtualNome: String?,
    val incluir: Boolean = true,
)

data class DisciplinaRevisao(
    val nome: String,
    val questoes: Int?,
    val pesoDeterminado: Boolean,
    val disciplinaExistenteId: Long?,
    val disciplinaExistenteNome: String?,
    val peso: TipoPeso,
    val assuntos: List<AssuntoRevisao>,
)

/** A proposta como o usuário a está deixando (D-62: só a confirmação grava). */
data class Revisao(
    val nome: String,
    val cargo: String,
    val banca: String,
    val dataProva: String?,
    val disciplinas: List<DisciplinaRevisao>,
)

data class EditalUiState(
    val carregando: Boolean = false,
    val erro: String? = null,
    val erroAcao: String? = null,
    val enviando: Boolean = false,
    val analise: AnaliseEditalDto? = null,
    val cargo: String? = null,
    val revisao: Revisao? = null,
    /** Id da análise criada pelo envio — a tela troca para a rota dela. */
    val criada: Long? = null,
    val confirmada: Boolean = false,
) {
    val trabalhando get() = analise?.status == StatusAnalise.LENDO_EDITAL || analise?.status == StatusAnalise.SEPARANDO_ASSUNTOS
}

/** Resumo da revisão, recalculado conforme o usuário desmarca (mesmo cálculo da SPA, 04_FRONTEND §7J). */
fun resumoRevisao(revisao: Revisao, servidor: br.com.estudos.app.data.api.ResumoPropostaDto): String {
    val incluidos = revisao.disciplinas.flatMap { it.assuntos }.filter { it.incluir }
    val emComum = incluidos.count { it.assuntoExistenteId != null }
    val novos = incluidos.size - emComum
    val fora = servidor.foraDoEdital + (servidor.emComum - emComum)
    return "$novos ${if (novos == 1) "assunto novo" else "assuntos novos"} · $emComum em comum (mantêm escada e histórico) · " +
        "$fora fora do edital (continuam como estão)"
}

/** O corpo de confirmar: só os assuntos marcados. */
fun corpoConfirmacao(r: Revisao) = ConfirmacaoEditalDto(
    nome = r.nome.trim(),
    cargo = r.cargo.trim().ifEmpty { null },
    banca = r.banca.trim().ifEmpty { null },
    dataProva = r.dataProva,
    disciplinas = r.disciplinas.map { d ->
        DisciplinaConfirmadaDto(d.nome, d.disciplinaExistenteId, d.peso,
            d.assuntos.filter { it.incluir }.map { AssuntoConfirmadoDto(it.nome, it.assuntoExistenteId) })
    },
)

/**
 * Edital novo (02_JORNADAS J-5): envia o PDF, escolhe o cargo, revisa e
 * confirma. A IA e o peso são do servidor (D-61); o app só mostra e coleta.
 */
class EditalViewModel(
    private val analiseId: Long?,
    private val provedor: ProvedorApi,
    private val intervaloConsultaMs: Long = 3000,
) : ViewModel() {
    private val _estado = MutableStateFlow(EditalUiState())
    val estado: StateFlow<EditalUiState> = _estado
    private var consulta: Job? = null

    init {
        if (analiseId != null) carregar()
    }

    fun carregar() {
        val id = analiseId ?: return
        viewModelScope.launch {
            _estado.update { it.copy(carregando = it.analise == null, erro = null) }
            try {
                aplicar(chamar { provedor.api().analise(id) })
            } catch (e: ErroApi) {
                _estado.update { it.copy(erro = e.message) }
            } finally {
                _estado.update { it.copy(carregando = false) }
            }
        }
    }

    private fun aplicar(analise: AnaliseEditalDto) {
        _estado.update { e ->
            e.copy(
                analise = analise,
                revisao = e.revisao ?: if (analise.status == StatusAnalise.PRONTA) montarRevisao(analise) else null,
                cargo = e.cargo ?: analise.cargos.singleOrNull(),
            )
        }
        consulta?.cancel()
        if (_estado.value.trabalhando) {
            // Enquanto a IA trabalha, consulta; acaba sozinha quando a tela sai (viewModelScope).
            consulta = viewModelScope.launch {
                delay(intervaloConsultaMs)
                carregar()
            }
        }
    }

    private fun montarRevisao(a: AnaliseEditalDto) = Revisao(
        nome = a.concursoNome.orEmpty(),
        cargo = a.cargoEscolhido.orEmpty(),
        banca = a.banca.orEmpty(),
        dataProva = a.dataProva,
        disciplinas = a.proposta.orEmpty().map { d ->
            DisciplinaRevisao(d.nome, d.questoes, d.pesoDeterminado, d.disciplinaExistenteId, d.disciplinaExistenteNome, d.peso,
                d.assuntos.map { AssuntoRevisao(it.nome, it.assuntoExistenteId, it.assuntoExistenteNome, it.disciplinaAtualNome) })
        },
    )

    fun enviar(nomeArquivo: String, bytes: ByteArray) {
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, erroAcao = null) }
            try {
                val parte = MultipartBody.Part.createFormData(
                    "arquivo", nomeArquivo, bytes.toRequestBody("application/pdf".toMediaType()))
                val analise = chamar { provedor.api().enviarEdital(parte) }
                _estado.update { it.copy(enviando = false, criada = analise.id) }
            } catch (e: ErroApi) {
                _estado.update { it.copy(enviando = false, erroAcao = e.message) }
            }
        }
    }

    fun escolherCargo(cargo: String) = _estado.update { it.copy(cargo = cargo) }

    fun separarAssuntos() = acao { id -> chamar { provedor.api().escolherCargo(id, EscolhaCargoDto(_estado.value.cargo!!)) } }

    fun tentarDeNovo() = acao { id -> chamar { provedor.api().tentarDeNovo(id) } }

    private fun acao(chamada: suspend (Long) -> AnaliseEditalDto) {
        val id = analiseId ?: return
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, erroAcao = null) }
            try {
                aplicar(chamada(id))
            } catch (e: ErroApi) {
                _estado.update { it.copy(erroAcao = e.message) }
            } finally {
                _estado.update { it.copy(enviando = false) }
            }
        }
    }

    fun editar(transformar: (Revisao) -> Revisao) = _estado.update { e -> e.copy(revisao = e.revisao?.let(transformar)) }

    fun alternarAssunto(i: Int, j: Int) = editarAssunto(i, j) { it.copy(incluir = !it.incluir) }

    fun tratarComoNovo(i: Int, j: Int) = editarAssunto(i, j) { it.copy(assuntoExistenteId = null, assuntoExistenteNome = null) }

    fun trocarPeso(i: Int, peso: TipoPeso) = editar { r ->
        r.copy(disciplinas = r.disciplinas.mapIndexed { k, d -> if (k == i) d.copy(peso = peso) else d })
    }

    private fun editarAssunto(i: Int, j: Int, f: (AssuntoRevisao) -> AssuntoRevisao) = editar { r ->
        r.copy(disciplinas = r.disciplinas.mapIndexed { k, d ->
            if (k != i) d else d.copy(assuntos = d.assuntos.mapIndexed { m, a -> if (m == j) f(a) else a })
        })
    }

    fun confirmar() {
        val id = analiseId ?: return
        val revisao = _estado.value.revisao ?: return
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, erroAcao = null) }
            try {
                chamar { provedor.api().confirmarEdital(id, corpoConfirmacao(revisao)) }
                _estado.update { it.copy(enviando = false, confirmada = true) }
            } catch (e: ErroApi) {
                _estado.update { it.copy(enviando = false, erroAcao = e.message) }
            }
        }
    }
}
