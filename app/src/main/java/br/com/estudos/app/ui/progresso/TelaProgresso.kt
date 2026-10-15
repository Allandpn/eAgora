package br.com.estudos.app.ui.progresso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.estudos.app.AppContainer
import br.com.estudos.app.data.ProvedorApi
import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.FrenteResumoDto
import br.com.estudos.app.data.api.JanelaMetrica
import br.com.estudos.app.data.api.M1Dto
import br.com.estudos.app.data.api.M3Dto
import br.com.estudos.app.data.api.M4Dto
import br.com.estudos.app.data.api.SugestaoDivisaoDto
import br.com.estudos.app.data.api.chamar
import br.com.estudos.app.ui.comum.Aviso
import br.com.estudos.app.ui.comum.CaixaErro
import br.com.estudos.app.ui.comum.Carregando
import br.com.estudos.app.ui.comum.CartaoItem
import br.com.estudos.app.ui.comum.Pagina
import br.com.estudos.app.ui.comum.lembrarViewModel
import br.com.estudos.app.ui.comum.plural
import br.com.estudos.app.ui.comum.rotulo
import br.com.estudos.app.ui.tema.LocalCoresExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class ProgressoUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val janela: JanelaMetrica = JanelaMetrica.GLOBAL,
    val disciplinas: Map<Long, String> = emptyMap(),
    val m1: M1Dto? = null,
    val m3: M3Dto? = null,
    val m4: M4Dto? = null,
    val frente: FrenteResumoDto? = null,
    val divisoes: List<SugestaoDivisaoDto> = emptyList(),
)

/** Progresso (J-3, 04_FRONTEND §7D/§7I): só mostra as métricas que o servidor calcula. */
class ProgressoViewModel(private val provedor: ProvedorApi) : ViewModel() {
    private val _estado = MutableStateFlow(ProgressoUiState())
    val estado: StateFlow<ProgressoUiState> = _estado

    init { carregar() }

    fun carregar() {
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }
            try {
                val api = provedor.api()
                val disciplinas = chamar { api.disciplinas() }.associate { it.id to it.nome }
                val m1 = chamar { api.m1(_estado.value.janela) }
                val m3 = chamar { api.m3() }
                val m4 = chamar { api.m4() }
                val frente = chamar { api.frente() }
                val divisoes = chamar { api.sugestoesDivisao() }
                _estado.update {
                    it.copy(carregando = false, disciplinas = disciplinas, m1 = m1, m3 = m3, m4 = m4, frente = frente, divisoes = divisoes)
                }
            } catch (e: ErroApi) {
                _estado.update { it.copy(carregando = false, erro = e.message) }
            }
        }
    }

    fun trocarJanela(janela: JanelaMetrica) {
        _estado.update { it.copy(janela = janela) }
        viewModelScope.launch {
            runCatching { chamar { provedor.api().m1(janela) } }.onSuccess { m1 -> _estado.update { it.copy(m1 = m1) } }
        }
    }
}

@Composable
fun TelaProgresso(container: AppContainer) {
    val vm = lembrarViewModel { ProgressoViewModel(container.provedorApi) }
    val e by vm.estado.collectAsStateWithLifecycle()
    val texto2 = LocalCoresExtras.current.texto2

    Pagina(Modifier.verticalScroll(rememberScrollState())) {
        if (e.carregando) { Carregando(); return@Pagina }
        e.erro?.let { CaixaErro(it) { vm.carregar() }; return@Pagina }
        val m3 = e.m3 ?: return@Pagina
        val m4 = e.m4 ?: return@Pagina
        val frente = e.frente ?: return@Pagina

        Text("A retenção (M-2) de cada assunto fica no detalhe dele, em Assuntos.", style = MaterialTheme.typography.bodySmall, color = texto2)

        Bloco("Eu me conheço?", "calibração (M-3)") {
            Linha(
                "Objetiva (questões/flashcards)",
                if (m3.objetiva.n == 0) "sem dados ainda" else {
                    val d = m3.objetiva.mediaDesvio
                    "${abs(d).roundToInt()} pts — " + when { d > 0 -> "superestimou"; d < 0 -> "subestimou"; else -> "sem desvio" }
                },
            )
            Linha(
                "Subjetiva (recuperação)",
                if (m3.subjetiva.n == 0) "sem dados ainda"
                else "${m3.subjetiva.superestimou} superestimou · ${m3.subjetiva.subestimou} subestimou · ${m3.subjetiva.acertouPrevisao} acertou",
            )
        }

        Bloco("Estou melhorando?", "acerto em questões (M-1)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = e.janela == JanelaMetrica.GLOBAL, onClick = { vm.trocarJanela(JanelaMetrica.GLOBAL) }, label = { Text("Global") })
                FilterChip(selected = e.janela == JanelaMetrica.POR_DISCIPLINA, onClick = { vm.trocarJanela(JanelaMetrica.POR_DISCIPLINA) }, label = { Text("Por disciplina") })
            }
            Text(
                "Número lento: com pouca amostra o percentual fica em \"—\". Compare trimestre a trimestre, não semana a semana.",
                style = MaterialTheme.typography.bodySmall, color = texto2,
            )
            e.m1?.linhas?.forEach { l ->
                val rotulo = l.disciplinaId?.let { e.disciplinas[it] ?: "—" } ?: "Global"
                val pct = l.percentual?.let { "${it.roundToInt()}%" } ?: "—"
                CartaoItem("$rotulo · ${l.formato.rotulo}: $pct", "${l.acertos}/${l.total}" + if (l.confiavel) " · amostra grande" else "")
            }
        }

        // 02_JORNADAS J-3: a pergunta operacional da semana.
        Bloco("A frente está saudável?", null) {
            Linha("Em estudo", "${frente.frenteAtual} de ${frente.tetoGlobalFrente}")
            Linha("Revisões atrasadas", "${frente.represado}")
            Linha("Entraram na semana", "${frente.entraramNaSemana}")
            Linha("Consolidaram na semana", "${frente.consolidaramNaSemana}")
            Text("${frente.consolidados} consolidados no total · ${frente.backlog} na fila para começar", style = MaterialTheme.typography.bodySmall, color = texto2)
            if (frente.alertaRepresamento) {
                val dias = frente.estimativaDiasParaNormalizar
                Aviso("Atraso acima de um dia de revisões: drenando no ritmo atual, normaliza em cerca de $dias ${plural(dias, "dia", "dias")}. Enquanto isso, nenhum assunto novo é sugerido.")
            }
            if (frente.avisoTetoInsuficiente) {
                Aviso("O limite diário de recuperações não sustenta o tamanho da frente nem no melhor caso. Reduza a frente ou aumente o limite em Ajustes.")
            }
        }

        // D-35 (01_DOMINIO §3.2.1): só sugere, nunca divide.
        if (e.divisoes.isNotEmpty()) {
            Bloco("Assuntos que podem estar grandes demais", null) {
                Text(
                    "Parcial seguido costuma ser assunto grande: você sabe uma parte e erra outra, e a escada não sobe. Dividir em assuntos menores costuma destravar.",
                    style = MaterialTheme.typography.bodySmall, color = texto2,
                )
                e.divisoes.forEach { d -> CartaoItem(d.nome, "${d.disciplina} · ${d.parciaisSeguidos} parciais seguidos") }
            }
        }

        Bloco("Estou em dia?", "aderência ao prazo (M-4)") {
            Linha((m4.percentual?.let { "${it.roundToInt()}%" } ?: "—") + " em até 3 dias do previsto", "${m4.dentroDoPrazo}/${m4.totalCumpridas}")
        }
    }
}

@Composable
private fun Bloco(titulo: String, sub: String?, conteudo: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            titulo + (sub?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.titleMedium,
        )
        conteudo()
    }
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, color = LocalCoresExtras.current.texto2, modifier = Modifier.weight(1f))
        Text(valor, fontWeight = FontWeight.Medium)
    }
}
