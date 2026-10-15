package br.com.estudos.app.data.api

import kotlinx.serialization.Serializable

// Espelho dos DTOs do SGE (src/main/java/br/com/estudos/**/*Response.java e *Request.java).
// Mesmos nomes de campo; datas como texto ISO "AAAA-MM-DD". Campo novo no servidor
// é ignorado (ignoreUnknownKeys, A-002) — campo que o app lê e sumir do servidor
// é mudança de API (especificação §4).

@Serializable
enum class TipoPeso { ALTO, MEDIO, BAIXO }

@Serializable
enum class TipoSessao { ESTUDO, QUESTOES, FLASHCARDS, RECUPERACAO }

@Serializable
enum class ResultadoSessao { SUCESSO, PARCIAL, FALHA }

@Serializable
enum class FormatoBanca { MULTIPLA_ESCOLHA, CERTO_ERRADO }

@Serializable
enum class FaseAssunto { BACKLOG, FRENTE, CONSOLIDADO }

@Serializable
enum class CausaErro { FALTA_CONHECIMENTO, ESQUECIMENTO, INTERPRETACAO, DESATENCAO, PEGADINHA, CHUTE, GESTAO_TEMPO }

@Serializable
enum class NivelConfianca { BAIXA, MEDIA, ALTA }

@Serializable
enum class JanelaMetrica { GLOBAL, POR_DISCIPLINA }

@Serializable
data class AssuntoDto(
    val id: Long,
    val disciplinaId: Long,
    val nome: String,
    val peso: TipoPeso,
    val dificuldadePercebida: Int? = null,
    val ordem: Int? = null,
    val ativo: Boolean = true,
    val chaveExterna: String? = null,
)

@Serializable
data class DisciplinaDto(
    val id: Long,
    val nome: String,
    val peso: TipoPeso,
    val ativo: Boolean = true,
)

@Serializable
data class FilaRecuperacaoItemDto(
    val assuntoId: Long,
    val nome: String,
    val nivel: Int,
    val dataPrevista: String,
    val diasAtraso: Long,
)

@Serializable
data class ReestudoItemDto(
    val assuntoId: Long,
    val nome: String,
    val nivel: Int,
    val dataFalha: String,
)

@Serializable
data class SegmentoSugeridoDto(
    val id: Long,
    val ordem: Int,
    val total: Int,
    val arquivo: String,
    val paginaInicial: Int? = null,
    val paginaFinal: Int? = null,
    val tempoEstimadoMin: Int? = null,
)

@Serializable
data class TurnoPlanoDto(
    val filaRecuperacao: List<FilaRecuperacaoItemDto> = emptyList(),
    val reestudos: List<ReestudoItemDto> = emptyList(),
    val blocoConteudo: AssuntoDto? = null,
    val blocoContinuacao: Boolean = false,
    val segmentoSugerido: SegmentoSugeridoDto? = null,
    val alertaRepresamento: Boolean = false,
    val diasSemEstudar: Long? = null,
    val represado: Long = 0,
    val estimativaDiasParaNormalizar: Long = 0,
    val vencidasAlemDoTeto: Long = 0,
    val proximoBloco: SegmentoSugeridoDto? = null,
    val antecipaveis: List<FilaRecuperacaoItemDto> = emptyList(),
)

@Serializable
data class SegmentoDto(
    val id: Long,
    val assuntoId: Long,
    val chaveExterna: String? = null,
    val ordem: Int,
    val arquivo: String,
    val paginaInicial: Int? = null,
    val paginaFinal: Int? = null,
    val tempoEstimadoMin: Int? = null,
)

@Serializable
data class SessaoRequestDto(
    val assuntoId: Long,
    val tipo: TipoSessao,
    val data: String,
    val tempoMinutos: Int,
    val questoesCorretas: Int? = null,
    val questoesTotal: Int? = null,
    val formato: FormatoBanca? = null,
    val previsaoPercentual: Int? = null,
    val previsaoReconstrucao: ResultadoSessao? = null,
    val resultado: ResultadoSessao? = null,
    val tentativaId: String,
    val segmentoId: Long? = null,
)

@Serializable
data class SessaoDto(
    val id: Long,
    val assuntoId: Long,
    val tipo: TipoSessao,
    val data: String,
    val resultado: ResultadoSessao? = null,
    val tentativaId: String? = null,
)

@Serializable
data class AssuntoFaseDto(val assuntoId: Long, val fase: FaseAssunto)

@Serializable
data class ErroRequestDto(
    val assuntoId: Long,
    val sessaoId: Long? = null,
    val descricao: String,
    val causa: CausaErro?,
    val confianca: NivelConfianca?,
)

@Serializable
data class ErroDto(
    val id: Long,
    val assuntoId: Long,
    val sessaoId: Long? = null,
    val descricao: String,
    val causa: CausaErro,
    val confianca: NivelConfianca,
    val resolvido: Boolean = false,
)

@Serializable
data class M1LinhaDto(
    val disciplinaId: Long? = null,
    val formato: FormatoBanca,
    val acertos: Int,
    val total: Int,
    val confiavel: Boolean = false,
    val percentual: Double? = null,
)

@Serializable
data class M1Dto(val janela: JanelaMetrica, val linhas: List<M1LinhaDto> = emptyList())

@Serializable
data class M2SerieDto(
    val tipo: TipoSessao,
    val percentualPrimeira: Double? = null,
    val percentualMediaSeguintes: Double? = null,
    val totalObservacoes: Int = 0,
)

@Serializable
data class M2Dto(val assuntoId: Long, val series: List<M2SerieDto> = emptyList())

@Serializable
data class M3ObjetivaDto(val mediaDesvio: Double = 0.0, val n: Int = 0)

@Serializable
data class M3SubjetivaDto(val superestimou: Int = 0, val subestimou: Int = 0, val acertouPrevisao: Int = 0, val n: Int = 0)

@Serializable
data class M3Dto(val objetiva: M3ObjetivaDto, val subjetiva: M3SubjetivaDto)

@Serializable
data class M4Dto(val percentual: Double? = null, val dentroDoPrazo: Int = 0, val totalCumpridas: Int = 0)

@Serializable
data class FrenteResumoDto(
    val frenteAtual: Int,
    val tetoGlobalFrente: Int,
    val backlog: Int,
    val consolidados: Int,
    val represado: Long,
    val tetoDiario: Int,
    val alertaRepresamento: Boolean,
    val estimativaDiasParaNormalizar: Long,
    val avisoTetoInsuficiente: Boolean,
    val entraramNaSemana: Int = 0,
    val consolidaramNaSemana: Int = 0,
)

@Serializable
data class SugestaoDivisaoDto(
    val assuntoId: Long,
    val nome: String,
    val disciplinaId: Long,
    val disciplina: String,
    val parciaisSeguidos: Int,
)

@Serializable
data class ParametroDto(
    val chave: String,
    val valor: String,
    val descricao: String? = null,
    val atualizadoEm: String? = null,
)

@Serializable
data class AtualizarParametroDto(val valor: String)

/** application/problem+json do SGE (ADR-026): `detail`, `codigo`, `campo`. */
@Serializable
data class ProblemaDto(
    val detail: String? = null,
    val codigo: String? = null,
    val campo: String? = null,
    val status: Int? = null,
)
