package activity.amigosecreto

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import activity.amigosecreto.db.room.GrupoRoomDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class EstatisticasUiState(
    val totalGrupos: Int = 0,
    val totalParticipantes: Int = 0,
    val totalSorteios: Int = 0,
    val totalDesejos: Int = 0,
    val mediaValor: Double? = null,
    val error: String? = null,
)

@HiltViewModel
class EstatisticasViewModel @Inject constructor(
    private val grupoDao: GrupoRoomDao,
) : ViewModel() {

    @VisibleForTesting
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _uiState = MutableLiveData<EstatisticasUiState>()
    val uiState: LiveData<EstatisticasUiState> = _uiState

    fun carregarEstatisticas() {
        viewModelScope.launch {
            try {
                val totalGrupos: Int
                val totalParticipantes: Int
                val totalSorteios: Int
                val totalDesejos: Int
                val mediaValor: Double?

                withContext(ioDispatcher) {
                    totalGrupos = grupoDao.contarGrupos()
                    totalParticipantes = grupoDao.contarParticipantes()
                    totalSorteios = grupoDao.contarSorteios()
                    totalDesejos = grupoDao.contarDesejos()
                    mediaValor = grupoDao.mediaValorDesejos()
                }

                _uiState.value = EstatisticasUiState(
                    totalGrupos = totalGrupos,
                    totalParticipantes = totalParticipantes,
                    totalSorteios = totalSorteios,
                    totalDesejos = totalDesejos,
                    mediaValor = mediaValor,
                )
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar estatísticas")
                _uiState.value = EstatisticasUiState(error = e.message ?: "Erro desconhecido")
            }
        }
    }
}
