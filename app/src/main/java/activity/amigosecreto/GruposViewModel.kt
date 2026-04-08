package activity.amigosecreto

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.repository.BackupRepository
import activity.amigosecreto.repository.GruposRepository
import activity.amigosecreto.repository.ParticipanteRepository
import activity.amigosecreto.util.BackupManager
import androidx.test.espresso.idling.CountingIdlingResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GruposViewModel @Inject constructor(
    application: Application,
    private var gruposRepository: GruposRepository,
    private var participanteRepository: ParticipanteRepository,
    private var backupRepository: BackupRepository,
) : AndroidViewModel(application) {

    companion object {
        const val SORT_CRIACAO = 0
        const val SORT_NOME = 1
        const val SORT_EVENTO = 2

        /** IdlingResource for Espresso tests — tracks in-flight ViewModel coroutines. */
        val idlingResource = CountingIdlingResource("GruposViewModel")
    }

    /**
     * Representa um grupo com suas contagens de participantes e enviados.
     * Elimina o double-notify atual (atualizarLista + recarregarContagensAsync).
     */
    data class GrupoComContagem(
        val grupo: Grupo,
        val totalParticipantes: Int,
        val totalEnviados: Int,
    )

    sealed class ImportarResultado {
        data class Sucesso(val gruposImportados: Int) : ImportarResultado()
        object Falha : ImportarResultado()
    }

    @VisibleForTesting
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _grupos = MutableLiveData<List<GrupoComContagem>>(emptyList())
    private val _isLoading = MutableLiveData(false)
    private val _errorMessage = MutableLiveData<String?>()
    private val _operacaoSucesso = MutableLiveData<Boolean?>()
    private val _exportarResultado = MutableLiveData<String?>()
    private val _importarResultado = MutableLiveData<ImportarResultado?>()

    val grupos: LiveData<List<GrupoComContagem>> get() = _grupos
    val isLoading: LiveData<Boolean> get() = _isLoading
    val errorMessage: LiveData<String?> get() = _errorMessage
    val operacaoSucesso: LiveData<Boolean?> get() = _operacaoSucesso
    val exportarResultado: LiveData<String?> get() = _exportarResultado
    val importarResultado: LiveData<ImportarResultado?> get() = _importarResultado

    fun clearErrorMessage() { _errorMessage.value = null }
    fun clearOperacaoSucesso() { _operacaoSucesso.value = null }
    fun clearExportarResultado() { _exportarResultado.value = null }
    fun clearImportarResultado() { _importarResultado.value = null }

    private var sortOrder: Int = SORT_CRIACAO

    private fun launchTracked(block: suspend () -> Unit) {
        idlingResource.increment()
        viewModelScope.launch {
            try { block() } finally { idlingResource.decrement() }
        }
    }

    fun carregarGrupos(sortOrder: Int = this.sortOrder) {
        this.sortOrder = sortOrder
        _isLoading.value = true
        launchTracked {
            try {
                val result = withContext(ioDispatcher) {
                    val grupos = gruposRepository.listar()
                    val contagemTotal = participanteRepository.contarPorGrupo()
                    val contagemEnviados = participanteRepository.contarEnviadosPorGrupo()
                    val ordenados = aplicarOrdenacao(grupos, sortOrder)
                    ordenados.map { g ->
                        GrupoComContagem(
                            grupo = g,
                            totalParticipantes = contagemTotal[g.id] ?: 0,
                            totalEnviados = contagemEnviados[g.id] ?: 0,
                        )
                    }
                }
                _grupos.value = result
            } catch (e: Exception) {
                Timber.e(e, "carregarGrupos failed")
                _errorMessage.value = getApplication<Application>().getString(R.string.error_load_groups)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun inserirGrupo(nome: String, data: String) {
        launchTracked {
            try {
                withContext(ioDispatcher) {
                    val grupo = Grupo(nome = nome, data = data)
                    gruposRepository.inserir(grupo)
                }
                _operacaoSucesso.value = true
                carregarGrupos()
            } catch (e: Exception) {
                Timber.e(e, "inserirGrupo failed")
                _errorMessage.value = getApplication<Application>().getString(R.string.grupo_erro_salvar)
                _operacaoSucesso.value = false
            }
        }
    }

    fun atualizarNomeGrupo(grupo: Grupo) {
        launchTracked {
            val salvo = try {
                withContext(ioDispatcher) { gruposRepository.atualizar(grupo) }
            } catch (e: Exception) {
                Timber.e(e, "atualizarNomeGrupo failed for id=${grupo.id}")
                false
            }
            _operacaoSucesso.value = salvo
            if (!salvo) {
                _errorMessage.value = getApplication<Application>().getString(R.string.grupo_erro_salvar)
            } else {
                carregarGrupos()
            }
        }
    }

    fun removerGrupo(grupo: Grupo) {
        launchTracked {
            try {
                withContext(ioDispatcher) { gruposRepository.remover(grupo) }
                carregarGrupos()
            } catch (e: Exception) {
                Timber.e(e, "removerGrupo failed for id=${grupo.id}")
                _errorMessage.value = getApplication<Application>().getString(R.string.error_load_groups)
            }
        }
    }

    fun limparTudo() {
        launchTracked {
            try {
                withContext(ioDispatcher) { gruposRepository.limparTudo() }
                carregarGrupos()
            } catch (e: Exception) {
                Timber.e(e, "limparTudo failed")
                _errorMessage.value = getApplication<Application>().getString(R.string.error_load_groups)
            }
        }
    }

    fun exportarBackup() {
        launchTracked {
            val json = try {
                withContext(ioDispatcher) { backupRepository.exportar() }
            } catch (e: Exception) {
                Timber.e(e, "exportarBackup failed")
                null
            }
            _exportarResultado.value = json
        }
    }

    fun importarBackup(json: String) {
        launchTracked {
            val resultado = try {
                withContext(ioDispatcher) { backupRepository.importar(json) }
            } catch (e: Exception) {
                Timber.e(e, "importarBackup failed")
                BackupManager.ImportResult.Failure("exception: ${e.message}")
            }
            when (resultado) {
                is BackupManager.ImportResult.Success -> {
                    carregarGrupos()
                    _importarResultado.value = ImportarResultado.Sucesso(resultado.gruposImportados)
                }
                is BackupManager.ImportResult.Failure -> {
                    Timber.e("importarBackup: ${resultado.reason}")
                    _importarResultado.value = ImportarResultado.Falha
                }
            }
        }
    }

    @VisibleForTesting
    fun setRepositories(
        gruposRepository: GruposRepository,
        participanteRepository: ParticipanteRepository,
        backupRepository: BackupRepository = this.backupRepository,
    ) {
        this.gruposRepository = gruposRepository
        this.participanteRepository = participanteRepository
        this.backupRepository = backupRepository
    }

    private fun aplicarOrdenacao(grupos: List<Grupo>, ordem: Int): List<Grupo> {
        return when (ordem) {
            SORT_NOME -> grupos.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nome ?: "" })
            SORT_EVENTO -> {
                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                grupos.sortedWith(compareBy { g ->
                    g.dataEvento?.let { raw ->
                        try { fmt.parse(raw) } catch (e: ParseException) { null }
                    }
                })
            }
            else -> grupos // SORT_CRIACAO: DAO já retorna por id DESC
        }
    }
}
