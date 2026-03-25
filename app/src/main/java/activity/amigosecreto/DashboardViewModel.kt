package activity.amigosecreto

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.room.GrupoRoomDao
import activity.amigosecreto.db.room.ParticipanteRoomDao
import activity.amigosecreto.db.room.SorteioRoomDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class DashboardUiState(
    val grupo: Grupo? = null,
    val totalParticipantes: Int = 0,
    val sorteioRealizado: Boolean = false,
    val confirmados: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val grupoDao: GrupoRoomDao,
    private val participanteDao: ParticipanteRoomDao,
    private val sorteioDao: SorteioRoomDao,
) : ViewModel() {

    @VisibleForTesting
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _uiState = MutableLiveData<DashboardUiState>()
    val uiState: LiveData<DashboardUiState> = _uiState

    fun carregarDados(grupoId: Int) {
        viewModelScope.launch {
            try {
                val state = withContext(ioDispatcher) {
                    val grupo = async { grupoDao.buscarPorId(grupoId) }
                    val totalParticipantes = async { participanteDao.contarPorGrupo(grupoId) }
                    val sorteioRealizado = async { sorteioDao.contarPorGrupo(grupoId) > 0 }
                    val confirmados = async { participanteDao.contarConfirmados(grupoId) }
                    DashboardUiState(
                        grupo = grupo.await(),
                        totalParticipantes = totalParticipantes.await(),
                        sorteioRealizado = sorteioRealizado.await(),
                        confirmados = confirmados.await(),
                    )
                }
                _uiState.value = state
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar dados do dashboard")
                _uiState.value = DashboardUiState(error = e.message ?: "Erro desconhecido")
            }
        }
    }
}
