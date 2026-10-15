package br.com.estudos.app.ui.assuntos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import br.com.estudos.app.AppContainer
import br.com.estudos.app.ui.Rotas
import br.com.estudos.app.ui.comum.CaixaErro
import br.com.estudos.app.ui.comum.Carregando
import br.com.estudos.app.ui.comum.CartaoItem
import br.com.estudos.app.ui.comum.Pagina
import br.com.estudos.app.ui.comum.SemDados
import br.com.estudos.app.ui.comum.TituloSecao
import br.com.estudos.app.ui.comum.abrirLink
import br.com.estudos.app.ui.comum.lembrarViewModel
import br.com.estudos.app.ui.comum.linkAbrivel
import br.com.estudos.app.ui.comum.rotulo
import br.com.estudos.app.ui.irParaAba
import br.com.estudos.app.ui.tema.LocalCoresExtras
import kotlin.math.roundToInt

@Composable
fun TelaAssuntos(container: AppContainer, nav: NavHostController) {
    val vm = lembrarViewModel { AssuntosViewModel(container.provedorApi, container.selecao) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    Pagina(Modifier.verticalScroll(rememberScrollState())) {
        when {
            estado.carregando -> Carregando()
            estado.erro != null -> CaixaErro(estado.erro!!) { vm.carregar() }
            estado.vazio -> Text("Nenhum assunto ativo ainda.", color = LocalCoresExtras.current.texto2)
            else -> estado.grupos.forEach { grupo ->
                val aberta = grupo.disciplina.id in estado.abertas
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val r = grupo.resumo
                    val contagem = if (r == null) "${grupo.assuntos.size} assuntos"
                    else "${r.backlog} no backlog · ${r.frente} na frente · ${r.consolidados} ${if (r.consolidados == 1L) "consolidado" else "consolidados"}"
                    CartaoItem(
                        "${grupo.disciplina.nome} · peso ${grupo.disciplina.peso.rotulo.lowercase()}",
                        contagem,
                    ) { vm.alternar(grupo.disciplina.id) }
                    if (aberta) {
                        if (grupo.assuntos.isEmpty()) Text("Nenhum assunto ativo.", color = LocalCoresExtras.current.texto2)
                        grupo.assuntos.forEach { a ->
                            val titulo = (a.assunto.ordem?.let { "$it. " } ?: "") + a.assunto.nome
                            Row(Modifier.padding(start = 16.dp)) {
                                CartaoItem(titulo, "${a.assunto.peso.rotulo} · ${a.fase.rotulo}") {
                                    vm.selecionar(grupo.disciplina, a)
                                    nav.navigate(Rotas.assunto(a.assunto.id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelaAssuntoDetalhe(container: AppContainer, nav: NavHostController, assuntoId: Long) {
    val vm = lembrarViewModel { AssuntoDetalheViewModel(assuntoId, container.provedorApi, container.selecao) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Pagina(Modifier.verticalScroll(rememberScrollState())) {
        val item = estado.item
        if (estado.semDados || item == null) {
            SemDados("Não foi possível abrir este assunto.", "Voltar para Assuntos") { irParaAba(nav, Rotas.ASSUNTOS) }
            return@Pagina
        }
        Column {
            Text(item.disciplina, color = LocalCoresExtras.current.texto2)
            Text(item.nome, style = MaterialTheme.typography.titleLarge)
            Text("${item.peso.rotulo} · ${item.fase.rotulo}", color = LocalCoresExtras.current.texto2)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("conteudo" to "Conteúdo", "questoes" to "Questões", "flashcards" to "Flashcards").forEach { (tipo, rotulo) ->
                    OutlinedButton(onClick = {
                        vm.selecionarRegistro()
                        nav.navigate(Rotas.registrar(tipo, assuntoId))
                    }, modifier = Modifier.weight(1f)) { Text(rotulo, maxLines = 1) }
                }
            }
            OutlinedButton(onClick = {
                vm.selecionarErros()
                nav.navigate(Rotas.erros(assuntoId))
            }, modifier = Modifier.fillMaxWidth()) { Text("Erros") }
        }

        if (estado.carregando) Carregando()
        if (estado.segmentos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TituloSecao("Material")
                estado.segmentos.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("${s.ordem}. p. ${s.paginaInicial ?: "?"}–${s.paginaFinal ?: "?"}")
                            s.tempoEstimadoMin?.let { Text("~$it min", style = MaterialTheme.typography.bodySmall, color = LocalCoresExtras.current.texto2) }
                        }
                        linkAbrivel(s.arquivo)?.let { link ->
                            TextButton(onClick = { abrirLink(context, link) }) { Text("abrir material") }
                        }
                    }
                }
            }
        }

        // M-2 (04_FRONTEND §7B/§7D).
        if (estado.retencao.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TituloSecao("Retenção")
                estado.retencao.forEach { s ->
                    Text(s.tipo.rotulo)
                    val primeira = s.percentualPrimeira?.let { "${it.roundToInt()}%" } ?: "—"
                    val depois = s.percentualMediaSeguintes?.let { " · depois: ${it.roundToInt()}%" } ?: ""
                    Text("1ª exposição: $primeira$depois", style = MaterialTheme.typography.bodySmall, color = LocalCoresExtras.current.texto2)
                }
            }
        }
    }
}
