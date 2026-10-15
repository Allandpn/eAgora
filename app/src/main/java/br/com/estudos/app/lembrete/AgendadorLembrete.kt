package br.com.estudos.app.lembrete

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.estudos.app.data.LembreteConfig
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private const val TRABALHO = "lembrete-turno"

/**
 * Lembrete diário (especificação §6): trabalho periódico de 24 h com o primeiro
 * disparo no horário escolhido. Sem servidor de push — o aparelho se lembra sozinho.
 */
class AgendadorLembrete(private val context: Context) {

    fun agendar(config: LembreteConfig, agora: LocalDateTime = LocalDateTime.now()) {
        val wm = WorkManager.getInstance(context)
        if (!config.ativo) {
            wm.cancelUniqueWork(TRABALHO)
            return
        }
        val pedido = PeriodicWorkRequestBuilder<LembreteWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(atrasoAte(config.hora, config.minuto, agora).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        // Horário novo substitui o agendamento antigo, com o primeiro disparo recalculado.
        wm.enqueueUniquePeriodicWork(TRABALHO, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, pedido)
    }
}

/** Quanto falta até a próxima ocorrência de hh:mm — hoje, se ainda não passou; senão amanhã. */
fun atrasoAte(hora: Int, minuto: Int, agora: LocalDateTime): Duration {
    var alvo = agora.toLocalDate().atTime(hora, minuto)
    if (!alvo.isAfter(agora)) alvo = alvo.plusDays(1)
    return Duration.between(agora, alvo)
}
