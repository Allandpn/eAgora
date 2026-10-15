package br.com.estudos.app

import android.content.Context
import br.com.estudos.app.data.ArmazenamentoDataStore
import br.com.estudos.app.data.EnvioRegistros
import br.com.estudos.app.data.ProvedorApi
import br.com.estudos.app.data.Selecao
import br.com.estudos.app.lembrete.AgendadorLembrete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Injeção de dependência manual (A-003): tudo o que as telas e o worker usam. */
class AppContainer(context: Context) {
    val armazenamento = ArmazenamentoDataStore(context.applicationContext)
    val escopoApp = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val provedorApi = ProvedorApi(armazenamento)
    val selecao = Selecao()
    val envio = EnvioRegistros(escopoApp, provedorApi, armazenamento)
    val agendadorLembrete = AgendadorLembrete(context.applicationContext)
    val agora: () -> Long = System::currentTimeMillis
}
