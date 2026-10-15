package br.com.estudos.app.ui.hoje

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import br.com.estudos.app.AppContainer
import br.com.estudos.app.data.api.TurnoPlanoDto
import br.com.estudos.app.ui.Rotas
import br.com.estudos.app.ui.comum.Aviso
import br.com.estudos.app.ui.comum.CaixaErro
import br.com.estudos.app.ui.comum.Carregando
import br.com.estudos.app.ui.comum.CartaoItem
import br.com.estudos.app.ui.comum.Pagina
import br.com.estudos.app.ui.comum.TituloSecao
import br.com.estudos.app.ui.comum.abrirLink
import br.com.estudos.app.ui.comum.dataBr
import br.com.estudos.app.ui.comum.descricao
import br.com.estudos.app.ui.comum.lembrarViewModel
import br.com.estudos.app.ui.comum.linkAbrivel
import br.com.estudos.app.ui.comum.plural
import br.com.estudos.app.ui.comum.textoProva
import kotlinx.coroutines.flow.drop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHoje(container: AppContainer, nav: NavHostController, snackbar: SnackbarHostState) {
    val vm = lembrarViewModel {
        HojeViewModel(container.provedorApi, container.armazenamento, container.selecao, container.agora)
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val falhas by container.envio.falhas.collectAsStateWithLifecycle()

    // Cada volta para Hoje recomeça do teto normal, como a SPA a cada navegação.
    LaunchedEffect(Unit) { vm.abrir() }
    // Registro otimista gravado: o plano muda (especificação §5.2).
    LaunchedEffect(Unit) { container.envio.gravados.drop(1).collect { vm.carregar() } }
    // Falha de envio: snackbar com Reenviar, que reabre a tela com o rascunho.
    val falha = falhas.firstOrNull()
    LaunchedEffect(falha) {
        if (falha == null) return@LaunchedEffect
        val resposta = snackbar.showSnackbar(
            message = "Não foi possível registrar ${falha.descricao}. ${falha.mensagem}",
            actionLabel = "Reenviar",
            duration = SnackbarDuration.Indefinite,
            withDismissAction = true,
        )
        container.envio.descartarFalha(falha.chaveRascunho)
        if (resposta == SnackbarResult.ActionPerformed) nav.navigate(falha.rota)
    }

    PullToRefreshBox(
        isRefreshing = estado.carregando && estado.plano != null,
        onRefresh = { vm.carregar() },
        modifier = Modifier.fillMaxSize(),
    ) {
        Pagina(Modifier.verticalScroll(rememberScrollState())) {
            estado.concurso?.let { linha ->
                val c = linha.concurso
                if (c != null) {
                    CartaoItem(c.nome + (c.cargo?.let { " · $it" } ?: ""), textoProva(c.diasParaProva)) { nav.navigate(Rotas.CONCURSOS) }
                } else {
                    CartaoItem("Nenhum concurso-alvo — adicionar edital") { nav.navigate(Rotas.CONCURSOS) }
                }
            }
            when {
                estado.carregando && estado.plano == null -> Carregando()
                estado.erro != null -> CaixaErro(estado.erro!!) { vm.carregar() }
                estado.plano != null -> ConteudoHoje(estado, estado.plano!!, vm, nav)
            }
        }
    }
}

@Composable
private fun ConteudoHoje(estado: HojeUiState, plano: TurnoPlanoDto, vm: HojeViewModel, nav: NavHostController) {
    val context = LocalContext.current

    estado.planoGuardadoEm?.let {
        val hora = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(it))
        Aviso("Sem conexão — plano de $hora")
    }

    // J-4 (02_JORNADAS): a volta depois de dias sem estudar — sem vermelho, sem lista, sem cobrança.
    if (estado.mostrarRetorno) {
        val linhas = buildList {
            add("Você esteve fora ${plano.diasSemEstudar} dias. Sem problema — a escada não reinicia.")
            val n = plano.filaRecuperacao.size
            if (n > 0) add("Hoje: $n ${if (n == 1) "recuperação." else "recuperações."}")
            if (plano.vencidasAlemDoTeto > 0) {
                val dias = plano.estimativaDiasParaNormalizar
                add("Acumulado: ${plano.represado}" + if (dias > 0) " · normaliza em cerca de $dias ${plural(dias, "dia.", "dias.")}" else ".")
            }
            if (plano.alertaRepresamento) add("Enquanto drena, nenhum assunto novo é sugerido.")
        }
        Aviso(linhas.joinToString("\n"), alerta = false)
    }

    if (estado.vazio) {
        Text("Nada para hoje. Volte amanhã.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // D-56 / 01_DOMINIO §5.3: o alerta diz por que não há assunto novo.
    if (plano.alertaRepresamento && !estado.mostrarRetorno) {
        Aviso("Revisões acumuladas. Drene a fila antes de abrir assunto novo — por isso nenhum é sugerido hoje.")
    }

    if (plano.filaRecuperacao.isNotEmpty()) {
        Secao("Recuperar") {
            plano.filaRecuperacao.forEach { item ->
                CartaoItem(item.nome, "Nível ${item.nivel}") {
                    vm.selecionarRecuperacao(item)
                    nav.navigate(Rotas.recuperar(item.assuntoId))
                }
            }
        }
    }

    // Reestudo (D-07/D-11): corrigir vem antes de abrir — 01_DOMINIO §7.4.
    if (plano.reestudos.isNotEmpty()) {
        Secao("Reestudar") {
            plano.reestudos.forEach { item ->
                CartaoItem(item.nome, "Falhou em ${dataBr(item.dataFalha)} · nível ${item.nivel}") {
                    vm.selecionarReestudo(item)
                    nav.navigate(Rotas.registrar("conteudo", item.assuntoId))
                }
            }
        }
    }

    plano.blocoConteudo?.let { assunto ->
        Secao(if (plano.blocoContinuacao) "Continuar leitura" else "Conteúdo novo") {
            CartaoItem(assunto.nome, plano.segmentoSugerido?.descricao()) {
                vm.selecionarBloco(outro = false)?.let { nav.navigate(Rotas.registrar("conteudo", it)) }
            }
            // D-55: o material do bloco abre direto daqui.
            linkAbrivel(plano.segmentoSugerido?.arquivo)?.let { link ->
                OutlinedButton(onClick = { abrirLink(context, link) }) { Text("Abrir material") }
            }
        }
    }

    // 02_JORNADAS §4.3: "puxar mais" fica no fim, só com algo a oferecer, sempre por pedido explícito.
    if (estado.temAdiantar) {
        Secao("Quer adiantar?") {
            if (plano.vencidasAlemDoTeto > 0) {
                CartaoItem("Drenar mais revisões atrasadas (${plano.vencidasAlemDoTeto})") { vm.drenarMais() }
            }
            plano.proximoBloco?.let { proximo ->
                CartaoItem("Outro bloco de conteúdo", "${plano.blocoConteudo?.nome} · ${proximo.descricao()}") {
                    vm.selecionarBloco(outro = true)?.let { nav.navigate(Rotas.registrar("conteudo", it)) }
                }
            }
            if (plano.antecipaveis.isNotEmpty()) {
                CartaoItem("Antecipar revisão futura", "⚠ encurta o espaçamento") { vm.alternarAntecipaveis() }
                if (estado.mostrarAntecipaveis) {
                    plano.antecipaveis.forEach { item ->
                        val dias = -item.diasAtraso
                        CartaoItem(item.nome, "Prevista para ${dataBr(item.dataPrevista)} ($dias ${plural(dias, "dia antes", "dias antes")})") {
                            vm.selecionarRecuperacao(item)
                            nav.navigate(Rotas.recuperar(item.assuntoId))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Secao(titulo: String, conteudo: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSecao(titulo)
        conteudo()
    }
}
