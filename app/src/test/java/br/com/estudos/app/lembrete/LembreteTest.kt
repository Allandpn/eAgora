package br.com.estudos.app.lembrete

import br.com.estudos.app.data.api.TurnoPlanoDto
import br.com.estudos.app.decodificar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

/** 02_JORNADAS §2.2 do SGE: o que há para hoje, sem cobrança; não lembra quem já estudou. */
class LembreteTest {
    private val real = decodificar("plano.json", TurnoPlanoDto.serializer())

    @Test fun `plano real vira a frase do dia`() =
        assertEquals("Hoje: 8 recuperações, 1 para reestudar e 1 bloco de leitura.", textoLembrete(real))

    @Test fun `ja estudou hoje nao notifica`() = assertNull(textoLembrete(real.copy(diasSemEstudar = 0)))

    @Test fun `nada para hoje nao notifica`() = assertNull(textoLembrete(TurnoPlanoDto(diasSemEstudar = 3)))

    @Test fun `nunca estudou ainda conta como nao ter estudado hoje`() =
        assertEquals("Hoje: 1 bloco de leitura.", textoLembrete(TurnoPlanoDto(blocoConteudo = real.blocoConteudo, diasSemEstudar = null)))

    @Test fun `singular e duas partes`() = assertEquals(
        "Hoje: 1 recuperação e 1 bloco de leitura.",
        textoLembrete(real.copy(filaRecuperacao = real.filaRecuperacao.take(1), reestudos = emptyList())),
    )

    @Test fun `servidor fora so a hora do turno`() = assertEquals("Hora do turno de estudo.", textoLembrete(null))

    @Test fun `primeiro disparo hoje se o horario nao passou, senao amanha`() {
        val agora = LocalDateTime.of(2026, 10, 15, 18, 30)
        assertEquals(Duration.ofMinutes(30), atrasoAte(19, 0, agora))
        assertEquals(Duration.ofHours(23).plusMinutes(30), atrasoAte(18, 0, agora))
        assertEquals(Duration.ofHours(24), atrasoAte(18, 30, agora))
    }
}
