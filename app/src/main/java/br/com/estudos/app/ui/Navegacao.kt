package br.com.estudos.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.estudos.app.AppContainer
import br.com.estudos.app.ui.ajustes.TelaAjustes
import br.com.estudos.app.ui.assuntos.TelaAssuntoDetalhe
import br.com.estudos.app.ui.assuntos.TelaAssuntos
import br.com.estudos.app.ui.comum.Carregando
import br.com.estudos.app.ui.configuracao.TelaConfiguracao
import br.com.estudos.app.ui.erros.TelaErros
import br.com.estudos.app.ui.hoje.TelaHoje
import br.com.estudos.app.ui.progresso.TelaProgresso
import br.com.estudos.app.ui.recuperar.TelaRecuperar
import br.com.estudos.app.ui.registrar.TelaRegistrar

/** Rotas da especificação §2. */
object Rotas {
    const val HOJE = "hoje"
    const val ASSUNTOS = "assuntos"
    const val PROGRESSO = "progresso"
    const val AJUSTES = "ajustes"
    const val CONFIGURACAO = "configuracao"
    fun recuperar(assuntoId: Long) = "recuperar/$assuntoId"
    fun registrar(tipo: String, assuntoId: Long) = "registrar/$tipo/$assuntoId"
    fun assunto(assuntoId: Long) = "assuntos/$assuntoId"
    fun erros(assuntoId: Long) = "erros/$assuntoId"
}

private data class Aba(val rota: String, val rotulo: String, val icone: ImageVector)

private val ABAS = listOf(
    Aba(Rotas.HOJE, "Hoje", Icons.Filled.Home), Aba(Rotas.ASSUNTOS, "Assuntos", Icons.AutoMirrored.Filled.List),
    Aba(Rotas.PROGRESSO, "Progresso", Icons.Filled.CheckCircle), Aba(Rotas.AJUSTES, "Ajustes", Icons.Filled.Settings),
)

@Composable
fun NavegacaoApp(container: AppContainer) {
    // Sentinela: enquanto o DataStore não respondeu, não decide a tela inicial.
    val url by container.armazenamento.urlServidor.collectAsState(initial = "…carregando")
    if (url == "…carregando") {
        Carregando()
        return
    }
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entrada by nav.currentBackStackEntryAsState()
    val rotaAtual = entrada?.destination?.route
    val mostrarAbas = ABAS.any { it.rota == rotaAtual }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (mostrarAbas) {
                NavigationBar {
                    ABAS.forEach { aba ->
                        NavigationBarItem(
                            selected = rotaAtual == aba.rota,
                            onClick = { irParaAba(nav, aba.rota) },
                            icon = { Icon(aba.icone, contentDescription = null) },
                            label = { Text(aba.rotulo) },
                        )
                    }
                }
            }
        },
    ) { margens ->
        Box(Modifier.fillMaxSize().padding(margens)) {
            NavHost(nav, startDestination = if (url == null) Rotas.CONFIGURACAO else Rotas.HOJE) {
                composable(Rotas.HOJE) { TelaHoje(container, nav, snackbar) }
                composable(Rotas.ASSUNTOS) { TelaAssuntos(container, nav) }
                composable(Rotas.PROGRESSO) { TelaProgresso(container) }
                composable(Rotas.AJUSTES) { TelaAjustes(container, nav) }
                composable(Rotas.CONFIGURACAO) { TelaConfiguracao(container, nav) }
                composable(
                    "recuperar/{assuntoId}",
                    arguments = listOf(navArgument("assuntoId") { type = NavType.LongType }),
                ) { TelaRecuperar(container, nav, it.arguments!!.getLong("assuntoId")) }
                composable(
                    "registrar/{tipo}/{assuntoId}",
                    arguments = listOf(
                        navArgument("tipo") { type = NavType.StringType },
                        navArgument("assuntoId") { type = NavType.LongType },
                    ),
                ) { TelaRegistrar(container, nav, it.arguments!!.getString("tipo")!!, it.arguments!!.getLong("assuntoId")) }
                composable(
                    "assuntos/{assuntoId}",
                    arguments = listOf(navArgument("assuntoId") { type = NavType.LongType }),
                ) { TelaAssuntoDetalhe(container, nav, it.arguments!!.getLong("assuntoId")) }
                composable(
                    "erros/{assuntoId}",
                    arguments = listOf(navArgument("assuntoId") { type = NavType.LongType }),
                ) { TelaErros(container, nav, it.arguments!!.getLong("assuntoId")) }
            }
        }
    }
}

fun irParaAba(nav: NavHostController, rota: String) {
    nav.navigate(rota) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Depois de registrar: volta para Hoje (transição otimista, especificação §5.2). */
fun voltarParaHoje(nav: NavHostController) {
    if (!nav.popBackStack(Rotas.HOJE, inclusive = false)) {
        nav.navigate(Rotas.HOJE) { popUpTo(0) }
    }
}
