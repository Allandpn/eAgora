package br.com.estudos.app.ui.registrar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.estudos.app.data.EnvioRegistros
import br.com.estudos.app.data.ProvedorApi
import br.com.estudos.app.data.Rascunhos
import br.com.estudos.app.data.Selecao
import br.com.estudos.app.data.api.FormatoBanca
import br.com.estudos.app.data.api.JsonSge
import br.com.estudos.app.data.api.SegmentoDto
import br.com.estudos.app.data.api.SessaoRequestDto
import br.com.estudos.app.data.api.TipoSessao
import br.com.estudos.app.data.api.chamar
import br.com.estudos.app.ui.Rotas
import br.com.estudos.app.ui.comum.VALIDADE_RASCUNHO_MS
import br.com.estudos.app.ui.comum.hojeIso
import br.com.estudos.app.ui.comum.novaTentativa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Tipos da tela, os mesmos da rota e da SPA: `conteudo`, `questoes`, `flashcards`. */
val TIPOS_REGISTRO = listOf("conteudo", "questoes", "flashcards")

/** Campos de Registrar como texto, exatamente o que está na tela (especificação §5.3). */
@Serializable
data class RascunhoRegistro(
    val assuntoId: Long,
    val nome: String,
    val tipo: String,
    val segmentoId: Long? = null,
    val formato: FormatoBanca = FormatoBanca.MULTIPLA_ESCOLHA,
    val feitas: String = "",
    val certas: String = "",
    val previsao: String = "",
    val tempo: String = "",
    val tempoEditado: Boolean = false,
    val tentativaId: String,
    val criadoEm: Long,
)

data class RegistrarUiState(
    val carregando: Boolean = true,
    val semDados: Boolean = false,
    val rascunho: RascunhoRegistro? = null,
    val segmentos: List<SegmentoDto> = emptyList(),
    val erro: String? = null,
)

fun chaveRascunhoRegistro(assuntoId: Long) = "registro:$assuntoId"

/**
 * Registrar sessão (04_FRONTEND §7A, §7G, §7H do SGE). Só validação de forma
 * aqui; regra de domínio é do servidor e o `detail` dele aparece como veio.
 */
class RegistrarViewModel(
    tipoInicial: String,
    private val assuntoId: Long,
    private val selecao: Selecao,
    private val provedor: ProvedorApi,
    private val rascunhos: Rascunhos,
    private val envio: EnvioRegistros,
    private val agora: () -> Long,
) : ViewModel() {
    private val _estado = MutableStateFlow(RegistrarUiState())
    val estado: StateFlow<RegistrarUiState> = _estado
    private val chave = chaveRascunhoRegistro(assuntoId)
    private var duracaoBloco: Int? = null

    init {
        viewModelScope.launch { abrir(if (tipoInicial in TIPOS_REGISTRO) tipoInicial else "conteudo") }
    }

    private suspend fun abrir(tipo: String) {
        val guardado = rascunhos.ler(chave)
            ?.let { runCatching { JsonSge.decodeFromString(RascunhoRegistro.serializer(), it) }.getOrNull() }
        // Rascunho de outro tipo não é reaproveitado: o mesmo tentativaId com outro
        // tipo faria o servidor devolver o registro antigo (D-45) e perder o novo.
        val valido = guardado?.takeIf { agora() - it.criadoEm < VALIDADE_RASCUNHO_MS && it.tipo == tipo }
        if (guardado != null && valido == null) rascunhos.apagar(chave)
        val item = selecao.registro?.takeIf { it.assuntoId == assuntoId }
        val rascunho = valido ?: item?.let {
            RascunhoRegistro(assuntoId, it.nome, tipo, tentativaId = novaTentativa(), criadoEm = agora())
        }
        if (rascunho == null) {
            _estado.value = RegistrarUiState(carregando = false, semDados = true)
            return
        }
        _estado.value = RegistrarUiState(carregando = false, rascunho = rascunho)
        salvar(rascunho)

        // Auxiliares: falhar não bloqueia o registro, o campo só não aparece (§7A).
        viewModelScope.launch {
            duracaoBloco = runCatching { chamar { provedor.api().parametros() } }.getOrNull()
                ?.firstOrNull { it.chave == "duracao_bloco_min" }?.valor?.toIntOrNull()
            atualizar { sugerirTempo(it) }
        }
        val segmentos = runCatching { chamar { provedor.api().segmentos(assuntoId) } }.getOrDefault(emptyList())
        _estado.update { it.copy(segmentos = segmentos) }
        // D-55: vindo do card de Hoje, o segmento sugerido já vem marcado.
        val sugerido = item?.segmentoSugeridoId?.takeIf { id -> valido == null && segmentos.any { it.id == id } }
        atualizar { sugerirTempo(if (sugerido != null) it.copy(segmentoId = sugerido) else it) }
    }

    /**
     * D-29 / 01_DOMINIO §7.7: em conteúdo, o tempo vem da estimativa do bloco ou da
     * duração parametrizada; depois que o usuário edita, nada sobrescreve.
     */
    private fun sugerirTempo(r: RascunhoRegistro): RascunhoRegistro {
        if (r.tempoEditado) return r
        if (r.tipo != "conteudo") return r.copy(tempo = "")
        val segmento = _estado.value.segmentos.firstOrNull { it.id == r.segmentoId }
        return r.copy(tempo = (segmento?.tempoEstimadoMin ?: duracaoBloco)?.toString() ?: "")
    }

    private fun atualizar(transformar: (RascunhoRegistro) -> RascunhoRegistro) {
        val atual = _estado.value.rascunho ?: return
        val novo = transformar(atual)
        if (novo == atual) return
        _estado.update { it.copy(rascunho = novo) }
        viewModelScope.launch { salvar(novo) }
    }

    private suspend fun salvar(r: RascunhoRegistro) =
        rascunhos.salvar(chave, JsonSge.encodeToString(RascunhoRegistro.serializer(), r))

    /** Troca de forma, nunca de rota (§7A). Tipo novo é tentativa nova. */
    fun selecionarTipo(tipo: String) = atualizar {
        if (it.tipo == tipo) it
        else sugerirTempo(RascunhoRegistro(it.assuntoId, it.nome, tipo, tentativaId = novaTentativa(), criadoEm = agora()))
    }

    fun selecionarSegmento(id: Long?) = atualizar { sugerirTempo(it.copy(segmentoId = id)) }
    fun selecionarFormato(formato: FormatoBanca) = atualizar { it.copy(formato = formato) }
    fun mudarFeitas(v: String) = atualizar { it.copy(feitas = soDigitos(v)) }
    fun mudarCertas(v: String) = atualizar { it.copy(certas = soDigitos(v)) }
    fun mudarPrevisao(v: String) = atualizar { it.copy(previsao = soDigitos(v)) }
    fun mudarTempo(v: String) = atualizar { it.copy(tempo = soDigitos(v), tempoEditado = true) }

    private fun soDigitos(v: String) = v.filter(Char::isDigit).take(6)

    /** Valida a forma e entrega o registro ao envio otimista; true = voltar para Hoje. */
    fun registrar(): Boolean {
        val r = _estado.value.rascunho ?: return false
        val tempo = r.tempo.toIntOrNull()
        if (tempo == null || tempo < 1) {
            _estado.update { it.copy(erro = "Informe o tempo em minutos.") }
            return false
        }
        _estado.update { it.copy(erro = null) }
        val corpo = corpoDe(r, tempo, hojeIso())
        envio.enviar(corpo, chave, "a sessão de ${r.nome}", Rotas.registrar(r.tipo, r.assuntoId))
        return true
    }

    fun sair() = envio.descartarRascunho(chave)
}

/** O corpo de POST /api/sessoes, o mesmo que a SPA monta para cada tipo. */
fun corpoDe(r: RascunhoRegistro, tempo: Int, data: String): SessaoRequestDto =
    if (r.tipo == "conteudo") {
        SessaoRequestDto(r.assuntoId, TipoSessao.ESTUDO, data, tempo, tentativaId = r.tentativaId, segmentoId = r.segmentoId)
    } else {
        SessaoRequestDto(
            assuntoId = r.assuntoId,
            tipo = if (r.tipo == "questoes") TipoSessao.QUESTOES else TipoSessao.FLASHCARDS,
            data = data,
            tempoMinutos = tempo,
            questoesTotal = r.feitas.toIntOrNull(),
            questoesCorretas = r.certas.toIntOrNull(),
            formato = if (r.tipo == "questoes") r.formato else null,
            previsaoPercentual = r.previsao.toIntOrNull(),
            tentativaId = r.tentativaId,
        )
    }

