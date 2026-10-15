package br.com.estudos.app.ui.hoje

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.estudos.app.data.ItemRecuperacao
import br.com.estudos.app.data.ItemRegistro
import br.com.estudos.app.data.PlanoCache
import br.com.estudos.app.data.ProvedorApi
import br.com.estudos.app.data.Selecao
import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.FilaRecuperacaoItemDto
import br.com.estudos.app.data.api.JsonSge
import br.com.estudos.app.data.api.ReestudoItemDto
import br.com.estudos.app.data.api.TurnoPlanoDto
import br.com.estudos.app.data.api.chamar
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HojeUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val plano: TurnoPlanoDto? = null,
    /** Instante do plano guardado, quando o que aparece veio do cache (especificação §5.4). */
    val planoGuardadoEm: Long? = null,
    val mostrarAntecipaveis: Boolean = false,
) {
    private val p get() = plano ?: TurnoPlanoDto()
    val vazio get() = plano != null && p.filaRecuperacao.isEmpty() && p.reestudos.isEmpty() && p.blocoConteudo == null

    /** J-4: dois dias ou mais sem sessão (limiar da SPA, SPRINT-17 §6.3 do SGE). */
    val mostrarRetorno get() = (p.diasSemEstudar ?: -1) >= 2
    val temAdiantar get() = p.vencidasAlemDoTeto > 0 || p.proximoBloco != null || p.antecipaveis.isNotEmpty()
}

/**
 * Hoje (J-2, J-4): mostra o plano que o servidor monta — nada é decidido aqui.
 * "Drenar mais" (02_JORNADAS §4.3) soma um teto diário por pedido, só nesta
 * visita; voltar para Hoje recomeça do teto normal (D-40).
 */
class HojeViewModel(
    private val provedor: ProvedorApi,
    private val cache: PlanoCache,
    private val selecao: Selecao,
    private val agora: () -> Long,
) : ViewModel() {
    private val _estado = MutableStateFlow(HojeUiState())
    val estado: StateFlow<HojeUiState> = _estado

    private var extraRecuperacoes = 0
    private var passoDrenar = 0
    private var carga: Job? = null

    /** Cada entrada na tela: recomeça do teto normal. */
    fun abrir() {
        extraRecuperacoes = 0
        _estado.update { it.copy(mostrarAntecipaveis = false) }
        carregar()
    }

    fun drenarMais() {
        extraRecuperacoes += passoDrenar
        carregar()
    }

    fun alternarAntecipaveis() = _estado.update { it.copy(mostrarAntecipaveis = !it.mostrarAntecipaveis) }

    fun carregar() {
        carga?.cancel()
        carga = viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }
            try {
                val plano = chamar { provedor.api().plano(extraRecuperacoes) }
                if (extraRecuperacoes == 0) {
                    passoDrenar = plano.filaRecuperacao.size // a fila sem pedido = o teto diário
                    cache.salvar(JsonSge.encodeToString(TurnoPlanoDto.serializer(), plano), agora())
                }
                _estado.update { it.copy(carregando = false, plano = plano, planoGuardadoEm = null) }
            } catch (e: ErroApi) {
                val guardado = if (e.semConexao) lerCache() else null
                _estado.update {
                    if (guardado != null) {
                        it.copy(carregando = false, plano = guardado.first, planoGuardadoEm = guardado.second)
                    } else {
                        it.copy(carregando = false, erro = e.message, plano = null, planoGuardadoEm = null)
                    }
                }
            }
        }
    }

    private suspend fun lerCache(): Pair<TurnoPlanoDto, Long>? {
        val (json, instante) = cache.ler() ?: return null
        val plano = runCatching { JsonSge.decodeFromString(TurnoPlanoDto.serializer(), json) }.getOrNull() ?: return null
        return plano to instante
    }

    fun selecionarRecuperacao(item: FilaRecuperacaoItemDto) {
        selecao.recuperacao = ItemRecuperacao(item.assuntoId, item.nome, item.nivel, item.dataPrevista)
    }

    /** D-55: o card leva o segmento sugerido para Registrar já vir com ele marcado. */
    fun selecionarBloco(outro: Boolean): Long? {
        val p = _estado.value.plano ?: return null
        val assunto = p.blocoConteudo ?: return null
        val segmento = if (outro) p.proximoBloco else p.segmentoSugerido
        selecao.registro = ItemRegistro(assunto.id, assunto.nome, segmento?.id)
        return assunto.id
    }

    /** D-07/D-11: reestudo é uma sessão ESTUDO do mesmo assunto, na tela de Registrar conteúdo. */
    fun selecionarReestudo(item: ReestudoItemDto) {
        selecao.registro = ItemRegistro(item.assuntoId, item.nome)
    }
}
