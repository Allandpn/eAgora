package br.com.estudos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.estudos.app.ui.NavegacaoApp
import br.com.estudos.app.ui.tema.TemaEstudos

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as EstudosApp).container
        setContent {
            TemaEstudos { NavegacaoApp(container) }
        }
    }
}
