package br.com.estudos.app.ui.erros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import br.com.estudos.app.AppContainer
import br.com.estudos.app.data.ProvedorApi
import br.com.estudos.app.data.Selecao
import br.com.estudos.app.data.api.CausaErro
import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.ErroDto
import br.com.estudos.app.data.api.ErroRequestDto
import br.com.estudos.app.data.api.NivelConfianca
import br.com.estudos.app.data.api.chamar
import br.com.estudos.app.ui.Rotas
import br.com.estudos.app.ui.comum.CaixaErro
import br.com.estudos.app.ui.comum.Carregando
import br.com.estudos.app.ui.comum.CartaoItem
import br.com.estudos.app.ui.comum.Pagina
import br.com.estudos.app.ui.comum.SemDados
import br.com.estudos.app.ui.comum.TituloSecao
import br.com.estudos.app.ui.comum.lembrarViewModel
import br.com.estudos.app.ui.comum.rotulo
import br.com.estudos.app.ui.irParaAba
import br.com.estudos.app.ui.tema.LocalCoresExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ErrosUiState(
    val nome: String? = null,
    val semDados: Boolean = false,
    val descricao: String = "",
    val causa: CausaErro? = null,
    val confianca: NivelConfianca? = null,
    val enviando: Boolean = false,
    val erroEnvio: String? = null,
    val carregandoLista: Boolean = true,
    val erros: List<ErroDto> = emptyList(),
)

/** Erros (04_FRONTEND §7C): não é otimista, de propósito — espera o POST, limpa e recarrega. */
class ErrosViewModel(
    private val assuntoId: Long,
    private val provedor: ProvedorApi,
    selecao: Selecao,
) : ViewModel() {
    private val _estado = MutableStateFlow(ErrosUiState())
    val estado: StateFlow<ErrosUiState> = _estado

    init {
        val item = selecao.erro?.takeIf { it.assuntoId == assuntoId }
        if (item == null) {
            _estado.value = ErrosUiState(semDados = true)
        } else {
            _estado.value = ErrosUiState(nome = item.nome)
            viewModelScope.launch { carregarLista() }
        }
    }

    private suspend fun carregarLista() {
        _estado.update { it.copy(carregandoLista = true) }
        val erros = runCatching { chamar { provedor.api().erros(assuntoId) } }.getOrDefault(emptyList())
        _estado.update { it.copy(carregandoLista = false, erros = erros) }
    }

    fun mudarDescricao(v: String) = _estado.update { it.copy(descricao = v) }
    fun escolherCausa(c: CausaErro) = _estado.update { it.copy(causa = c) }
    fun escolherConfianca(c: NivelConfianca) = _estado.update { it.copy(confianca = c) }

    fun registrar() {
        val e = _estado.value
        if (e.enviando) return
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, erroEnvio = null) }
            try {
                chamar { provedor.api().registrarErro(ErroRequestDto(assuntoId, null, e.descricao, e.causa, e.confianca)) }
                _estado.update { it.copy(descricao = "", causa = null, confianca = null, enviando = false) }
                carregarLista()
            } catch (x: ErroApi) {
                _estado.update { it.copy(enviando = false, erroEnvio = x.message) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelaErros(container: AppContainer, nav: NavHostController, assuntoId: Long) {
    val vm = lembrarViewModel { ErrosViewModel(assuntoId, container.provedorApi, container.selecao) }
    val e by vm.estado.collectAsStateWithLifecycle()

    Pagina(Modifier.verticalScroll(rememberScrollState())) {
        if (e.semDados) {
            SemDados("Não foi possível abrir os erros deste assunto.", "Voltar para Assuntos") { irParaAba(nav, Rotas.ASSUNTOS) }
            return@Pagina
        }
        Text(e.nome.orEmpty(), style = MaterialTheme.typography.titleLarge)
        e.erroEnvio?.let { CaixaErro(it) }
        OutlinedTextField(
            value = e.descricao, onValueChange = vm::mudarDescricao, label = { Text("O que aconteceu?") },
            minLines = 3, modifier = Modifier.fillMaxWidth(),
        )
        Column {
            TituloSecao("Causa")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CausaErro.entries.forEach { c ->
                    FilterChip(selected = e.causa == c, onClick = { vm.escolherCausa(c) }, label = { Text(c.rotulo) })
                }
            }
        }
        Column {
            TituloSecao("Você tinha certeza quando errou?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NivelConfianca.entries.forEach { c ->
                    FilterChip(selected = e.confianca == c, onClick = { vm.escolherConfianca(c) }, label = { Text(c.rotulo) })
                }
            }
        }
        Button(onClick = vm::registrar, enabled = !e.enviando, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Registrar")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TituloSecao("Erros deste assunto")
            when {
                e.carregandoLista -> Carregando()
                e.erros.isEmpty() -> Text("Nenhum erro registrado ainda.", color = LocalCoresExtras.current.texto2)
                else -> e.erros.forEach { erro ->
                    CartaoItem(erro.descricao, "${erro.causa.rotulo} · confiança ${erro.confianca.rotulo.lowercase()}")
                }
            }
        }
    }
}
