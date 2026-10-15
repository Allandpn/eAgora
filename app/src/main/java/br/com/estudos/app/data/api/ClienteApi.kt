package br.com.estudos.app.data.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** JSON tolerante a campo novo do servidor (A-002). */
val JsonSge = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

/**
 * Erro que a tela mostra como veio — mesma tradução de `api.js` da SPA
 * (especificação §4): `problem+json` usa o `detail` do servidor; erro sem esse
 * formato e falha de conexão ganham texto próprio.
 */
class ErroApi(
    mensagem: String,
    val codigo: String? = null,
    val campo: String? = null,
    val status: Int = 0,
) : Exception(mensagem) {
    val semConexao: Boolean get() = codigo == REDE_INDISPONIVEL

    companion object {
        const val REDE_INDISPONIVEL = "REDE_INDISPONIVEL"
        const val ERRO_SERVIDOR = "ERRO_SERVIDOR"
    }
}

fun criarApi(urlBase: String, cliente: OkHttpClient = clientePadrao()): SgeApi {
    val base = if (urlBase.endsWith("/")) urlBase else "$urlBase/"
    return Retrofit.Builder()
        .baseUrl(base)
        .client(cliente)
        .addConverterFactory(JsonSge.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SgeApi::class.java)
}

fun clientePadrao(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    // Ponto de interceptação reservado para autenticação, se um dia existir (ADR-040 do SGE).
    .build()

/** Executa uma chamada e traduz qualquer falha em [ErroApi]. */
suspend fun <T> chamar(bloco: suspend () -> T): T = try {
    bloco()
} catch (e: HttpException) {
    throw traduzir(e)
} catch (e: IOException) {
    throw ErroApi("Sem conexão com o servidor. Confira a rede e o Tailscale e tente de novo.", ErroApi.REDE_INDISPONIVEL)
} catch (e: kotlinx.serialization.SerializationException) {
    throw ErroApi("A resposta do servidor não está no formato esperado. Atualize o app.", ErroApi.ERRO_SERVIDOR)
}

internal fun traduzir(e: HttpException): ErroApi {
    val corpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val tipo = e.response()?.errorBody()?.contentType()?.toString().orEmpty()
    val problema = if (corpo != null && (tipo.contains("json") || corpo.trimStart().startsWith("{"))) {
        runCatching { JsonSge.decodeFromString(ProblemaDto.serializer(), corpo) }.getOrNull()
    } else null
    return if (problema?.detail != null) {
        ErroApi(problema.detail, problema.codigo, problema.campo, e.code())
    } else {
        ErroApi("O servidor respondeu com erro (${e.code()}). Tente de novo em instantes.", ErroApi.ERRO_SERVIDOR, status = e.code())
    }
}
