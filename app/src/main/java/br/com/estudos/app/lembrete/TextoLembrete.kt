package br.com.estudos.app.lembrete

import br.com.estudos.app.data.api.TurnoPlanoDto

/**
 * O texto do lembrete a partir do plano (02_JORNADAS §2.2 do SGE): diz o que há
 * para hoje, nunca o que falta; sem dias seguidos, sem cobrança. `null` quando
 * não deve haver notificação (já estudou hoje, ou não há nada para hoje). Sem plano (servidor fora do
 * alcance), só a hora do turno, sem números.
 */
fun textoLembrete(plano: TurnoPlanoDto?): String? {
    if (plano == null) return "Hora do turno de estudo."
    if (plano.diasSemEstudar == 0L) return null
    val partes = mutableListOf<String>()
    val recuperacoes = plano.filaRecuperacao.size
    if (recuperacoes > 0) partes += "$recuperacoes ${if (recuperacoes == 1) "recuperação" else "recuperações"}"
    val reestudos = plano.reestudos.size
    if (reestudos > 0) partes += "$reestudos para reestudar"
    if (plano.blocoConteudo != null) partes += "1 bloco de leitura"
    if (partes.isEmpty()) return null // nada para hoje: lembrar seria ruído
    val lista = if (partes.size == 1) partes[0] else partes.dropLast(1).joinToString(", ") + " e " + partes.last()
    return "Hoje: $lista."
}
