package br.com.estudos.app.data

import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.SgeApi
import br.com.estudos.app.data.api.criarApi

/** Entrega a API do servidor configurado; recria o cliente quando o endereço muda (A-002). */
class ProvedorApi(
    private val configuracao: Configuracao,
    private val fabrica: (String) -> SgeApi = { criarApi(it) },
) {
    @Volatile private var atual: Pair<String, SgeApi>? = null

    suspend fun api(): SgeApi {
        val url = configuracao.urlServidorAtual()
            ?: throw ErroApi("Configure o endereço do servidor em Ajustes.", "SEM_SERVIDOR")
        atual?.let { (u, api) -> if (u == url) return api }
        return fabrica(url).also { atual = url to it }
    }
}
