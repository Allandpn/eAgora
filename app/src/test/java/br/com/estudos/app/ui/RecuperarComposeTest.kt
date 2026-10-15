package br.com.estudos.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.estudos.app.data.api.ResultadoSessao
import br.com.estudos.app.ui.recuperar.ConteudoRecuperar
import br.com.estudos.app.ui.recuperar.RascunhoRecuperacao
import br.com.estudos.app.ui.recuperar.RecuperarUiState
import br.com.estudos.app.ui.tema.TemaEstudos
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regra vinculante de 02_JORNADAS §4.1: na etapa 1 o resultado não existe na árvore. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecuperarComposeTest {
    @get:Rule val regra = createComposeRule()

    @Test fun `etapa 1 nao contem o resultado, etapa 2 mostra a previsao`() {
        var confirmado: ResultadoSessao? = null
        regra.setContent {
            var r by remember { mutableStateOf(RascunhoRecuperacao(3, "Transações", 1, "2026-09-30", tentativaId = "t", inicioEm = 0)) }
            TemaEstudos(escuro = false) {
                ConteudoRecuperar(
                    estado = RecuperarUiState.Aberta(r),
                    aoPrever = { r = r.copy(etapa = 2, previsao = it) },
                    aoConfirmar = { confirmado = it },
                    aoRegistrarErro = {}, aoVoltarParaHoje = {},
                )
            }
        }
        regra.onNodeWithText("O que você espera?").assertIsDisplayed()
        regra.onNodeWithText("Nível 1 · previsto para 30/09/2026").assertIsDisplayed()
        regra.onNodeWithTag("etapa2").assertDoesNotExist()
        regra.onNodeWithText("Conseguiu reconstruir?").assertDoesNotExist()
        regra.onNodeWithText("Reconstruí").assertDoesNotExist()
        regra.onNodeWithText("+ registrar um erro").assertDoesNotExist()

        regra.onNodeWithText("Vai faltar um pedaço").performClick()

        regra.onNodeWithTag("etapa1").assertDoesNotExist()
        regra.onNodeWithText("Você previu: vai faltar um pedaço").assertIsDisplayed()
        regra.onNodeWithText("Reconstruí").performClick()
        assertEquals(ResultadoSessao.SUCESSO, confirmado)
    }
}
