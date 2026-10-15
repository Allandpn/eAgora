package br.com.estudos.app.data

import br.com.estudos.app.data.api.*
import br.com.estudos.app.fixture
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** O app desserializa as respostas reais do SGE e traduz os erros como a SPA (especificação §4, §7). */
class ApiTest {
    private lateinit var servidor: MockWebServer
    private lateinit var api: SgeApi

    @Before fun subir() {
        servidor = MockWebServer().apply { start() }
        api = criarApi(servidor.url("/").toString())
    }

    @After fun descer() = servidor.shutdown()

    private fun responder(nome: String) =
        servidor.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(fixture(nome)))

    @Test fun `plano real do turno`() = runTest {
        responder("plano.json")
        val plano = api.plano(4)
        assertEquals("/api/turno/plano?extraRecuperacoes=4", servidor.takeRequest().path)
        assertEquals(8, plano.filaRecuperacao.size)
        assertEquals("Índices", plano.reestudos.single().nome)
        assertEquals("TCP/IP — camadas", plano.blocoConteudo?.nome)
        assertTrue(plano.blocoContinuacao)
        assertEquals(2, plano.segmentoSugerido?.ordem)
        assertEquals(3, plano.segmentoSugerido?.total)
        assertEquals(4L, plano.vencidasAlemDoTeto)
        assertEquals(3L, plano.proximoBloco?.id)
        assertEquals(1L, plano.diasSemEstudar)
    }

    @Test fun `demais respostas reais desserializam`() = runTest {
        responder("health.json"); assertEquals(JsonPrimitive("UP"), api.saude()["status"])
        responder("segmentos.json"); assertEquals(listOf(1, 2, 3), api.segmentos(13).map { it.ordem })
        responder("parametros.json"); assertTrue(api.parametros().any { it.chave == "duracao_bloco_min" })
        responder("disciplinas.json"); assertEquals(2, api.disciplinas().size)
        responder("assuntos.json"); assertEquals(12, api.assuntos(1).size)
        responder("fase.json"); assertEquals(FaseAssunto.FRENTE, api.fase(1).fase)
        responder("erros.json"); assertEquals(CausaErro.INTERPRETACAO, api.erros(1).single().causa)
        responder("m1-global.json"); assertEquals(70.0, api.m1(JanelaMetrica.GLOBAL).linhas.single().percentual!!, 0.001)
        responder("m1-por-disciplina.json"); assertNotNull(api.m1(JanelaMetrica.POR_DISCIPLINA).linhas.single().disciplinaId)
        responder("m2.json"); assertTrue(api.m2(1).series.isNotEmpty())
        responder("m3.json"); assertEquals(1, api.m3().subjetiva.superestimou)
        responder("m4.json"); api.m4()
        responder("frente.json"); assertEquals(13, api.frente().frenteAtual)
        responder("sugestoes-divisao.json"); api.sugestoesDivisao()
        responder("sessao.json"); assertEquals(TipoSessao.ESTUDO, api.registrarSessao(
            SessaoRequestDto(3, TipoSessao.ESTUDO, "2026-10-15", 30, tentativaId = "t"),
        ).tipo)
        responder("parametro.json"); assertEquals("60", api.atualizarParametro("duracao_bloco_min", AtualizarParametroDto("60")).valor)
    }

    @Test fun `corpo do registro omite nulos e leva o tentativaId`() = runTest {
        responder("sessao.json")
        api.registrarSessao(SessaoRequestDto(3, TipoSessao.RECUPERACAO, "2026-10-15", 2,
            previsaoReconstrucao = ResultadoSessao.SUCESSO, resultado = ResultadoSessao.PARCIAL, tentativaId = "abc"))
        val corpo = servidor.takeRequest().body.readUtf8()
        assertTrue(corpo.contains("\"tentativaId\":\"abc\""))
        assertTrue(corpo.contains("\"resultado\":\"PARCIAL\""))
        assertFalse(corpo.contains("null"))
    }

    @Test fun `problem+json vira o detail do servidor`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(422).setHeader("Content-Type", "application/problem+json").setBody(fixture("problema.json")))
        val e = assertThrows(ErroApi::class.java) { kotlinx.coroutines.runBlocking { chamar { api.plano() } } }
        assertEquals("questoesCorretas e questoesTotal são obrigatórios para QUESTOES.", e.message)
        assertEquals("QUESTOES_OBRIGATORIAS", e.codigo)
        assertEquals("questoesTotal", e.campo)
        assertEquals(422, e.status)
    }

    @Test fun `erro sem problem+json ganha texto proprio`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(502).setBody("<html>Bad gateway</html>"))
        val e = assertThrows(ErroApi::class.java) { kotlinx.coroutines.runBlocking { chamar { api.plano() } } }
        assertEquals("O servidor respondeu com erro (502). Tente de novo em instantes.", e.message)
    }

    @Test fun `falha de conexao`() = runTest {
        servidor.shutdown()
        val e = assertThrows(ErroApi::class.java) { kotlinx.coroutines.runBlocking { chamar { api.plano() } } }
        assertTrue(e.semConexao)
    }
}
