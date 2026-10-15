package br.com.estudos.app

import android.app.Application
import br.com.estudos.app.lembrete.criarCanalLembrete

class EstudosApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        criarCanalLembrete(this)
    }
}
