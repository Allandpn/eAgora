package br.com.estudos.app.ui.configuracao

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import br.com.estudos.app.AppContainer
import br.com.estudos.app.BuildConfig
import br.com.estudos.app.data.Configuracao
import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.SgeApi
import br.com.estudos.app.data.api.chamar
import br.com.estudos.app.data.api.criarApi
import br.com.estudos.app.ui.Rotas
import br.com.estudos.app.ui.comum.CaixaErro
import br.com.estudos.app.ui.comum.Pagina
import br.com.estudos.app.ui.tema.LocalCoresExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

data class ConfiguracaoUiState(
    val url: String = "",
    val testando: Boolean = false,
    val erro: String? = null,
    val salvo: Boolean = false,
)

/**
 * Normaliza e confere o endereço digitado (especificação §3.8): em release só
 * `https://` (03E_DEPLOYMENT §8A do SGE); em debug, `http://` também.
 * Devolve o endereço sem barra final, ou a mensagem de erro.
 */
fun validarEndereco(digitado: String, aceitaHttp: Boolean): Result<String> {
    val url = digitado.trim().trimEnd('/')
    val https = url.startsWith("https://")
    val http = url.startsWith("http://")
    return when {
        url.isEmpty() -> Result.failure(IllegalArgumentException("Informe o endereço do servidor."))
        http && !aceitaHttp -> Result.failure(IllegalArgumentException("Use https:// — o servidor só responde por HTTPS (tailscale serve)."))
        !https && !http -> Result.failure(IllegalArgumentException("O endereço começa com https://"))
        url.length <= "https://".length -> Result.failure(IllegalArgumentException("Informe o endereço do servidor."))
        else -> Result.success(url)
    }
}

class ConfiguracaoViewModel(
    private val configuracao: Configuracao,
    private val fabrica: (String) -> SgeApi = { criarApi(it) },
    private val aceitaHttp: Boolean = BuildConfig.DEBUG,
) : ViewModel() {
    private val _estado = MutableStateFlow(ConfiguracaoUiState())
    val estado: StateFlow<ConfiguracaoUiState> = _estado

    init {
        viewModelScope.launch {
            configuracao.urlServidorAtual()?.let { atual -> _estado.update { it.copy(url = atual) } }
        }
    }

    fun mudarUrl(v: String) = _estado.update { it.copy(url = v, erro = null) }

    /** Trocar o endereço exige novo teste: só grava depois de GET /actuator/health responder UP. */
    fun testarESalvar() {
        val url = validarEndereco(_estado.value.url, aceitaHttp).getOrElse { e ->
            _estado.update { it.copy(erro = e.message) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(testando = true, erro = null) }
            try {
                val saude = chamar { fabrica(url).saude() }
                val status = (saude["status"] as? JsonPrimitive)?.content
                if (status != "UP") throw ErroApi("O servidor respondeu, mas não está saudável (${status ?: "sem status"}).")
                configuracao.salvarUrlServidor(url)
                _estado.update { it.copy(testando = false, url = url, salvo = true) }
            } catch (e: ErroApi) {
                _estado.update { it.copy(testando = false, erro = e.message) }
            } catch (e: IllegalArgumentException) {
                _estado.update { it.copy(testando = false, erro = "Endereço inválido.") }
            }
        }
    }
}

@Composable
fun TelaConfiguracao(container: AppContainer, nav: NavHostController) {
    val vm = br.com.estudos.app.ui.comum.lembrarViewModel { ConfiguracaoViewModel(container.armazenamento) }
    val e by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(e.salvo) {
        if (e.salvo) nav.navigate(Rotas.HOJE) { popUpTo(0) }
    }

    Pagina {
        Text("Servidor do SGE", style = MaterialTheme.typography.titleLarge)
        Text(
            "O endereço do SGE na sua Tailscale, por exemplo https://raspberrypi.tail1234.ts.net. A Tailscale precisa estar ligada neste aparelho.",
            color = LocalCoresExtras.current.texto2,
        )
        e.erro?.let { CaixaErro(it) }
        OutlinedTextField(
            value = e.url, onValueChange = vm::mudarUrl, label = { Text("Endereço") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = vm::testarESalvar, enabled = !e.testando, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (e.testando) "Testando…" else "Testar e salvar")
        }
    }
}
