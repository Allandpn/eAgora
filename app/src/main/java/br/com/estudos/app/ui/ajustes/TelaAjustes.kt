package br.com.estudos.app.ui.ajustes

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import br.com.estudos.app.AppContainer
import br.com.estudos.app.data.LembreteConfig
import br.com.estudos.app.data.ProvedorApi
import br.com.estudos.app.data.api.AtualizarParametroDto
import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.ParametroDto
import br.com.estudos.app.data.api.chamar
import br.com.estudos.app.ui.Rotas
import br.com.estudos.app.ui.comum.CaixaErro
import br.com.estudos.app.ui.comum.Carregando
import br.com.estudos.app.ui.comum.ORDEM_GRUPOS
import br.com.estudos.app.ui.comum.Pagina
import br.com.estudos.app.ui.comum.TituloSecao
import br.com.estudos.app.ui.comum.grupoParametro
import br.com.estudos.app.ui.comum.lembrarViewModel
import br.com.estudos.app.ui.comum.rotuloParametro
import br.com.estudos.app.ui.tema.LocalCoresExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AjustesUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val parametros: List<ParametroDto> = emptyList(),
    val rascunho: Map<String, String> = emptyMap(),
    val salvando: Set<String> = emptySet(),
    val erroPorChave: Map<String, String> = emptyMap(),
)

/** Ajustes (04_FRONTEND §7E): cada linha salva sozinha, sem otimismo; erro volta o valor anterior. */
class AjustesViewModel(private val provedor: ProvedorApi) : ViewModel() {
    private val _estado = MutableStateFlow(AjustesUiState())
    val estado: StateFlow<AjustesUiState> = _estado

    init { carregar() }

    fun carregar() {
        viewModelScope.launch {
            _estado.value = AjustesUiState(carregando = true)
            _estado.value = try {
                val ps = chamar { provedor.api().parametros() }
                AjustesUiState(carregando = false, parametros = ps, rascunho = ps.associate { it.chave to it.valor })
            } catch (e: ErroApi) {
                AjustesUiState(carregando = false, erro = e.message)
            }
        }
    }

    fun editar(chave: String, valor: String) = _estado.update { it.copy(rascunho = it.rascunho + (chave to valor)) }

    fun salvar(chave: String) {
        val atual = _estado.value
        val p = atual.parametros.firstOrNull { it.chave == chave } ?: return
        val novo = atual.rascunho[chave]?.trim() ?: return
        if (novo == p.valor || chave in atual.salvando) return
        viewModelScope.launch {
            _estado.update { it.copy(salvando = it.salvando + chave, erroPorChave = it.erroPorChave - chave) }
            try {
                val atualizado = chamar { provedor.api().atualizarParametro(chave, AtualizarParametroDto(novo)) }
                _estado.update { s ->
                    s.copy(
                        parametros = s.parametros.map { if (it.chave == chave) atualizado else it },
                        rascunho = s.rascunho + (chave to atualizado.valor),
                        salvando = s.salvando - chave,
                    )
                }
            } catch (e: ErroApi) {
                _estado.update { s ->
                    s.copy(
                        rascunho = s.rascunho + (chave to p.valor),
                        salvando = s.salvando - chave,
                        erroPorChave = s.erroPorChave + (chave to (e.message ?: "Não foi possível salvar")),
                    )
                }
            }
        }
    }
}

@Composable
fun TelaAjustes(container: AppContainer, nav: NavHostController) {
    val vm = lembrarViewModel { AjustesViewModel(container.provedorApi) }
    val e by vm.estado.collectAsStateWithLifecycle()

    Pagina(Modifier.verticalScroll(rememberScrollState())) {
        when {
            e.carregando -> Carregando()
            e.erro != null -> CaixaErro(e.erro!!) { vm.carregar() }
            else -> {
                val porGrupo = e.parametros.groupBy { grupoParametro(it.chave) }
                ORDEM_GRUPOS.filter { it in porGrupo }.forEach { grupo ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TituloSecao(grupo)
                        porGrupo.getValue(grupo).forEach { p ->
                            LinhaParametro(
                                rotulo = rotuloParametro(p.chave),
                                valor = e.rascunho[p.chave].orEmpty(),
                                erro = e.erroPorChave[p.chave],
                                salvando = p.chave in e.salvando,
                                aoEditar = { vm.editar(p.chave, it) },
                                aoConfirmar = { vm.salvar(p.chave) },
                            )
                        }
                    }
                }
            }
        }
        SecaoLembrete(container)
        SecaoServidor(container, nav)
    }
}

@Composable
private fun LinhaParametro(
    rotulo: String, valor: String, erro: String?, salvando: Boolean,
    aoEditar: (String) -> Unit, aoConfirmar: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(rotulo)
            erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        // Salva ao confirmar no teclado ou ao sair do campo, como o @change da SPA.
        var tinhaFoco by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = valor, onValueChange = aoEditar, enabled = !salvando, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { aoConfirmar() }),
            modifier = Modifier.width(110.dp).onFocusChanged {
                if (tinhaFoco && !it.isFocused) aoConfirmar()
                tinhaFoco = it.isFocused
            },
        )
    }
}

/** Lembrete do turno (especificação §6, 02_JORNADAS §2.2): desligado por padrão. */
@Composable
private fun SecaoLembrete(container: AppContainer) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val config by container.armazenamento.lembrete.collectAsState(initial = LembreteConfig())
    var aviso by remember { mutableStateOf<String?>(null) }

    fun aplicar(nova: LembreteConfig) {
        escopo.launch {
            container.armazenamento.salvarLembrete(nova)
            container.agendadorLembrete.agendar(nova)
        }
    }

    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) {
            aviso = null
            aplicar(config.copy(ativo = true))
        } else {
            aviso = "Sem permissão de notificação o lembrete não aparece. Libere em Configurações do Android."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSecao("Lembrete")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Lembrete do turno")
                Text(
                    "Uma notificação por dia dizendo o que há para hoje. Não aparece se você já estudou.",
                    style = MaterialTheme.typography.bodySmall, color = LocalCoresExtras.current.texto2,
                )
            }
            Switch(checked = config.ativo, onCheckedChange = { ligar ->
                if (!ligar) {
                    aplicar(config.copy(ativo = false))
                } else if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    aplicar(config.copy(ativo = true))
                }
            })
        }
        if (config.ativo) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Horário", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, h, m -> aplicar(config.copy(hora = h, minuto = m)) }, config.hora, config.minuto, true).show()
                }) { Text("%02d:%02d".format(config.hora, config.minuto)) }
            }
        }
        aviso?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun SecaoServidor(container: AppContainer, nav: NavHostController) {
    val url by container.armazenamento.urlServidor.collectAsState(initial = null)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSecao("Servidor")
        Text(url ?: "Não configurado", color = LocalCoresExtras.current.texto2)
        OutlinedButton(onClick = { nav.navigate(Rotas.CONFIGURACAO) }) { Text("Trocar servidor") }
    }
}
