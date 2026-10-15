package br.com.estudos.app.ui.registrar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import br.com.estudos.app.AppContainer
import br.com.estudos.app.data.api.FormatoBanca
import br.com.estudos.app.ui.comum.CaixaErro
import br.com.estudos.app.ui.comum.Carregando
import br.com.estudos.app.ui.comum.Pagina
import br.com.estudos.app.ui.comum.SemDados
import br.com.estudos.app.ui.comum.TituloSecao
import br.com.estudos.app.ui.comum.descricao
import br.com.estudos.app.ui.comum.lembrarViewModel
import br.com.estudos.app.ui.comum.rotulo
import br.com.estudos.app.ui.tema.LocalCoresExtras
import br.com.estudos.app.ui.voltarParaHoje

private val ROTULO_TIPO = mapOf("conteudo" to "Conteúdo", "questoes" to "Questões", "flashcards" to "Flashcards")

@Composable
fun TelaRegistrar(container: AppContainer, nav: NavHostController, tipo: String, assuntoId: Long) {
    val vm = lembrarViewModel {
        RegistrarViewModel(
            tipo, assuntoId, container.selecao, container.provedorApi,
            container.armazenamento, container.envio, container.agora,
        )
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    BackHandler {
        vm.sair()
        nav.popBackStack()
    }

    Pagina(Modifier.verticalScroll(rememberScrollState())) {
        val r = estado.rascunho
        when {
            estado.carregando -> Carregando()
            estado.semDados || r == null ->
                SemDados("Não foi possível abrir este registro.", "Voltar para Hoje") { voltarParaHoje(nav) }
            else -> {
                estado.erro?.let { CaixaErro(it) }
                Text(r.nome, style = MaterialTheme.typography.titleLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TIPOS_REGISTRO.forEach { t ->
                        FilterChip(selected = r.tipo == t, onClick = { vm.selecionarTipo(t) }, label = { Text(ROTULO_TIPO.getValue(t)) })
                    }
                }

                // Segmento: ausente quando o assunto não tem nenhum, não desabilitado.
                if (r.tipo == "conteudo" && estado.segmentos.isNotEmpty()) {
                    Column {
                        TituloSecao("Bloco lido")
                        OpcaoSegmento("O assunto inteiro (começa as revisões)", r.segmentoId == null) { vm.selecionarSegmento(null) }
                        estado.segmentos.forEach { s ->
                            OpcaoSegmento(s.descricao(), r.segmentoId == s.id) { vm.selecionarSegmento(s.id) }
                        }
                        // D-54
                        Text(
                            "As revisões deste assunto começam depois que você ler o último bloco.",
                            style = MaterialTheme.typography.bodySmall, color = LocalCoresExtras.current.texto2,
                        )
                    }
                }

                if (r.tipo == "questoes") {
                    Column {
                        TituloSecao("Formato da banca")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormatoBanca.entries.forEach { f ->
                                FilterChip(selected = r.formato == f, onClick = { vm.selecionarFormato(f) }, label = { Text(f.rotulo) })
                            }
                        }
                    }
                }

                if (r.tipo == "questoes" || r.tipo == "flashcards") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoNumero(if (r.tipo == "questoes") "Questões feitas" else "Cartões feitos", r.feitas, vm::mudarFeitas, Modifier.weight(1f))
                        CampoNumero("Acertos", r.certas, vm::mudarCertas, Modifier.weight(1f))
                    }
                    CampoNumero("Previsão de acerto antes de ver o resultado (%)", r.previsao, vm::mudarPrevisao)
                }

                // D-29 / 01_DOMINIO §7.7: tempo declarado, não tempo de tela.
                CampoNumero(if (r.tipo == "conteudo") "Tempo de estudo (min)" else "Tempo gasto (min)", r.tempo, vm::mudarTempo)

                Button(
                    onClick = { if (vm.registrar()) voltarParaHoje(nav) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Registrar") }
            }
        }
    }
}

@Composable
private fun OpcaoSegmento(texto: String, marcado: Boolean, aoEscolher: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        RadioButton(selected = marcado, onClick = aoEscolher)
        Text(texto)
    }
}

@Composable
private fun CampoNumero(rotulo: String, valor: String, aoMudar: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = valor, onValueChange = aoMudar, label = { Text(rotulo) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier,
    )
}
