package br.com.estudos.app.data

import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.SessaoRequestDto
import br.com.estudos.app.data.api.chamar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Um registro que falhou e pode ser reaberto com o rascunho intacto. */
data class FalhaEnvio(
    val chaveRascunho: String,
    val descricao: String,
    val rota: String,
    val mensagem: String,
)

/**
 * Envio otimista num escopo do aplicativo (A-006, especificação §5.2): a tela
 * entrega o registro e volta para Hoje; o envio termina mesmo sem a tela. O
 * rascunho só é apagado depois do sucesso; o tentativaId garante que reenviar
 * nunca duplica (D-45).
 */
class EnvioRegistros(
    private val escopo: CoroutineScope,
    private val provedor: ProvedorApi,
    private val rascunhos: Rascunhos,
) {
    private val _falhas = MutableStateFlow<List<FalhaEnvio>>(emptyList())
    val falhas: StateFlow<List<FalhaEnvio>> = _falhas

    /** Incrementa a cada registro gravado — Hoje recarrega o plano quando muda. */
    private val _gravados = MutableStateFlow(0)
    val gravados: StateFlow<Int> = _gravados

    fun enviar(corpo: SessaoRequestDto, chaveRascunho: String, descricao: String, rota: String) {
        _falhas.update { lista -> lista.filterNot { it.chaveRascunho == chaveRascunho } }
        escopo.launch {
            try {
                chamar { provedor.api().registrarSessao(corpo) }
                rascunhos.apagar(chaveRascunho)
                _gravados.update { it + 1 }
            } catch (e: ErroApi) {
                _falhas.update { it + FalhaEnvio(chaveRascunho, descricao, rota, e.message ?: "Falha ao registrar") }
            }
        }
    }

    /** Saída explícita da tela (voltar): apaga o rascunho num escopo que sobrevive à tela. */
    fun descartarRascunho(chaveRascunho: String) {
        escopo.launch { rascunhos.apagar(chaveRascunho) }
    }

    fun descartarFalha(chaveRascunho: String) {
        _falhas.update { lista -> lista.filterNot { it.chaveRascunho == chaveRascunho } }
    }
}
