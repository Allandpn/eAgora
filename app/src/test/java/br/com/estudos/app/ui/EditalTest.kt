package br.com.estudos.app.ui

import br.com.estudos.app.ApiFalsa
import br.com.estudos.app.data.api.*
import br.com.estudos.app.decodificar
import br.com.estudos.app.provedorCom
import br.com.estudos.app.ui.comum.textoProva
import br.com.estudos.app.ui.concursos.EditalViewModel
import br.com.estudos.app.ui.concursos.resumoRevisao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Edital novo no app (02_JORNADAS J-5): o app só mostra, coleta e envia — peso e IA são do servidor. */
@OptIn(ExperimentalCoroutinesApi::class)
class EditalTest {
    private val despachante = StandardTestDispatcher()
    private val pronta = decodificar("analise-pronta.json", AnaliseEditalDto.serializer())
    private val escolherCargo = decodificar("analise-cargo.json", AnaliseEditalDto.serializer())

    @Before fun main() = Dispatchers.setMain(despachante)
    @After fun limpar() = Dispatchers.resetMain()

    private inner class Api(var respostas: ArrayDeque<AnaliseEditalDto>) : ApiFalsa() {
        var consultas = 0
        var confirmado: ConfirmacaoEditalDto? = null
        override suspend fun analise(id: Long): AnaliseEditalDto { consultas++; return respostas.removeFirst().let { if (respostas.isEmpty()) respostas.addLast(it); it } }
        override suspend fun confirmarEdital(id: Long, corpo: ConfirmacaoEditalDto): ConcursoDto {
            confirmado = corpo
            return ConcursoDto(9, corpo.nome, origem = OrigemConcurso.ANALISE_IA, ativo = true)
        }
    }

    @Test fun `consulta enquanto a IA trabalha e para quando muda de etapa`() = runTest(despachante) {
        val lendo = escolherCargo.copy(status = StatusAnalise.LENDO_EDITAL, cargos = emptyList())
        val api = Api(ArrayDeque(listOf(lendo, escolherCargo)))
        val vm = EditalViewModel(2, provedorCom(api), intervaloConsultaMs = 3000)
        runCurrent()
        assertTrue(vm.estado.value.trabalhando)
        advanceTimeBy(3001); runCurrent()
        assertEquals(StatusAnalise.ESCOLHER_CARGO, vm.estado.value.analise!!.status)
        advanceTimeBy(10_000); runCurrent()
        assertEquals("parou de consultar ao sair de LENDO_EDITAL", 2, api.consultas)
    }

    @Test fun `revisao recalcula o resumo e confirma so o que ficou marcado`() = runTest(despachante) {
        val api = Api(ArrayDeque(listOf(pronta)))
        val vm = EditalViewModel(2, provedorCom(api))
        runCurrent()
        val r = vm.estado.value.revisao!!
        val servidor = pronta.resumo!!
        assertTrue(resumoRevisao(r, servidor).startsWith("${servidor.novos} assuntos novos · ${servidor.emComum} em comum"))

        val iComum = r.disciplinas.indexOfFirst { d -> d.assuntos.any { it.assuntoExistenteId != null } }
        val jComum = r.disciplinas[iComum].assuntos.indexOfFirst { it.assuntoExistenteId != null }
        vm.tratarComoNovo(iComum, jComum)
        vm.alternarAssunto(iComum, jComum) // e ainda desmarca
        vm.trocarPeso(r.disciplinas.lastIndex, TipoPeso.BAIXO)
        val depois = vm.estado.value.revisao!!
        assertTrue(resumoRevisao(depois, servidor).startsWith("${servidor.novos} assuntos novos · ${servidor.emComum - 1} em comum"))

        vm.confirmar(); runCurrent()
        val corpo = api.confirmado!!
        assertEquals(pronta.concursoNome, corpo.nome)
        assertEquals(r.disciplinas[iComum].assuntos.size - 1, corpo.disciplinas[iComum].assuntos.size)
        assertEquals(TipoPeso.BAIXO, corpo.disciplinas.last().peso)
        assertTrue(vm.estado.value.confirmada)
    }

    @Test fun `linha da prova em Hoje`() {
        assertEquals("prova em 45 dias", textoProva(45))
        assertEquals("prova amanhã", textoProva(1))
        assertEquals("prova hoje", textoProva(0))
        assertEquals("a prova já passou", textoProva(-3))
        assertEquals("sem data de prova", textoProva(null))
    }
}
