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

// ---- Sprint 18 do SGE: concurso-alvo, disciplinas e edital novo (ADR-042, ADR-043) ----

@Serializable
enum class OrigemConcurso { ANALISE_IA, PLANEJAMENTO }

@Serializable
enum class StatusAnalise { LENDO_EDITAL, ESCOLHER_CARGO, SEPARANDO_ASSUNTOS, PRONTA, CONFIRMADA, FALHOU }

@Serializable
data class ConcursoDto(
    val id: Long,
    val nome: String,
    val cargo: String? = null,
    val banca: String? = null,
    val dataProva: String? = null,
    val origem: OrigemConcurso,
    val ativo: Boolean,
    val ativadoEm: String? = null,
    val desativadoEm: String? = null,
    /** Calculado pelo servidor; nulo sem data de prova (02_JORNADAS §4.4). */
    val diasParaProva: Long? = null,
    val totalAssuntos: Int = 0,
    val emComumComAtivo: Long = 0,
)

@Serializable
data class DisciplinaResumoDto(
    val id: Long,
    val nome: String,
    val peso: TipoPeso,
    val backlog: Long = 0,
    val frente: Long = 0,
    val consolidados: Long = 0,
    val total: Long = 0,
)

@Serializable
data class IaStatusDto(val disponivel: Boolean, val motivo: String? = null, val modelo: String? = null)

@Serializable
data class AssuntoPropostoDto(
    val nome: String,
    val assuntoExistenteId: Long? = null,
    val assuntoExistenteNome: String? = null,
    val disciplinaAtualNome: String? = null,
)

@Serializable
data class DisciplinaPropostaDto(
    val nome: String,
    val questoes: Int? = null,
    val peso: TipoPeso,
    val pesoDeterminado: Boolean = true,
    val disciplinaExistenteId: Long? = null,
    val disciplinaExistenteNome: String? = null,
    val assuntos: List<AssuntoPropostoDto> = emptyList(),
)

@Serializable
data class ResumoPropostaDto(val novos: Int = 0, val emComum: Int = 0, val foraDoEdital: Int = 0)

@Serializable
data class AnaliseEditalDto(
    val id: Long,
    val nomeArquivo: String,
    val status: StatusAnalise,
    val etapaFalha: StatusAnalise? = null,
    val erro: String? = null,
    val modelo: String? = null,
    val concursoNome: String? = null,
    val banca: String? = null,
    val dataProva: String? = null,
    val cargos: List<String> = emptyList(),
    val cargoEscolhido: String? = null,
    val proposta: List<DisciplinaPropostaDto>? = null,
    val resumo: ResumoPropostaDto? = null,
    val concursoId: Long? = null,
)

@Serializable
data class EscolhaCargoDto(val cargo: String)

@Serializable
data class AssuntoConfirmadoDto(val nome: String, val assuntoExistenteId: Long?)

@Serializable
data class DisciplinaConfirmadaDto(
    val nome: String,
    val disciplinaExistenteId: Long?,
    val peso: TipoPeso,
    val assuntos: List<AssuntoConfirmadoDto>,
)

@Serializable
data class ConfirmacaoEditalDto(
    val nome: String,
    val cargo: String?,
    val banca: String?,
    val dataProva: String?,
    val disciplinas: List<DisciplinaConfirmadaDto>,
)
