package br.com.estudos.app.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta da SPA (04_FRONTEND §8A do SGE): primária por papel, alerta separado.
private val Primaria = Color(0xFF0694A2)
private val PrimariaEscura = Color(0xFF3CC3CF)

data class CoresExtras(
    val alerta: Color,
    val alertaFundo: Color,
    val retornoFundo: Color,
    val texto2: Color,
    val borda: Color,
)

private val ExtrasClaro = CoresExtras(
    alerta = Color(0xFFE2530A), alertaFundo = Color(0xFFFFF4ED), retornoFundo = Color(0xFFE6F7F8),
    texto2 = Color(0xFF56696B), borda = Color(0xFFD6E3E4),
)
private val ExtrasEscuro = CoresExtras(
    alerta = Color(0xFFFF8A4C), alertaFundo = Color(0xFF3A2014), retornoFundo = Color(0xFF11343A),
    texto2 = Color(0xFF96A9AB), borda = Color(0xFF26393B),
)

val LocalCoresExtras = staticCompositionLocalOf { ExtrasClaro }

@Composable
fun TemaEstudos(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    val esquema = if (escuro) {
        darkColorScheme(
            primary = PrimariaEscura, onPrimary = Color(0xFF00363C),
            background = Color(0xFF0D1718), surface = Color(0xFF142224),
            onBackground = Color(0xFFE3EEEF), onSurface = Color(0xFFE3EEEF),
            surfaceVariant = Color(0xFF1B2C2E), onSurfaceVariant = Color(0xFF96A9AB),
        )
    } else {
        lightColorScheme(
            primary = Primaria, onPrimary = Color.White,
            background = Color(0xFFF6F9F9), surface = Color.White,
            onBackground = Color(0xFF12282B), onSurface = Color(0xFF12282B),
            surfaceVariant = Color(0xFFEEF3F3), onSurfaceVariant = Color(0xFF56696B),
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalCoresExtras provides if (escuro) ExtrasEscuro else ExtrasClaro) {
        MaterialTheme(colorScheme = esquema, content = conteudo)
    }
}
