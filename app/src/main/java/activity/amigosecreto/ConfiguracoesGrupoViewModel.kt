package activity.amigosecreto

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.room.GrupoRoomDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

sealed class SalvarResultado {
    data class Sucesso(val grupo: Grupo) : SalvarResultado()
    data object SemLinhasAfetadas : SalvarResultado()
    data class Falha(val causa: Throwable) : SalvarResultado()
}

@HiltViewModel
class ConfiguracoesGrupoViewModel @Inject constructor(
    private val grupoDao: GrupoRoomDao,
) : ViewModel() {

    @VisibleForTesting
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _resultado = MutableLiveData<SalvarResultado?>()
    val resultado: LiveData<SalvarResultado?> = _resultado

    fun salvar(grupo: Grupo) {
        viewModelScope.launch {
            try {
                val rows = withContext(ioDispatcher) {
                    grupoDao.atualizar(grupo)
                }
                _resultado.value = if (rows > 0) {
                    SalvarResultado.Sucesso(grupo)
                } else {
                    SalvarResultado.SemLinhasAfetadas
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao salvar configurações do grupo")
                _resultado.value = SalvarResultado.Falha(e)
            }
        }
    }

    fun resultadoConsumido() {
        _resultado.value = null
    }
}
