package activity.amigosecreto

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.room.GrupoRoomDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConfiguracoesGrupoViewModel @Inject constructor(
    private val grupoDao: GrupoRoomDao,
) : ViewModel() {

    private val _salvoComSucesso = MutableLiveData<Grupo?>()
    val salvoComSucesso: LiveData<Grupo?> = _salvoComSucesso

    private val _erro = MutableLiveData<String?>()
    val erro: LiveData<String?> = _erro

    fun salvar(grupo: Grupo) {
        viewModelScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) {
                    grupoDao.atualizar(grupo)
                }
                if (rows > 0) {
                    _salvoComSucesso.value = grupo
                } else {
                    _erro.value = "rows_zero"
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao salvar configurações do grupo")
                _erro.value = e.message
            }
        }
    }

    fun erroConsumido() {
        _erro.value = null
    }

    fun salvoConsumido() {
        _salvoComSucesso.value = null
    }
}
