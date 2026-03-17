package activity.amigosecreto

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.repository.SorteioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HistoricoSorteiosViewModel @Inject constructor(
    private val sorteioRepository: SorteioRepository
) : ViewModel() {

    private val _sorteios = MutableLiveData<List<Sorteio>>()
    val sorteios: LiveData<List<Sorteio>> = _sorteios

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _hasError = MutableLiveData<Boolean>()
    val hasError: LiveData<Boolean> = _hasError

    fun carregarHistorico(grupoId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    sorteioRepository.listarPorGrupo(grupoId)
                }
                _sorteios.value = result
            } catch (e: Exception) {
                android.util.Log.e("HistoricoSorteiosVM", "Erro ao carregar histórico", e)
                _hasError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
}
