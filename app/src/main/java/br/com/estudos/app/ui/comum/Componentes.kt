package br.com.estudos.app.ui.comum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.estudos.app.ui.tema.LocalCoresExtras

/** Coluna centrada com largura máxima — tablet e paisagem (especificação §1). */
@Composable
fun Pagina(modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = conteudo,
        )
    }
}

@Composable
fun Carregando() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Os quatro estados de tela (04_FRONTEND §8): erro com "Tentar de novo". */
@Composable
fun CaixaErro(mensagem: String, aoTentarDeNovo: (() -> Unit)? = null) {
    Surface(
        color = Color(0x14E53935), shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, Color(0x66E53935)), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(mensagem, color = MaterialTheme.colorScheme.error)
            if (aoTentarDeNovo != null) OutlinedButton(onClick = aoTentarDeNovo) { Text("Tentar de novo") }
        }
    }
}

@Composable
fun TituloSecao(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Item tocável: alvo de toque de no mínimo 48 dp (02_JORNADAS §1.2). */
@Composable
fun CartaoItem(
    titulo: String,
    subtitulo: String? = null,
    destaque: Boolean = false,
    aoTocar: (() -> Unit)? = null,
) {
    val extras = LocalCoresExtras.current
    val modificador = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    val borda = BorderStroke(1.dp, if (destaque) extras.alerta else extras.borda)
    val cor = if (destaque) extras.alertaFundo else MaterialTheme.colorScheme.surface
    val corpo: @Composable () -> Unit = {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, fontWeight = FontWeight.Medium)
            if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = extras.texto2)
        }
    }
    if (aoTocar != null) {
        Surface(onClick = aoTocar, modifier = modificador, color = cor, border = borda, shape = MaterialTheme.shapes.small, content = corpo)
    } else {
        Surface(modifier = modificador, color = cor, border = borda, shape = MaterialTheme.shapes.small, content = corpo)
    }
}

@Composable
fun Aviso(texto: String, alerta: Boolean = true) {
    val extras = LocalCoresExtras.current
    Surface(
        color = if (alerta) extras.alertaFundo else extras.retornoFundo,
        border = BorderStroke(1.dp, if (alerta) extras.alerta else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth(),
    ) { Text(texto, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
}

/** Rota aberta sem o item da tela anterior (especificação §2), como o `semDados` da SPA. */
@Composable
fun SemDados(mensagem: String, rotuloVoltar: String, aoVoltar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(mensagem)
        OutlinedButton(onClick = aoVoltar) { Text(rotuloVoltar) }
    }
}
