package br.com.estudos.app

import br.com.estudos.app.data.Configuracao
import br.com.estudos.app.data.LembreteConfig
import br.com.estudos.app.data.PlanoCache
import br.com.estudos.app.data.ProvedorApi
import br.com.estudos.app.data.Rascunhos
import br.com.estudos.app.data.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/** Lê uma resposta real do SGE (capturada em 2026-10-15, especificação §7). */
fun fixture(nome: String): String =
    File(requireNotNull(object {}.javaClass.classLoader?.getResource("sge/$nome")) { "fixture $nome" }.toURI()).readText()

inline fun <reified T> decodificar(nome: String, serializer: kotlinx.serialization.KSerializer<T>): T =
    JsonSge.decodeFromString(serializer, fixture(nome))

/** DataStore em memória. */
class ArmazenamentoFalso(url: String? = "https://sge.exemplo") : Configuracao, Rascunhos, PlanoCache {
    private val _url = MutableStateFlow(url)
    private val _lembrete = MutableStateFlow(LembreteConfig())
    val rascunhos = mutableMapOf<String, String>()
    var plano: Pair<String, Long>? = null

    override val urlServidor: Flow<String?> = _url
    override suspend fun urlServidorAtual() = _url.value
    override suspend fun salvarUrlServidor(url: String) { _url.value = url }
    override val lembrete: Flow<LembreteConfig> = _lembrete
    override suspend fun lembreteAtual() = _lembrete.value
    override suspend fun salvarLembrete(config: LembreteConfig) { _lembrete.value = config }
    override suspend fun ler(chave: String) = rascunhos[chave]
    override suspend fun salvar(chave: String, json: String) { rascunhos[chave] = json }
    override suspend fun apagar(chave: String) { rascunhos.remove(chave) }
    override suspend fun ler() = plano
    override suspend fun salvar(json: String, instante: Long) { plano = json to instante }
}

/** API falsa: cada teste sobrescreve só o que usa; o resto falha alto. */
open class ApiFalsa : SgeApi {
    override suspend fun saude(): Map<String, kotlinx.serialization.json.JsonElement> = TODO()
    override suspend fun plano(extraRecuperacoes: Int): TurnoPlanoDto = TODO()
    override suspend fun registrarSessao(corpo: SessaoRequestDto): SessaoDto = TODO()
    override suspend fun segmentos(assuntoId: Long): List<SegmentoDto> = TODO()
    override suspend fun parametros(): List<ParametroDto> = TODO()
    override suspend fun atualizarParametro(chave: String, corpo: AtualizarParametroDto): ParametroDto = TODO()
    override suspend fun disciplinas(): List<DisciplinaDto> = TODO()
    override suspend fun assuntos(disciplinaId: Long): List<AssuntoDto> = TODO()
    override suspend fun fase(assuntoId: Long): AssuntoFaseDto = TODO()
    override suspend fun erros(assuntoId: Long): List<ErroDto> = TODO()
    override suspend fun registrarErro(corpo: ErroRequestDto): ErroDto = TODO()
    override suspend fun m1(janela: JanelaMetrica): M1Dto = TODO()
    override suspend fun m2(assuntoId: Long): M2Dto = TODO()
    override suspend fun m3(): M3Dto = TODO()
    override suspend fun m4(): M4Dto = TODO()
    override suspend fun frente(): FrenteResumoDto = TODO()
    override suspend fun sugestoesDivisao(): List<SugestaoDivisaoDto> = TODO()
    override suspend fun concursos(): List<ConcursoDto> = TODO()
    override suspend fun concursoAtivo(): retrofit2.Response<ConcursoDto> = TODO()
    override suspend fun ativarConcurso(id: Long): ConcursoDto = TODO()
    override suspend fun disciplinasResumo(): List<DisciplinaResumoDto> = TODO()
    override suspend fun iaStatus(): IaStatusDto = TODO()
    override suspend fun enviarEdital(arquivo: okhttp3.MultipartBody.Part): AnaliseEditalDto = TODO()
    override suspend fun analises(): List<AnaliseEditalDto> = TODO()
    override suspend fun analise(id: Long): AnaliseEditalDto = TODO()
    override suspend fun escolherCargo(id: Long, corpo: EscolhaCargoDto): AnaliseEditalDto = TODO()
    override suspend fun tentarDeNovo(id: Long): AnaliseEditalDto = TODO()
    override suspend fun confirmarEdital(id: Long, corpo: ConfirmacaoEditalDto): ConcursoDto = TODO()
}

fun provedorCom(api: SgeApi, armazenamento: ArmazenamentoFalso = ArmazenamentoFalso()) = ProvedorApi(armazenamento) { api }
