package br.com.estudos.app.ui

import br.com.estudos.app.ApiFalsa
import br.com.estudos.app.ArmazenamentoFalso
import br.com.estudos.app.data.EnvioRegistros
import br.com.estudos.app.data.ItemRecuperacao
import br.com.estudos.app.data.ItemRegistro
import br.com.estudos.app.data.Selecao
import br.com.estudos.app.data.api.*
import br.com.estudos.app.decodificar
import br.com.estudos.app.provedorCom
import br.com.estudos.app.ui.configuracao.validarEndereco
import br.com.estudos.app.ui.hoje.HojeViewModel
import br.com.estudos.app.ui.recuperar.RascunhoRecuperacao
import br.com.estudos.app.ui.recuperar.RecuperarUiState
import br.com.estudos.app.ui.recuperar.RecuperarViewModel
import br.com.estudos.app.ui.recuperar.chaveRascunhoRecuperacao
import br.com.estudos.app.ui.registrar.RegistrarViewModel
import br.com.estudos.app.ui.registrar.chaveRascunhoRegistro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val despachante = UnconfinedTestDispatcher()
    private val planoReal = decodificar("plano.json", TurnoPlanoDto.serializer())
    private val segmentosReais = decodificar("segmentos.json", kotlinx.serialization.builtins.ListSerializer(SegmentoDto.serializer()))
    private var agora = 1_000_000_000L

    @Before fun main() = Dispatchers.setMain(despachante)
    @After fun limpar() = Dispatchers.resetMain()

    /** API que registra os POSTs e pode simular a rede caída. */
    private inner class Api : ApiFalsa() {
        var semRede = false
        val pedidosPlano = mutableListOf<Int>()
        val sessoes = mutableListOf<SessaoRequestDto>()
        override suspend fun plano(extraRecuperacoes: Int): TurnoPlanoDto {
            if (semRede) throw IOException("sem rede")
            pedidosPlano += extraRecuperacoes
            return planoReal
        }
        override suspend fun registrarSessao(corpo: SessaoRequestDto): SessaoDto {
            if (semRede) throw IOException("sem rede")
            sessoes += corpo
            return SessaoDto(1, corpo.assuntoId, corpo.tipo, corpo.data, corpo.resultado, corpo.tentativaId)
        }
        override suspend fun segmentos(assuntoId: Long) = segmentosReais
        override suspend fun parametros() = listOf(ParametroDto("duracao_bloco_min", "45"))
    }

    // --- Hoje ---

    @Test fun `hoje carrega, guarda o plano e drena mais um teto por pedido`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso()
        val vm = HojeViewModel(provedorCom(api, arm), arm, Selecao()) { agora }
        vm.abrir()
        assertEquals(planoReal, vm.estado.value.plano)
        assertNotNull(arm.plano)
        vm.drenarMais(); vm.drenarMais()
        assertEquals(listOf(0, 8, 16), api.pedidosPlano) // passo = a fila sem pedido (o teto diário)
        vm.abrir()
        assertEquals(0, api.pedidosPlano.last()) // voltar para Hoje recomeça do teto normal (D-40)
    }

    @Test fun `hoje sem rede mostra o ultimo plano com a hora`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso()
        val vm = HojeViewModel(provedorCom(api, arm), arm, Selecao()) { agora }
        vm.abrir()
        api.semRede = true
        vm.carregar()
        assertEquals(planoReal, vm.estado.value.plano)
        assertEquals(agora, vm.estado.value.planoGuardadoEm)
        assertNull(vm.estado.value.erro)
    }

    @Test fun `hoje sem rede e sem plano guardado mostra o erro`() = runTest(despachante) {
        val api = Api().apply { semRede = true }; val arm = ArmazenamentoFalso()
        val vm = HojeViewModel(provedorCom(api, arm), arm, Selecao()) { agora }
        vm.abrir()
        assertTrue(vm.estado.value.erro!!.startsWith("Sem conexão"))
    }

    @Test fun `card do bloco leva o segmento sugerido para Registrar (D-55)`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso(); val sel = Selecao()
        val vm = HojeViewModel(provedorCom(api, arm), arm, sel) { agora }
        vm.abrir()
        assertEquals(13L, vm.selecionarBloco(outro = false))
        assertEquals(ItemRegistro(13, "TCP/IP — camadas", 2), sel.registro)
        vm.selecionarBloco(outro = true)
        assertEquals(3L, sel.registro?.segmentoSugeridoId)
    }

    // --- Recuperar ---

    private fun envio(api: Api, arm: ArmazenamentoFalso) =
        EnvioRegistros(CoroutineScope(despachante), provedorCom(api, arm), arm)

    @Test fun `recuperar sem item nem rascunho e sem dados`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso()
        val vm = RecuperarViewModel(3, Selecao(), arm, envio(api, arm)) { agora }
        assertEquals(RecuperarUiState.SemDados, vm.estado.value)
    }

    @Test fun `recuperar mede o tempo da tela, envia e apaga o rascunho`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso()
        val sel = Selecao().apply { recuperacao = ItemRecuperacao(3, "Transações", 1, "2026-09-30") }
        val vm = RecuperarViewModel(3, sel, arm, envio(api, arm)) { agora }
        assertNotNull(arm.rascunhos[chaveRascunhoRecuperacao(3)])
        assertFalse(vm.confirmar(ResultadoSessao.SUCESSO)) // etapa 1: não há resultado a confirmar
        vm.prever(ResultadoSessao.SUCESSO)
        agora += 7 * 60_000 + 20_000
        assertTrue(vm.confirmar(ResultadoSessao.PARCIAL))
        val corpo = api.sessoes.single()
        assertEquals(TipoSessao.RECUPERACAO, corpo.tipo)
        assertEquals(7, corpo.tempoMinutos)
        assertEquals(ResultadoSessao.SUCESSO, corpo.previsaoReconstrucao)
        assertEquals(ResultadoSessao.PARCIAL, corpo.resultado)
        assertNull(arm.rascunhos[chaveRascunhoRecuperacao(3)])
    }

    @Test fun `recuperar reabre do rascunho com a etapa, a previsao e o mesmo tentativaId`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso()
        val sel = Selecao().apply { recuperacao = ItemRecuperacao(3, "Transações", 1, null) }
        val primeira = RecuperarViewModel(3, sel, arm, envio(api, arm)) { agora }
        primeira.prever(ResultadoSessao.FALHA)
        val id = (primeira.estado.value as RecuperarUiState.Aberta).rascunho.tentativaId
        // O Android matou o app: a seleção em memória sumiu.
        val segunda = RecuperarViewModel(3, Selecao(), arm, envio(api, arm)) { agora + 60_000 }
        val r = (segunda.estado.value as RecuperarUiState.Aberta).rascunho
        assertEquals(2, r.etapa)
        assertEquals(ResultadoSessao.FALHA, r.previsao)
        assertEquals(id, r.tentativaId)
    }

    @Test fun `falha de envio guarda o rascunho e reenviar usa o mesmo tentativaId (D-45)`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso(); val env = envio(api, arm)
        val sel = Selecao().apply { recuperacao = ItemRecuperacao(3, "Transações", 1, null) }
        val vm = RecuperarViewModel(3, sel, arm, env) { agora }
        vm.prever(ResultadoSessao.SUCESSO)
        api.semRede = true
        vm.confirmar(ResultadoSessao.SUCESSO)
        assertEquals(Rotas.recuperar(3), env.falhas.value.single().rota)
        assertNotNull(arm.rascunhos[chaveRascunhoRecuperacao(3)])
        val id = (vm.estado.value as RecuperarUiState.Aberta).rascunho.tentativaId
        api.semRede = false
        val reaberta = RecuperarViewModel(3, Selecao(), arm, env) { agora }
        reaberta.confirmar(ResultadoSessao.SUCESSO)
        assertEquals(id, api.sessoes.single().tentativaId)
        assertTrue(env.falhas.value.isEmpty())
    }

    @Test fun `rascunho com mais de 24 h e descartado`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso()
        arm.rascunhos[chaveRascunhoRecuperacao(3)] = JsonSge.encodeToString(
            RascunhoRecuperacao.serializer(),
            RascunhoRecuperacao(3, "Transações", 1, etapa = 2, previsao = ResultadoSessao.SUCESSO, tentativaId = "velho", inicioEm = agora - 25 * 3_600_000L),
        )
        val vm = RecuperarViewModel(3, Selecao(), arm, envio(api, arm)) { agora }
        assertEquals(RecuperarUiState.SemDados, vm.estado.value)
        assertNull(arm.rascunhos[chaveRascunhoRecuperacao(3)])
    }

    @Test fun `voltar sai sem registrar e apaga o rascunho`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso()
        val sel = Selecao().apply { recuperacao = ItemRecuperacao(3, "Transações", 1, null) }
        val vm = RecuperarViewModel(3, sel, arm, envio(api, arm)) { agora }
        vm.sair()
        assertTrue(arm.rascunhos.isEmpty())
        assertTrue(api.sessoes.isEmpty())
    }

    // --- Registrar ---

    @Test fun `registrar conteudo marca o segmento sugerido e preenche o tempo dele`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso()
        val sel = Selecao().apply { registro = ItemRegistro(13, "TCP/IP — camadas", segmentoSugeridoId = 3) }
        val vm = RegistrarViewModel("conteudo", 13, sel, provedorCom(api, arm), arm, envio(api, arm)) { agora }
        val r = vm.estado.value.rascunho!!
        assertEquals(3L, r.segmentoId)
        assertEquals("40", r.tempo)
        vm.selecionarSegmento(null)
        assertEquals("45", vm.estado.value.rascunho!!.tempo) // sem bloco: duracao_bloco_min
        vm.mudarTempo("50")
        vm.selecionarSegmento(1)
        assertEquals("50", vm.estado.value.rascunho!!.tempo) // editado: nada sobrescreve
        assertTrue(vm.registrar())
        val corpo = api.sessoes.single()
        assertEquals(TipoSessao.ESTUDO, corpo.tipo)
        assertEquals(1L, corpo.segmentoId)
        assertEquals(50, corpo.tempoMinutos)
        assertNull(arm.rascunhos[chaveRascunhoRegistro(13)])
    }

    @Test fun `registrar questoes exige so a forma do tempo`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso()
        val sel = Selecao().apply { registro = ItemRegistro(3, "Transações") }
        val vm = RegistrarViewModel("questoes", 3, sel, provedorCom(api, arm), arm, envio(api, arm)) { agora }
        assertEquals("", vm.estado.value.rascunho!!.tempo) // questões: sem estimativa honesta
        assertFalse(vm.registrar())
        assertEquals("Informe o tempo em minutos.", vm.estado.value.erro)
        vm.selecionarFormato(FormatoBanca.CERTO_ERRADO)
        vm.mudarFeitas("20"); vm.mudarCertas("15"); vm.mudarPrevisao("70"); vm.mudarTempo("25")
        assertTrue(vm.registrar())
        val c = api.sessoes.single()
        assertEquals(TipoSessao.QUESTOES, c.tipo)
        assertEquals(FormatoBanca.CERTO_ERRADO, c.formato)
        assertEquals(20, c.questoesTotal); assertEquals(15, c.questoesCorretas); assertEquals(70, c.previsaoPercentual)
    }

    @Test fun `trocar de tipo gera tentativa nova`() = runTest(despachante) {
        val api = Api(); val arm = ArmazenamentoFalso()
        val sel = Selecao().apply { registro = ItemRegistro(3, "Transações") }
        val vm = RegistrarViewModel("conteudo", 3, sel, provedorCom(api, arm), arm, envio(api, arm)) { agora }
        val antes = vm.estado.value.rascunho!!.tentativaId
        vm.selecionarTipo("flashcards")
        assertNotEquals(antes, vm.estado.value.rascunho!!.tentativaId)
    }

    // --- Configuração ---

    @Test fun `endereco do servidor`() {
        assertEquals("https://pi.tail1.ts.net", validarEndereco(" https://pi.tail1.ts.net/ ", aceitaHttp = false).getOrNull())
        assertTrue(validarEndereco("http://192.168.0.2:8080", aceitaHttp = false).isFailure)
        assertEquals("http://192.168.0.2:8080", validarEndereco("http://192.168.0.2:8080", aceitaHttp = true).getOrNull())
        assertTrue(validarEndereco("pi.tail1.ts.net", aceitaHttp = true).isFailure)
        assertTrue(validarEndereco("", aceitaHttp = true).isFailure)
    }
}
