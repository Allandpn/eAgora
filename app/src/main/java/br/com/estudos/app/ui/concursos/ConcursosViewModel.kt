package br.com.estudos.app.ui.concursos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.estudos.app.data.ProvedorApi
import br.com.estudos.app.data.api.AnaliseEditalDto
import br.com.estudos.app.data.api.ConcursoDto
import br.com.estudos.app.data.api.ErroApi
import br.com.estudos.app.data.api.IaStatusDto
import br.com.estudos.app.data.api.StatusAnalise
import br.com.estudos.app.data.api.chamar
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConcursosUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val erroAcao: String? = null,
    val concursos: List<ConcursoDto> = emptyList(),
    val ia: IaStatusDto? = null,
    /** Análises que ainda pedem algo do usuário (02_JORNADAS J-5). */
    val analises: List<AnaliseEditalDto> = emptyList(),
    val reativando: Long? = null,
) {
    val ativo get() = concursos.firstOrNull { it.ativo }
    val anteriores get() = concursos.filterNot { it.ativo }
}

/** Concursos (02_JORNADAS J-5): o alvo, o histórico e a entrada de edital novo. */
class ConcursosViewModel(private val provedor: ProvedorApi) : ViewModel() {
    private val _estado = MutableStateFlow(ConcursosUiState())
    val estado: StateFlow<ConcursosUiState> = _estado

    fun carregar() {
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }
            try {
                val api = provedor.api()
                coroutineScope {
                    val concursos = async { chamar { api.concursos() } }
                    val ia = async { chamar { api.iaStatus() } }
                    val analises = async { chamar { api.analises() } }
                    _estado.update {
                        it.copy(
                            carregando = false,
                            concursos = concursos.await(),
                            ia = ia.await(),
                            analises = analises.await().filter { a -> a.status != StatusAnalise.CONFIRMADA },
                        )
                    }
                }
            } catch (e: ErroApi) {
                _estado.update { it.copy(carregando = false, erro = e.message) }
            }
        }
    }

    /** D-60: muda peso e ordem — a tela pede confirmação antes. */
    fun reativar(id: Long) {
        viewModelScope.launch {
            _estado.update { it.copy(reativando = id, erroAcao = null) }
            try {
                chamar { provedor.api().ativarConcurso(id) }
                _estado.update { it.copy(reativando = null) }
                carregar()
            } catch (e: ErroApi) {
                _estado.update { it.copy(reativando = null, erroAcao = e.message) }
            }
        }
    }
}
