package br.com.estudos.app.ui.comum

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import java.util.UUID

/** ViewModel por entrada da pilha de navegação, construído à mão (A-003). */
@Composable
inline fun <reified VM : ViewModel> lembrarViewModel(crossinline criar: () -> VM): VM =
    viewModel(factory = viewModelFactory { initializer { criar() } })

/** Data do registro: o dia local do aparelho, como `hojeISO()` da SPA. */
fun hojeIso(): String = LocalDate.now().toString()

/** D-45: identificador da tentativa, gerado ao abrir a tela. */
fun novaTentativa(): String = UUID.randomUUID().toString()

/** Rascunho com mais de 24 h é descartado ao abrir (especificação §5.3). */
const val VALIDADE_RASCUNHO_MS = 24L * 60 * 60 * 1000

/** "Abrir material": o Android oferece o app do Google Drive (especificação §3.1). */
fun abrirLink(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
