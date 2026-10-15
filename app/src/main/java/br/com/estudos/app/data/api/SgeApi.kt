package br.com.estudos.app.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Os endpoints do SGE que o app consome — tabela de §4 da especificação. */
interface SgeApi {

    @GET("actuator/health")
    suspend fun saude(): Map<String, kotlinx.serialization.json.JsonElement>

    @GET("api/turno/plano")
    suspend fun plano(@Query("extraRecuperacoes") extraRecuperacoes: Int = 0): TurnoPlanoDto

    @POST("api/sessoes")
    suspend fun registrarSessao(@Body corpo: SessaoRequestDto): SessaoDto

    @GET("api/assuntos/{id}/segmentos")
    suspend fun segmentos(@Path("id") assuntoId: Long): List<SegmentoDto>

    @GET("api/parametros")
    suspend fun parametros(): List<ParametroDto>

    @PATCH("api/parametros/{chave}")
    suspend fun atualizarParametro(@Path("chave") chave: String, @Body corpo: AtualizarParametroDto): ParametroDto

    @GET("api/disciplinas")
    suspend fun disciplinas(): List<DisciplinaDto>

    @GET("api/assuntos")
    suspend fun assuntos(@Query("disciplinaId") disciplinaId: Long): List<AssuntoDto>

    @GET("api/assuntos/{id}/fase")
    suspend fun fase(@Path("id") assuntoId: Long): AssuntoFaseDto

    @GET("api/erros")
    suspend fun erros(@Query("assuntoId") assuntoId: Long): List<ErroDto>

    @POST("api/erros")
    suspend fun registrarErro(@Body corpo: ErroRequestDto): ErroDto

    @GET("api/metricas/m1")
    suspend fun m1(@Query("janela") janela: JanelaMetrica): M1Dto

    @GET("api/metricas/m2")
    suspend fun m2(@Query("assuntoId") assuntoId: Long): M2Dto

    @GET("api/metricas/m3")
    suspend fun m3(): M3Dto

    @GET("api/metricas/m4")
    suspend fun m4(): M4Dto

    @GET("api/frente")
    suspend fun frente(): FrenteResumoDto

    @GET("api/frente/sugestoes-divisao")
    suspend fun sugestoesDivisao(): List<SugestaoDivisaoDto>

    // ---- Sprint 18 do SGE (ADR-042, ADR-043) ----

    @GET("api/concursos")
    suspend fun concursos(): List<ConcursoDto>

    /** 204 sem concurso-alvo: corpo nulo. */
    @GET("api/concursos/ativo")
    suspend fun concursoAtivo(): Response<ConcursoDto>

    @POST("api/concursos/{id}/ativar")
    suspend fun ativarConcurso(@Path("id") id: Long): ConcursoDto

    @GET("api/disciplinas/resumo")
    suspend fun disciplinasResumo(): List<DisciplinaResumoDto>

    @GET("api/editais/ia")
    suspend fun iaStatus(): IaStatusDto

    @Multipart
    @POST("api/editais/analises")
    suspend fun enviarEdital(@Part arquivo: MultipartBody.Part): AnaliseEditalDto

    @GET("api/editais/analises")
    suspend fun analises(): List<AnaliseEditalDto>

    @GET("api/editais/analises/{id}")
    suspend fun analise(@Path("id") id: Long): AnaliseEditalDto

    @POST("api/editais/analises/{id}/cargo")
    suspend fun escolherCargo(@Path("id") id: Long, @Body corpo: EscolhaCargoDto): AnaliseEditalDto

    @POST("api/editais/analises/{id}/tentar-de-novo")
    suspend fun tentarDeNovo(@Path("id") id: Long): AnaliseEditalDto

    @POST("api/editais/analises/{id}/confirmar")
    suspend fun confirmarEdital(@Path("id") id: Long, @Body corpo: ConfirmacaoEditalDto): ConcursoDto
}
