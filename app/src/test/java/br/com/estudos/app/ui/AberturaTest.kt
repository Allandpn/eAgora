package br.com.estudos.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import br.com.estudos.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fumaça do app inteiro: a primeira abertura cai na configuração do servidor (especificação §3.8). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AberturaTest {
    @get:Rule val regra = createAndroidComposeRule<MainActivity>()

    @Test fun `primeira abertura pede o servidor e recusa endereco sem esquema`() {
        regra.waitUntil(5_000) { regra.onAllNodesWithTextExists("Servidor do SGE") }
        regra.onNodeWithText("Endereço").performTextInput("raspberrypi.tail1234.ts.net")
        regra.onNodeWithText("Testar e salvar").performClick()
        regra.onNodeWithText("O endereço começa com https://").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithTextExists(texto: String) =
        onAllNodes(androidx.compose.ui.test.hasText(texto)).fetchSemanticsNodes().isNotEmpty()
}
