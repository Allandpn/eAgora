package br.com.estudos.app.data

import br.com.estudos.app.data.api.FaseAssunto
import br.com.estudos.app.data.api.TipoPeso

/**
 * Handoff entre telas (especificação §2): a API não tem GET /api/assuntos/{id},
 * então o item escolhido vai em memória e a rota leva só o id — o mesmo papel
 * dos stores Alpine da SPA. Rota aberta sem o item mostra "sem dados".
 */
class Selecao {
    @Volatile var recuperacao: ItemRecuperacao? = null
    @Volatile var registro: ItemRegistro? = null
    @Volatile var assunto: ItemAssunto? = null
    @Volatile var erro: ItemErro? = null
}

data class ItemRecuperacao(val assuntoId: Long, val nome: String, val nivel: Int, val dataPrevista: String?)
data class ItemRegistro(val assuntoId: Long, val nome: String, val segmentoSugeridoId: Long? = null)
data class ItemAssunto(val assuntoId: Long, val nome: String, val disciplina: String, val peso: TipoPeso, val fase: FaseAssunto)
data class ItemErro(val assuntoId: Long, val nome: String)
