package br.com.estudos.app.ui.comum

import br.com.estudos.app.data.api.CausaErro
import br.com.estudos.app.data.api.FaseAssunto
import br.com.estudos.app.data.api.FormatoBanca
import br.com.estudos.app.data.api.NivelConfianca
import br.com.estudos.app.data.api.ResultadoSessao
import br.com.estudos.app.data.api.SegmentoDto
import br.com.estudos.app.data.api.SegmentoSugeridoDto
import br.com.estudos.app.data.api.TipoPeso
import br.com.estudos.app.data.api.TipoSessao

// Os mesmos textos da SPA (index.html, pages.js, api.js) — uma tela não pode dizer
// uma coisa na web e outra no app. As âncoras de Recuperar são as de 02_JORNADAS §4.1.

/** "Bloco 2 de 5 · p. 31–60 · ~60 min" — igual a descreverSegmento() de api.js. */
fun descreverSegmento(ordem: Int, total: Int?, paginaInicial: Int?, paginaFinal: Int?, tempoEstimadoMin: Int?): String {
    val partes = mutableListOf(if (total != null) "Bloco $ordem de $total" else "Bloco $ordem")
    if (paginaInicial != null && paginaFinal != null) partes += "p. $paginaInicial–$paginaFinal"
    if (tempoEstimadoMin != null) partes += "~$tempoEstimadoMin min"
    return partes.joinToString(" · ")
}

fun SegmentoSugeridoDto.descricao() = descreverSegmento(ordem, total, paginaInicial, paginaFinal, tempoEstimadoMin)
fun SegmentoDto.descricao() = descreverSegmento(ordem, null, paginaInicial, paginaFinal, tempoEstimadoMin)

/** Link que o Android consegue abrir (o SGE guarda, nunca abre — 02_JORNADAS §1.1). */
fun linkAbrivel(arquivo: String?): String? =
    arquivo?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

/** "2026-10-05" → "05/10/2026". */
fun dataBr(iso: String): String = iso.split("-").reversed().joinToString("/")

fun plural(n: Long, singular: String, plural: String) = if (n == 1L) singular else plural

val TipoPeso.rotulo get() = when (this) { TipoPeso.ALTO -> "Alto"; TipoPeso.MEDIO -> "Médio"; TipoPeso.BAIXO -> "Baixo" }
val FaseAssunto.rotulo get() = when (this) { FaseAssunto.BACKLOG -> "Backlog"; FaseAssunto.FRENTE -> "Frente"; FaseAssunto.CONSOLIDADO -> "Consolidado" }
val FormatoBanca.rotulo get() = when (this) { FormatoBanca.MULTIPLA_ESCOLHA -> "Múltipla escolha"; FormatoBanca.CERTO_ERRADO -> "Certo/errado" }
val TipoSessao.rotulo get() = when (this) {
    TipoSessao.ESTUDO -> "Conteúdo"; TipoSessao.QUESTOES -> "Questões"
    TipoSessao.FLASHCARDS -> "Flashcards"; TipoSessao.RECUPERACAO -> "Recuperação"
}
val CausaErro.rotulo get() = when (this) {
    CausaErro.FALTA_CONHECIMENTO -> "Falta de conhecimento"; CausaErro.ESQUECIMENTO -> "Esquecimento"
    CausaErro.INTERPRETACAO -> "Interpretação"; CausaErro.DESATENCAO -> "Desatenção"
    CausaErro.PEGADINHA -> "Pegadinha"; CausaErro.CHUTE -> "Chute"; CausaErro.GESTAO_TEMPO -> "Gestão de tempo"
}
val NivelConfianca.rotulo get() = when (this) { NivelConfianca.BAIXA -> "Baixa"; NivelConfianca.MEDIA -> "Média"; NivelConfianca.ALTA -> "Alta" }

/** Previsão da etapa 1 de Recuperar (02_JORNADAS §4.1): a mesma régua, no futuro. */
val ResultadoSessao.previsao get() = when (this) {
    ResultadoSessao.SUCESSO -> "Vou reconstruir"; ResultadoSessao.PARCIAL -> "Vai faltar um pedaço"; ResultadoSessao.FALHA -> "Não vai vir"
}
/** Resultado da etapa 2, com a âncora de estrutura × detalhe (01_DOMINIO §4.1). */
val ResultadoSessao.resultado get() = when (this) {
    ResultadoSessao.SUCESSO -> "Reconstruí"; ResultadoSessao.PARCIAL -> "Faltou um pedaço"; ResultadoSessao.FALHA -> "Não veio"
}
val ResultadoSessao.ancora get() = when (this) {
    ResultadoSessao.SUCESSO -> "os pontos principais vieram; faltou detalhe, não estrutura"
    ResultadoSessao.PARCIAL -> "a estrutura veio, mas um bloco inteiro não"
    ResultadoSessao.FALHA -> "preciso reler antes de tentar de novo"
}

/** Rótulos de Ajustes, os mesmos de ROTULOS_PARAMETRO da SPA; chave nova cai na própria chave. */
private val ROTULOS_PARAMETRO = mapOf(
    "teto_diario_recuperacoes" to "Recuperações por dia",
    "teto_global_frente" to "Assuntos na frente (global)",
    "teto_disciplina_frente" to "Assuntos na frente (por disciplina)",
    "limiar_sucesso_multipla_escolha" to "Sucesso · múltipla escolha",
    "limiar_parcial_multipla_escolha" to "Parcial · múltipla escolha",
    "limiar_sucesso_certo_errado" to "Sucesso · certo/errado",
    "limiar_parcial_certo_errado" to "Parcial · certo/errado",
    "limiar_sucesso_flashcards" to "Sucesso · flashcards",
    "limiar_parcial_flashcards" to "Parcial · flashcards",
    "intervalo_nivel_1" to "Intervalo nível 1",
    "intervalo_nivel_2" to "Intervalo nível 2",
    "intervalo_nivel_3" to "Intervalo nível 3",
    "intervalo_nivel_4" to "Intervalo nível 4",
    "intervalo_nivel_5" to "Intervalo nível 5",
    "intervalo_nivel_6" to "Intervalo nível 6",
    "intervalo_manutencao_dias" to "Manutenção (consolidado)",
    "janela_tolerancia_percentual" to "Janela de tolerância",
    "lote_minimo_questoes" to "Lote mínimo de questões",
    "duracao_bloco_min" to "Duração sugerida do bloco (min)",
    "parciais_seguidos_dividir" to "Parciais seguidos para sugerir divisão",
)

fun rotuloParametro(chave: String) = ROTULOS_PARAMETRO[chave] ?: chave

/** Grupos de Ajustes por prefixo, na mesma ordem da SPA (04_FRONTEND §7E). */
fun grupoParametro(chave: String) = when {
    chave.startsWith("teto_") -> "Tetos"
    chave.startsWith("limiar_") -> "Limiares de resultado"
    chave.startsWith("duracao_") -> "Turno"
    else -> "Escada"
}
val ORDEM_GRUPOS = listOf("Tetos", "Turno", "Limiares de resultado", "Escada")

/** Linha do concurso-alvo em Hoje (02_JORNADAS §4.4): contexto, não alarme. Os dias vêm do servidor. */
fun textoProva(diasParaProva: Long?): String = when {
    diasParaProva == null -> "sem data de prova"
    diasParaProva == 0L -> "prova hoje"
    diasParaProva < 0 -> "a prova já passou"
    diasParaProva == 1L -> "prova amanhã"
    else -> "prova em $diasParaProva dias"
}

/** "2026-10-15T14:09:21.58Z" → "15/10/2026" (data do instante, em UTC; basta para o histórico). */
fun dataDoInstante(instante: String?): String = instante?.take(10)?.let(::dataBr) ?: ""

val br.com.estudos.app.data.api.StatusAnalise.rotulo get() = when (this) {
    br.com.estudos.app.data.api.StatusAnalise.LENDO_EDITAL -> "Lendo o edital…"
    br.com.estudos.app.data.api.StatusAnalise.ESCOLHER_CARGO -> "Escolha o cargo"
    br.com.estudos.app.data.api.StatusAnalise.SEPARANDO_ASSUNTOS -> "Separando os assuntos…"
    br.com.estudos.app.data.api.StatusAnalise.PRONTA -> "Pronta para revisar"
    br.com.estudos.app.data.api.StatusAnalise.CONFIRMADA -> "Confirmada"
    br.com.estudos.app.data.api.StatusAnalise.FALHOU -> "Falhou — toque para tentar de novo"
}
