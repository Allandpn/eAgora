package br.com.estudos.app.ui.recuperar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import br.com.estudos.app.AppContainer
import br.com.estudos.app.data.api.ResultadoSessao
import br.com.estudos.app.ui.Rotas
import br.com.estudos.app.ui.comum.Carregando
import br.com.estudos.app.ui.comum.Pagina
import br.com.estudos.app.ui.comum.SemDados
import br.com.estudos.app.ui.comum.ancora
import br.com.estudos.app.ui.comum.dataBr
import br.com.estudos.app.ui.comum.lembrarViewModel
import br.com.estudos.app.ui.comum.previsao
import br.com.estudos.app.ui.comum.resultado
import br.com.estudos.app.ui.tema.LocalCoresExtras
import br.com.estudos.app.ui.voltarParaHoje

@Composable
fun TelaRecuperar(container: AppContainer, nav: NavHostController, assuntoId: Long) {
    val vm = lembrarViewModel {
        RecuperarViewModel(assuntoId, container.selecao, container.armazenamento, container.envio, container.agora)
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    // Sem botão "pular"; voltar do sistema sai sem registrar (especificação §3.2).
    BackHandler {
        vm.sair()
        nav.popBackStack()
    }
    ConteudoRecuperar(
        estado = estado,
        aoPrever = vm::prever,
        aoConfirmar = { if (vm.confirmar(it)) voltarParaHoje(nav) },
        aoRegistrarErro = {
            vm.selecionarErro()
            nav.navigate(Rotas.erros(assuntoId))
        },
        aoVoltarParaHoje = { voltarParaHoje(nav) },
    )
}

/** Separado da tela para o teste de Compose das regras vinculantes. */
@Composable
fun ConteudoRecuperar(
    estado: RecuperarUiState,
    aoPrever: (ResultadoSessao) -> Unit,
    aoConfirmar: (ResultadoSessao) -> Unit,
    aoRegistrarErro: () -> Unit,
    aoVoltarParaHoje: () -> Unit,
) {
    Pagina(Modifier.verticalScroll(rememberScrollState())) {
        when (estado) {
            RecuperarUiState.Carregando -> Carregando()
            RecuperarUiState.SemDados -> SemDados("Não foi possível abrir esta recuperação.", "Voltar para Hoje", aoVoltarParaHoje)
            is RecuperarUiState.Aberta -> {
                val r = estado.rascunho
                // Etapa 1 sem resultado na árvore — ausente, não escondido (regra vinculante).
                if (r.etapa == 1) Etapa1(r, aoPrever) else Etapa2(r, aoConfirmar, aoRegistrarErro)
            }
        }
    }
}

@Composable
private fun Etapa1(r: RascunhoRecuperacao, aoPrever: (ResultadoSessao) -> Unit) {
    Column(Modifier.testTag("etapa1"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(r.nome, style = MaterialTheme.typography.titleLarge)
            Text(
                "Nível ${r.nivel}" + (r.dataPrevista?.let { " · previsto para ${dataBr(it)}" } ?: ""),
                color = LocalCoresExtras.current.texto2,
            )
        }
        Text("O que você espera?", fontWeight = FontWeight.Medium)
        ResultadoSessao.entries.forEach { valor ->
            OutlinedButton(onClick = { aoPrever(valor) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(valor.previsao)
            }
        }
        Text("Feche o material e reconstrua de memória.", color = LocalCoresExtras.current.texto2)
    }
}

@Composable
private fun Etapa2(r: RascunhoRecuperacao, aoConfirmar: (ResultadoSessao) -> Unit, aoRegistrarErro: () -> Unit) {
    Column(Modifier.testTag("etapa2"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(r.nome, style = MaterialTheme.typography.titleLarge)
        // A previsão aparece como texto, não editável (regra vinculante).
        Text("Você previu: ${r.previsao?.previsao?.lowercase() ?: "—"}", color = LocalCoresExtras.current.texto2)
        Text("Conseguiu reconstruir?", fontWeight = FontWeight.Medium)
        ResultadoSessao.entries.forEach { valor ->
            Button(onClick = { aoConfirmar(valor) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Text(valor.resultado, fontWeight = FontWeight.Medium)
                    Text(valor.ancora, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        TextButton(onClick = aoRegistrarErro) { Text("+ registrar um erro") }
    }
}
