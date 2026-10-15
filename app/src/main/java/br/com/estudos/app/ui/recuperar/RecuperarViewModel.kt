package br.com.estudos.app.ui.recuperar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.estudos.app.data.EnvioRegistros
import br.com.estudos.app.data.ItemErro
import br.com.estudos.app.data.Rascunhos
import br.com.estudos.app.data.Selecao
import br.com.estudos.app.data.api.JsonSge
import br.com.estudos.app.data.api.ResultadoSessao
import br.com.estudos.app.data.api.SessaoRequestDto
import br.com.estudos.app.data.api.TipoSessao
import br.com.estudos.app.ui.Rotas
import br.com.estudos.app.ui.comum.VALIDADE_RASCUNHO_MS
import br.com.estudos.app.ui.comum.hojeIso
import br.com.estudos.app.ui.comum.novaTentativa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.roundToLong

/** O que sobrevive ao Android fechar o app no meio da recuperação (especificação §5.3). */
@Serializable
data class RascunhoRecuperacao(
    val assuntoId: Long,
    val nome: String,
    val nivel: Int,
    val dataPrevista: String? = null,
    val etapa: Int = 1,
    val previsao: ResultadoSessao? = null,
    val tentativaId: String,
    /** Instante em que a tela abriu: o tempo medido continua honesto depois de reabrir. */
    val inicioEm: Long,
)

sealed interface RecuperarUiState {
    data object Carregando : RecuperarUiState
    data object SemDados : RecuperarUiState
    data class Aberta(val rascunho: RascunhoRecuperacao) : RecuperarUiState
}

fun chaveRascunhoRecuperacao(assuntoId: Long) = "recuperacao:$assuntoId"

/**
 * Recuperar (02_JORNADAS §4.1): previsão antes, resultado depois. O tentativaId
 * nasce ao abrir e fica no rascunho (D-45); o envio é otimista (§5.2).
 */
class RecuperarViewModel(
    private val assuntoId: Long,
    private val selecao: Selecao,
    private val rascunhos: Rascunhos,
    private val envio: EnvioRegistros,
    private val agora: () -> Long,
) : ViewModel() {
    private val _estado = MutableStateFlow<RecuperarUiState>(RecuperarUiState.Carregando)
    val estado: StateFlow<RecuperarUiState> = _estado
    private val chave = chaveRascunhoRecuperacao(assuntoId)

    init {
        viewModelScope.launch { _estado.value = abrir() }
    }

    private suspend fun abrir(): RecuperarUiState {
        val guardado = rascunhos.ler(chave)
            ?.let { runCatching { JsonSge.decodeFromString(RascunhoRecuperacao.serializer(), it) }.getOrNull() }
        if (guardado != null && agora() - guardado.inicioEm < VALIDADE_RASCUNHO_MS) {
            return RecuperarUiState.Aberta(guardado)
        }
        if (guardado != null) rascunhos.apagar(chave)
        val item = selecao.recuperacao?.takeIf { it.assuntoId == assuntoId } ?: return RecuperarUiState.SemDados
        val novo = RascunhoRecuperacao(
            assuntoId, item.nome, item.nivel, item.dataPrevista,
            tentativaId = novaTentativa(), inicioEm = agora(),
        )
        salvar(novo)
        return RecuperarUiState.Aberta(novo)
    }

    private suspend fun salvar(r: RascunhoRecuperacao) =
        rascunhos.salvar(chave, JsonSge.encodeToString(RascunhoRecuperacao.serializer(), r))

    fun prever(previsao: ResultadoSessao) {
        val atual = (_estado.value as? RecuperarUiState.Aberta)?.rascunho ?: return
        if (atual.etapa != 1) return
        val novo = atual.copy(etapa = 2, previsao = previsao)
        _estado.value = RecuperarUiState.Aberta(novo)
        viewModelScope.launch { salvar(novo) }
    }

    /** Envia e devolve true; a tela volta para Hoje na hora (otimista). */
    fun confirmar(resultado: ResultadoSessao): Boolean {
        val r = (_estado.value as? RecuperarUiState.Aberta)?.rascunho ?: return false
        if (r.etapa != 2 || r.previsao == null) return false
        // D-29: o tempo da recuperação é o da tela (abrir → confirmar), mínimo 1.
        val tempo = max(1L, ((agora() - r.inicioEm) / 60000.0).roundToLong()).toInt()
        val corpo = SessaoRequestDto(
            assuntoId = r.assuntoId,
            tipo = TipoSessao.RECUPERACAO,
            data = hojeIso(),
            tempoMinutos = tempo,
            previsaoReconstrucao = r.previsao,
            resultado = resultado,
            tentativaId = r.tentativaId,
        )
        envio.enviar(corpo, chave, "a recuperação de ${r.nome}", Rotas.recuperar(r.assuntoId))
        return true
    }

    /** Voltar sai sem registrar: o rascunho só sobrevive ao Android fechar o app ou a uma falha de envio. */
    fun sair() = envio.descartarRascunho(chave)

    fun selecionarErro() {
        val r = (_estado.value as? RecuperarUiState.Aberta)?.rascunho ?: return
        selecao.erro = ItemErro(r.assuntoId, r.nome)
    }
}
