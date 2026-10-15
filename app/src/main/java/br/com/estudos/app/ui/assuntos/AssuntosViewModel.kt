package br.com.estudos.app.ui.assuntos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.estudos.app.data.ItemAssunto
import br.com.estudos.app.data.ItemErro
import br.com.estudos.app.data.ItemRegistro
import br.com.estudos.app.data.ProvedorApi
import br.com.estudos.app.data.Selecao
import br.com.estudos.app.data.api.AssuntoDto
import br.com.estudos.app.data.api.DisciplinaDto
import br.com.estudos.app.data.api.DisciplinaResumoDto
import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.FaseAssunto
import br.com.estudos.app.data.api.M2SerieDto
import br.com.estudos.app.data.api.SegmentoDto
import br.com.estudos.app.data.api.chamar
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AssuntoComFase(val assunto: AssuntoDto, val fase: FaseAssunto)
data class GrupoDisciplina(val disciplina: DisciplinaDto, val resumo: DisciplinaResumoDto?, val assuntos: List<AssuntoComFase>)

data class AssuntosUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val grupos: List<GrupoDisciplina> = emptyList(),
    /** Disciplinas expandidas (02_JORNADAS §4.5: Assuntos abre pelas disciplinas). */
    val abertas: Set<Long> = emptySet(),
) {
    val vazio get() = !carregando && erro == null && grupos.isEmpty()
}

/** Assuntos (04_FRONTEND §7B): uma chamada de fase por assunto, como a SPA — a fase é derivada no servidor (D-16). */
class AssuntosViewModel(private val provedor: ProvedorApi, private val selecao: Selecao) : ViewModel() {
    private val _estado = MutableStateFlow(AssuntosUiState())
    val estado: StateFlow<AssuntosUiState> = _estado

    init { carregar() }

    fun carregar() {
        viewModelScope.launch {
            _estado.value = AssuntosUiState(carregando = true)
            _estado.value = try {
                val api = provedor.api()
                val resumos = chamar { api.disciplinasResumo() }.associateBy { it.id }
                val grupos = coroutineScope {
                    chamar { api.disciplinas() }.map { d ->
                        async {
                            val assuntos = chamar { api.assuntos(d.id) }
                            GrupoDisciplina(d, resumos[d.id], assuntos.map { a -> async { AssuntoComFase(a, chamar { api.fase(a.id) }.fase) } }.awaitAll())
                        }
                    }.awaitAll()
                }
                AssuntosUiState(carregando = false, grupos = grupos, abertas = _estado.value.abertas)
            } catch (e: ErroApi) {
                AssuntosUiState(carregando = false, erro = e.message)
            }
        }
    }

    fun alternar(disciplinaId: Long) {
        _estado.value = _estado.value.let { e ->
            e.copy(abertas = if (disciplinaId in e.abertas) e.abertas - disciplinaId else e.abertas + disciplinaId)
        }
    }

    fun selecionar(d: DisciplinaDto, a: AssuntoComFase) {
        selecao.assunto = ItemAssunto(a.assunto.id, a.assunto.nome, d.nome, a.assunto.peso, a.fase)
    }
}

data class DetalheUiState(
    val item: ItemAssunto? = null,
    val semDados: Boolean = false,
    val carregando: Boolean = true,
    val segmentos: List<SegmentoDto> = emptyList(),
    val retencao: List<M2SerieDto> = emptyList(),
)

/** Detalhe do assunto: segmentos e M-2 são auxiliares — falhar não bloqueia a tela (§7B). */
class AssuntoDetalheViewModel(
    private val assuntoId: Long,
    private val provedor: ProvedorApi,
    private val selecao: Selecao,
) : ViewModel() {
    private val _estado = MutableStateFlow(DetalheUiState())
    val estado: StateFlow<DetalheUiState> = _estado

    init {
        val item = selecao.assunto?.takeIf { it.assuntoId == assuntoId }
        if (item == null) {
            _estado.value = DetalheUiState(semDados = true, carregando = false)
        } else {
            _estado.value = DetalheUiState(item = item)
            viewModelScope.launch {
                val segmentos = runCatching { chamar { provedor.api().segmentos(assuntoId) } }.getOrDefault(emptyList())
                _estado.value = _estado.value.copy(segmentos = segmentos, carregando = false)
                val retencao = runCatching { chamar { provedor.api().m2(assuntoId) }.series }.getOrDefault(emptyList())
                _estado.value = _estado.value.copy(retencao = retencao)
            }
        }
    }

    fun selecionarRegistro() {
        val item = _estado.value.item ?: return
        selecao.registro = ItemRegistro(item.assuntoId, item.nome)
    }

    fun selecionarErros() {
        val item = _estado.value.item ?: return
        selecao.erro = ItemErro(item.assuntoId, item.nome)
    }
}
