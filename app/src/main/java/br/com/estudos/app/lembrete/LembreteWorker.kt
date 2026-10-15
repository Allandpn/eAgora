package br.com.estudos.app.lembrete

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.estudos.app.EstudosApp
import br.com.estudos.app.MainActivity
import br.com.estudos.app.R
import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.chamar
import kotlinx.coroutines.withTimeoutOrNull

const val CANAL_LEMBRETE = "lembrete-turno"
private const val ID_NOTIFICACAO = 1

/** Canal próprio, importância padrão (especificação §6). */
fun criarCanalLembrete(context: Context) {
    val canal = NotificationChannel(CANAL_LEMBRETE, context.getString(R.string.canal_lembrete), NotificationManager.IMPORTANCE_DEFAULT)
    canal.description = context.getString(R.string.canal_lembrete_descricao)
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
}

/**
 * No disparo, pergunta ao servidor o que há para hoje (02_JORNADAS §2.2 do SGE).
 * Já estudou hoje ou nada para hoje: não notifica. Servidor fora do alcance:
 * só "Hora do turno de estudo.", sem números.
 */
class LembreteWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as EstudosApp).container
        if (!container.armazenamento.lembreteAtual().ativo) return Result.success()
        val plano = withTimeoutOrNull(20_000) {
            try {
                chamar { container.provedorApi.api().plano() }
            } catch (e: ErroApi) {
                null
            }
        }
        val texto = textoLembrete(plano) ?: return Result.success()
        notificar(applicationContext, texto)
        return Result.success()
    }
}

@SuppressLint("MissingPermission") // conferida logo abaixo
private fun notificar(context: Context, texto: String) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    // Tocar na notificação abre Hoje.
    val abrir = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val notificacao = NotificationCompat.Builder(context, CANAL_LEMBRETE)
        .setSmallIcon(R.drawable.ic_notificacao)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(texto)
        .setContentIntent(abrir)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(ID_NOTIFICACAO, notificacao)
}
