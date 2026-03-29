package activity.amigosecreto

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.repository.SorteioRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class HistoricoSorteiosViewModel @Inject constructor(
    private val sorteioRepository: SorteioRepository
) : ViewModel() {

    // Dispatcher injetável via construtor secundário — usado nos testes para
    // substituir Dispatchers.IO por UnconfinedTestDispatcher e usar advanceUntilIdle().
    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    @VisibleForTesting
    constructor(
        sorteioRepository: SorteioRepository,
        ioDispatcher: CoroutineDispatcher
    ) : this(sorteioRepository) {
        this.ioDispatcher = ioDispatcher
    }

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
                val result = withContext(ioDispatcher) {
                    sorteioRepository.listarPorGrupo(grupoId)
                }
                _sorteios.value = result
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar histórico")
                _hasError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
}
