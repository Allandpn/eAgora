package br.com.estudos.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "estudos")

/** Configuração do app (A-005): servidor e lembrete. */
interface Configuracao {
    val urlServidor: Flow<String?>
    suspend fun urlServidorAtual(): String?
    suspend fun salvarUrlServidor(url: String)
    val lembrete: Flow<LembreteConfig>
    suspend fun lembreteAtual(): LembreteConfig
    suspend fun salvarLembrete(config: LembreteConfig)
}

data class LembreteConfig(val ativo: Boolean = false, val hora: Int = 19, val minuto: Int = 0)

/** Rascunhos de Recuperar/Registrar (especificação §5.3), JSON por chave `tipo:assunto`. */
interface Rascunhos {
    suspend fun ler(chave: String): String?
    suspend fun salvar(chave: String, json: String)
    suspend fun apagar(chave: String)
}

/** Último plano carregado com sucesso, para Hoje sem rede (especificação §5.4). */
interface PlanoCache {
    suspend fun ler(): Pair<String, Long>?
    suspend fun salvar(json: String, instante: Long)
}

class ArmazenamentoDataStore(private val context: Context) : Configuracao, Rascunhos, PlanoCache {
    private val store get() = context.dataStore

    private object Chaves {
        val url = stringPreferencesKey("servidor_url")
        val lembreteAtivo = booleanPreferencesKey("lembrete_ativo")
        val lembreteHora = intPreferencesKey("lembrete_hora")
        val lembreteMinuto = intPreferencesKey("lembrete_minuto")
        val planoJson = stringPreferencesKey("plano_json")
        val planoInstante = longPreferencesKey("plano_instante")
        fun rascunho(chave: String) = stringPreferencesKey("rascunho:$chave")
    }

    override val urlServidor: Flow<String?> = store.data.map { it[Chaves.url] }
    override suspend fun urlServidorAtual(): String? = urlServidor.first()
    override suspend fun salvarUrlServidor(url: String) {
        store.edit { it[Chaves.url] = url }
    }

    override val lembrete: Flow<LembreteConfig> = store.data.map {
        LembreteConfig(it[Chaves.lembreteAtivo] ?: false, it[Chaves.lembreteHora] ?: 19, it[Chaves.lembreteMinuto] ?: 0)
    }
    override suspend fun lembreteAtual(): LembreteConfig = lembrete.first()
    override suspend fun salvarLembrete(config: LembreteConfig) {
        store.edit {
            it[Chaves.lembreteAtivo] = config.ativo
            it[Chaves.lembreteHora] = config.hora
            it[Chaves.lembreteMinuto] = config.minuto
        }
    }

    override suspend fun ler(chave: String): String? = store.data.first()[Chaves.rascunho(chave)]
    override suspend fun salvar(chave: String, json: String) {
        store.edit { it[Chaves.rascunho(chave)] = json }
    }
    override suspend fun apagar(chave: String) {
        store.edit { it.remove(Chaves.rascunho(chave)) }
    }

    override suspend fun ler(): Pair<String, Long>? {
        val prefs = store.data.first()
        val json = prefs[Chaves.planoJson] ?: return null
        return json to (prefs[Chaves.planoInstante] ?: 0L)
    }
    override suspend fun salvar(json: String, instante: Long) {
        store.edit {
            it[Chaves.planoJson] = json
            it[Chaves.planoInstante] = instante
        }
    }
}
