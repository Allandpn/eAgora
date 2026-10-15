package br.com.estudos.app.ui.concursos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import br.com.estudos.app.AppContainer
import br.com.estudos.app.data.api.ConcursoDto
import br.com.estudos.app.ui.Rotas
import br.com.estudos.app.ui.comum.CaixaErro
import br.com.estudos.app.ui.comum.Carregando
import br.com.estudos.app.ui.comum.CartaoItem
import br.com.estudos.app.ui.comum.Pagina
import br.com.estudos.app.ui.comum.TituloSecao
import br.com.estudos.app.ui.comum.dataBr
import br.com.estudos.app.ui.comum.dataDoInstante
import br.com.estudos.app.ui.comum.lembrarViewModel
import br.com.estudos.app.ui.comum.rotulo
import br.com.estudos.app.ui.comum.textoProva
import br.com.estudos.app.ui.tema.LocalCoresExtras

/** Concursos (02_JORNADAS J-5): concurso-alvo, edital novo e os editais de antes. */
@Composable
fun TelaConcursos(container: AppContainer, nav: NavHostController) {
    val vm = lembrarViewModel { ConcursosViewModel(container.provedorApi) }
    val e by vm.estado.collectAsStateWithLifecycle()
    var paraReativar by remember { mutableStateOf<ConcursoDto?>(null) }
    LaunchedEffect(Unit) { vm.carregar() }
    val texto2 = LocalCoresExtras.current.texto2

    Pagina(Modifier.verticalScroll(rememberScrollState())) {
        when {
            e.carregando && e.concursos.isEmpty() -> Carregando()
            e.erro != null -> CaixaErro(e.erro!!) { vm.carregar() }
            else -> {
                e.erroAcao?.let { CaixaErro(it) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TituloSecao("Concurso-alvo")
                    val ativo = e.ativo
                    if (ativo == null) {
                        Text("Nenhum concurso-alvo ainda.", color = texto2)
                    } else {
                        Text(ativo.nome, style = MaterialTheme.typography.titleLarge)
                        ativo.cargo?.let { Text(it) }
                        Text(listOfNotNull(ativo.banca, ativo.dataProva?.let { "prova em ${dataBr(it)}" } ?: "sem data de prova").joinToString(" · "), color = texto2)
                        Text(
                            "${textoProva(ativo.diasParaProva).replaceFirstChar { it.uppercase() }} · alvo desde ${dataDoInstante(ativo.ativadoEm)} · ${ativo.totalAssuntos} assuntos no edital",
                            style = MaterialTheme.typography.bodySmall, color = texto2,
                        )
                    }
                }

                val ia = e.ia
                if (ia != null && ia.disponivel) {
                    Button(onClick = { nav.navigate(Rotas.EDITAL_NOVO) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Novo edital")
                    }
                } else if (ia != null) {
                    Text("Novo edital indisponível: ${ia.motivo.orEmpty()}", color = texto2)
                }
                e.analises.forEach { a ->
                    CartaoItem(a.concursoNome ?: a.nomeArquivo, a.status.rotulo) { nav.navigate(Rotas.analise(a.id)) }
                }

                if (e.anteriores.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TituloSecao("Editais de antes")
                        e.anteriores.forEach { c ->
                            CartaoItem(
                                c.nome + (c.cargo?.let { " · $it" } ?: ""),
                                "${dataDoInstante(c.ativadoEm)} – ${if (c.desativadoEm != null) dataDoInstante(c.desativadoEm) else "hoje"} · " +
                                    "${c.totalAssuntos} assuntos · ${c.emComumComAtivo} em comum com o atual",
                            )
                            OutlinedButton(onClick = { paraReativar = c }, enabled = e.reativando == null) { Text("Reativar") }
                        }
                    }
                }
            }
        }
    }

    // D-60: reativar muda peso e ordem — sempre com confirmação.
    paraReativar?.let { c ->
        AlertDialog(
            onDismissRequest = { paraReativar = null },
            title = { Text("Reativar ${c.nome}?") },
            text = { Text("Peso e ordem dos assuntos voltam a ser os deste edital. O estudo feito não muda.") },
            confirmButton = { TextButton(onClick = { vm.reativar(c.id); paraReativar = null }) { Text("Reativar") } },
            dismissButton = { TextButton(onClick = { paraReativar = null }) { Text("Cancelar") } },
        )
    }
}
