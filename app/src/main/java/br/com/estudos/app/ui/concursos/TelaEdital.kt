package br.com.estudos.app.ui.concursos

import android.app.DatePickerDialog
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import br.com.estudos.app.AppContainer
import br.com.estudos.app.data.api.StatusAnalise
import br.com.estudos.app.data.api.TipoPeso
import br.com.estudos.app.ui.Rotas
import br.com.estudos.app.ui.comum.Aviso
import br.com.estudos.app.ui.comum.CaixaErro
import br.com.estudos.app.ui.comum.Carregando
import br.com.estudos.app.ui.comum.Pagina
import br.com.estudos.app.ui.comum.TituloSecao
import br.com.estudos.app.ui.comum.dataBr
import br.com.estudos.app.ui.comum.lembrarViewModel
import br.com.estudos.app.ui.comum.rotulo
import br.com.estudos.app.ui.tema.LocalCoresExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Edital novo (02_JORNADAS J-5). `analiseId` nulo = escolher e enviar o PDF. */
@Composable
fun TelaEdital(container: AppContainer, nav: NavHostController, analiseId: Long?) {
    val vm = lembrarViewModel { EditalViewModel(analiseId, container.provedorApi) }
    val e by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(e.criada) {
        e.criada?.let { id -> nav.navigate(Rotas.analise(id)) { popUpTo(Rotas.EDITAL_NOVO) { inclusive = true } } }
    }
    LaunchedEffect(e.confirmada) {
        if (e.confirmada) nav.popBackStack(Rotas.CONCURSOS, inclusive = false)
    }

    Pagina(Modifier.verticalScroll(rememberScrollState())) {
        e.erroAcao?.let { CaixaErro(it) }
        if (analiseId == null) {
            EnviarPdf(enviando = e.enviando, aoEnviar = vm::enviar)
            return@Pagina
        }
        val a = e.analise
        when {
            e.carregando -> Carregando()
            e.erro != null -> CaixaErro(e.erro!!) { vm.carregar() }
            a == null -> Unit
            e.trabalhando -> Aviso(
                (if (a.status == StatusAnalise.LENDO_EDITAL) "Lendo o edital…" else "Separando os assuntos de ${a.cargoEscolhido}…") +
                    "\nPode levar um ou dois minutos. Dá para sair desta tela: a análise continua e fica em Concursos.",
                alerta = false,
            )
            a.status == StatusAnalise.FALHOU -> CaixaErro(a.erro ?: "A análise falhou.") { vm.tentarDeNovo() }
            a.status == StatusAnalise.ESCOLHER_CARGO -> {
                Text(a.concursoNome ?: "Edital", style = MaterialTheme.typography.titleLarge)
                Text(listOfNotNull(a.banca, a.dataProva?.let { "prova em ${dataBr(it)}" }).joinToString(" · "), color = LocalCoresExtras.current.texto2)
                Column {
                    TituloSecao("Qual cargo?")
                    a.cargos.forEach { cargo ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            RadioButton(selected = e.cargo == cargo, onClick = { vm.escolherCargo(cargo) })
                            Text(cargo)
                        }
                    }
                }
                Button(onClick = vm::separarAssuntos, enabled = e.cargo != null && !e.enviando, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Separar os assuntos deste cargo")
                }
            }
            a.status == StatusAnalise.PRONTA && e.revisao != null -> RevisaoProposta(e, vm)
            a.status == StatusAnalise.CONFIRMADA -> {
                Text("Concurso confirmado e ativado.")
                OutlinedButton(onClick = { nav.popBackStack(Rotas.CONCURSOS, inclusive = false) }) { Text("Ver concursos") }
            }
        }
    }
}

@Composable
private fun EnviarPdf(enviando: Boolean, aoEnviar: (String, ByteArray) -> Unit) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var escolhido by remember { mutableStateOf<Pair<String, ByteArray>?>(null) }
    val escolher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        escopo.launch {
            escolhido = withContext(Dispatchers.IO) {
                val nome = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "edital.pdf"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                nome to bytes
            }
        }
    }
    Text("Novo edital", style = MaterialTheme.typography.titleLarge)
    Text(
        "Envie o PDF do edital. Uma IA lê o texto, lista os cargos e, para o cargo que você escolher, separa as disciplinas e os assuntos. Nada é gravado até você revisar e confirmar.",
        color = LocalCoresExtras.current.texto2,
    )
    OutlinedButton(onClick = { escolher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
        Text(escolhido?.first ?: "Escolher o PDF")
    }
    Button(
        onClick = { escolhido?.let { (nome, bytes) -> aoEnviar(nome, bytes) } },
        enabled = escolhido != null && !enviando,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text(if (enviando) "Enviando…" else "Enviar edital") }
}

@Composable
private fun RevisaoProposta(e: EditalUiState, vm: EditalViewModel) {
    val r = e.revisao ?: return
    val context = LocalContext.current
    val texto2 = LocalCoresExtras.current.texto2
    Text("Revise antes de confirmar", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(r.nome, { v -> vm.editar { it.copy(nome = v) } }, label = { Text("Concurso") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(r.cargo, { v -> vm.editar { it.copy(cargo = v) } }, label = { Text("Cargo") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(r.banca, { v -> vm.editar { it.copy(banca = v) } }, label = { Text("Banca") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Data da prova: " + (r.dataProva?.let(::dataBr) ?: "não informada"), modifier = Modifier.weight(1f))
        TextButton(onClick = {
            val base = r.dataProva?.let(LocalDate::parse) ?: LocalDate.now()
            DatePickerDialog(context, { _, ano, mes, dia -> vm.editar { it.copy(dataProva = LocalDate.of(ano, mes + 1, dia).toString()) } },
                base.year, base.monthValue - 1, base.dayOfMonth).show()
        }) { Text("Alterar") }
    }
    e.analise?.resumo?.let { Aviso(resumoRevisao(r, it), alerta = false) }

    r.disciplinas.forEachIndexed { i, d ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(d.nome, style = MaterialTheme.typography.titleMedium)
            Text(
                (if (d.questoes != null) "${d.questoes} questões" else "sem quadro de questões — peso não determinado") +
                    (if (d.disciplinaExistenteNome != null) " · já existe" else " · disciplina nova"),
                style = MaterialTheme.typography.bodySmall, color = texto2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoPeso.entries.forEach { p ->
                    FilterChip(selected = d.peso == p, onClick = { vm.trocarPeso(i, p) }, label = { Text(p.rotulo) })
                }
            }
            d.assuntos.forEachIndexed { j, a ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = a.incluir, onCheckedChange = { vm.alternarAssunto(i, j) })
                    Column(Modifier.weight(1f).padding(top = 12.dp)) {
                        Text(a.nome, textDecoration = if (a.incluir) null else TextDecoration.LineThrough,
                            color = if (a.incluir) MaterialTheme.colorScheme.onSurface else texto2)
                        if (a.assuntoExistenteId != null) {
                            Text("já existe: ${a.assuntoExistenteNome} (${a.disciplinaAtualNome}) — mantém escada e histórico",
                                style = MaterialTheme.typography.bodySmall, color = texto2)
                            TextButton(onClick = { vm.tratarComoNovo(i, j) }) { Text("tratar como novo") }
                        } else {
                            Text("novo", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
    Button(onClick = vm::confirmar, enabled = !e.enviando, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(if (e.enviando) "Confirmando…" else "Confirmar e ativar")
    }
}
