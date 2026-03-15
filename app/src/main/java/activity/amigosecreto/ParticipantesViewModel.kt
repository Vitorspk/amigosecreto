package activity.amigosecreto

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.repository.DesejoRepository
import activity.amigosecreto.repository.ParticipanteRepository
import activity.amigosecreto.util.MensagemSecretaBuilder
import activity.amigosecreto.util.SorteioEngine

@HiltViewModel
class ParticipantesViewModel @Inject constructor(
    application: Application,
    private var participanteRepository: ParticipanteRepository,
    private var desejoRepository: DesejoRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ParticipantesViewModel"
    }

    /** Resultado do sorteio — substitui sealed class (mantido compatível com Java). */
    class SorteioResultado(@JvmField val status: Status) {
        enum class Status { SUCCESS, FAILURE_NOT_ENOUGH, FAILURE_IMPOSSIBLE }
    }

    /** Resultado de prepararMensagensSms — lista de participantes com telefone + mapa de mensagens. */
    class MensagensSmsResultado(
        @JvmField val participantesComTelefone: List<Participante>,
        @JvmField val mensagens: Map<Int, String>
    )

    /** Resultado de prepararMensagemCompartilhamento — mensagem formatada para um participante. */
    class MensagemCompartilhamentoResultado(
        @JvmField val participante: Participante,
        @JvmField val mensagem: String
    )

    private val _participants = MutableLiveData<List<Participante>>(emptyList())
    private val _wishCounts = MutableLiveData<Map<Int, Int>>(emptyMap())
    private val _isLoading = MutableLiveData(false)
    private val _sorteioResult = MutableLiveData<SorteioResultado?>(null)
    private val _errorMessage = MutableLiveData<String?>(null)
    private val _mensagensSmsResult = MutableLiveData<MensagensSmsResultado?>(null)
    private val _mensagemCompartilhamentoResult = MutableLiveData<MensagemCompartilhamentoResultado?>(null)
    private val _atualizarSucesso = MutableLiveData<Boolean?>(null)

    // Kotlin properties — Kotlin compiler auto-generates get*() accessors for Java interop.
    // ParticipantesActivity.java and ParticipantesViewModelTest.java call these via getXxx().
    val participants: LiveData<List<Participante>> get() = _participants
    val wishCounts: LiveData<Map<Int, Int>> get() = _wishCounts
    // @JvmName forces the getter to be named getIsLoading() instead of isLoading()
    // so that ParticipantesActivity.java and ParticipantesViewModelTest.java can call it.
    @get:JvmName("getIsLoading")
    val isLoading: LiveData<Boolean> get() = _isLoading
    val sorteioResult: LiveData<SorteioResultado?> get() = _sorteioResult
    val errorMessage: LiveData<String?> get() = _errorMessage
    val mensagensSmsResult: LiveData<MensagensSmsResultado?> get() = _mensagensSmsResult
    val mensagemCompartilhamentoResult: LiveData<MensagemCompartilhamentoResultado?> get() = _mensagemCompartilhamentoResult
    val atualizarSucesso: LiveData<Boolean?> get() = _atualizarSucesso

    private val mainHandler = Handler(Looper.getMainLooper())
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var grupoId = -1

    /**
     * Deve ser chamado uma vez em onCreate() após obter o ViewModel.
     * A guarda por grupoId evita recarregar após rotação de tela.
     */
    fun init(grupoId: Int) {
        if (this.grupoId == grupoId) return
        this.grupoId = grupoId
        carregarParticipantes()
    }

    fun clearAtualizarSucesso() { _atualizarSucesso.value = null }
    fun clearSorteioResult() { _sorteioResult.value = null }
    fun clearErrorMessage() { _errorMessage.value = null }
    fun clearMensagensSmsResult() { _mensagensSmsResult.value = null }
    fun clearMensagemCompartilhamentoResult() { _mensagemCompartilhamentoResult.value = null }

    /** Marca participante como enviado em background (evita ANR). */
    fun marcarComoEnviado(participanteId: Int) {
        executor.execute {
            try {
                participanteRepository.marcarComoEnviado(participanteId)
            } catch (e: Exception) {
                handleDbError(e, "Erro ao marcar como enviado id=$participanteId", R.string.error_save_failed)
            }
        }
    }

    /** Remove participante em background (evita ANR). */
    fun removerParticipante(participanteId: Int) {
        executor.execute {
            try {
                participanteRepository.remover(participanteId)
                postMain(::carregarParticipantes)
            } catch (e: Exception) {
                handleDbError(e, "Erro ao remover participante id=$participanteId", R.string.error_save_failed)
            }
        }
    }

    /**
     * Atualiza participante em background (evita ANR).
     * Posta `true` em [atualizarSucesso] se bem-sucedido, `false` caso contrário.
     *
     * Canal de erro diferente dos demais métodos: usa `atualizarSucesso=false`
     * em vez de `errorMessage`, porque a Activity precisa saber se deve fechar o dialog de
     * edição ou manter os campos preenchidos para o usuário corrigir.
     */
    fun atualizarParticipante(participante: Participante) {
        executor.execute {
            val sucesso = try {
                participanteRepository.atualizar(participante)
            } catch (e: Exception) {
                false
            }
            postMain {
                _atualizarSucesso.value = sucesso
                if (sucesso) carregarParticipantes()
            }
        }
    }

    /** Insere participante em background (evita ANR). */
    fun inserirParticipante(participante: Participante, grupoId: Int) {
        executor.execute {
            try {
                participanteRepository.inserir(participante, grupoId)
                postMain(::carregarParticipantes)
            } catch (e: Exception) {
                handleDbError(e, "Erro ao inserir participante grupoId=$grupoId", R.string.error_save_failed)
            }
        }
    }

    /** Deleta todos os participantes de um grupo em background (evita ANR). */
    fun deletarTodosDoGrupo(grupoId: Int) {
        executor.execute {
            try {
                participanteRepository.deletarTodosDoGrupo(grupoId)
                postMain(::carregarParticipantes)
            } catch (e: Exception) {
                handleDbError(e, "Erro ao deletar todos do grupo id=$grupoId", R.string.error_save_failed)
            }
        }
    }

    /** Salva exclusões de um participante em background em transação atômica (evita ANR e falha parcial). */
    fun salvarExclusoes(participanteId: Int, adicionar: List<Int>, remover: List<Int>) {
        executor.execute {
            try {
                participanteRepository.salvarExclusoes(participanteId, adicionar, remover)
                postMain(::carregarParticipantes)
            } catch (e: Exception) {
                handleDbError(e, "Erro ao salvar exclusões participanteId=$participanteId", R.string.error_save_failed)
            }
        }
    }

    /**
     * Carrega participantes e contagens de desejos do banco em background.
     * Pode ser chamado a qualquer momento para forçar atualização (ex: após voltar de outra tela).
     */
    fun carregarParticipantes() {
        if (grupoId == -1) return
        _isLoading.value = true
        executor.execute {
            try {
                val lista = participanteRepository.listarPorGrupo(grupoId)
                val counts = desejoRepository.contarDesejosPorGrupo(grupoId)
                postMain {
                    _participants.value = lista
                    _wishCounts.value = counts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                postMain {
                    _isLoading.value = false
                    _errorMessage.value = getApplication<Application>().getString(R.string.error_load_participants)
                }
            }
        }
    }

    /**
     * Executa o sorteio em background.
     * Posta o resultado em sorteioResult; a Activity observa e exibe o dialog/toast adequado.
     */
    fun realizarSorteio() {
        val snapshot = _participants.value
        if (snapshot == null || snapshot.size < 3) {
            _sorteioResult.value = SorteioResultado(SorteioResultado.Status.FAILURE_NOT_ENOUGH)
            return
        }

        _isLoading.value = true
        val sortableSnapshot = ArrayList(snapshot)

        executor.execute {
            var sorteados: List<Participante>? = null
            var tentativas = 0
            while (sorteados == null && tentativas < 100) {
                tentativas++
                sorteados = SorteioEngine.tentarSorteio(ArrayList(sortableSnapshot))
            }

            if (sorteados == null) {
                postMain {
                    _isLoading.value = false
                    _sorteioResult.value = SorteioResultado(SorteioResultado.Status.FAILURE_IMPOSSIBLE)
                }
                return@execute
            }

            val salvo = try {
                participanteRepository.salvarSorteio(sortableSnapshot, sorteados)
            } catch (e: Exception) {
                false
            }

            postMain {
                _isLoading.value = false
                if (salvo) {
                    carregarParticipantes()
                    _sorteioResult.value = SorteioResultado(SorteioResultado.Status.SUCCESS)
                } else {
                    _errorMessage.value = getApplication<Application>().getString(R.string.error_save_draw)
                }
            }
        }
    }

    /**
     * Prepara as mensagens SMS para todos os participantes com telefone.
     * Acessa o banco em background; posta o resultado em mensagensSmsResult.
     * Aceita uma lista de participantes para suportar tanto o fluxo normal (snapshot da lista)
     * quanto a reconstrução após rotação (lista restaurada do bundle).
     */
    fun prepararMensagensSms(snapshot: List<Participante>) {
        executor.execute {
            try {
                // Nomes frescos do banco — evita dados desatualizados se alguém editou um
                // participante em outra tela após o snapshot ter sido capturado.
                val participantesAtuais = participanteRepository.listarPorGrupo(grupoId)
                val nomeMap = participantesAtuais.associate { it.id to it.nome }

                // Uma única query traz todos os desejos do grupo de uma vez.
                val desejosMap = desejoRepository.listarDesejosPorGrupo(grupoId)

                val comTelefone = mutableListOf<Participante>()
                val mensagens = mutableMapOf<Int, String>()
                for (p in snapshot) {
                    val tel = p.telefone
                    if (!tel.isNullOrBlank()) {
                        comTelefone.add(p)
                        val amigoId = p.amigoSorteadoId
                        val nomeAmigo = if (amigoId != null && amigoId > 0) nomeMap[amigoId] else null
                        val desejos: List<Desejo> = if (amigoId != null && amigoId > 0) desejosMap[amigoId] ?: emptyList() else emptyList()
                        mensagens[p.id] = MensagemSecretaBuilder.gerar(p.nome, nomeAmigo, desejos)
                    }
                }
                val resultado = MensagensSmsResultado(comTelefone, mensagens)
                postMain { _mensagensSmsResult.value = resultado }
            } catch (e: Exception) {
                postMain { _errorMessage.value = getApplication<Application>().getString(R.string.error_prepare_messages_failed) }
            }
        }
    }

    /**
     * Prepara a mensagem de compartilhamento (WhatsApp/share sheet) para um participante.
     * Acessa o banco em background, marca como enviado e posta o resultado em
     * mensagemCompartilhamentoResult.
     *
     * Limitação conhecida: marcarComoEnviado é chamado antes do usuário confirmar
     * o share sheet (a API do ACTION_SEND não oferece callback de confirmação).
     */
    fun prepararMensagemCompartilhamento(participante: Participante) {
        executor.execute {
            try {
                val amigoId = participante.amigoSorteadoId
                val nomeAmigo = if (amigoId != null && amigoId > 0) participanteRepository.getNomeAmigoSorteado(amigoId) else null
                val desejos: List<Desejo> = if (amigoId != null && amigoId > 0) desejoRepository.listarPorParticipante(amigoId) else emptyList()
                participanteRepository.marcarComoEnviado(participante.id)
                val mensagem = MensagemSecretaBuilder.gerar(participante.nome, nomeAmigo, desejos)
                postMain { _mensagemCompartilhamentoResult.value = MensagemCompartilhamentoResultado(participante, mensagem) }
            } catch (e: Exception) {
                postMain { _errorMessage.value = getApplication<Application>().getString(R.string.error_load_share_data) }
            }
        }
    }

    private fun postMain(r: () -> Unit) {
        mainHandler.post(r)
    }

    /**
     * Trata exceções de operações de banco:
     * - Loga sempre via Log.e (stack trace visível no adb logcat em debug e release)
     * - Posta a mensagem de erro informada para o main thread em todos os casos
     *
     * Não relança a exceção: relançar de dentro de um Runnable submetido ao executor
     * vai para o UncaughtExceptionHandler da thread de background — o processo
     * não derruba de forma imediata e o postMain com a mensagem de erro nunca roda,
     * deixando o usuário sem feedback.
     */
    private fun handleDbError(e: Exception, logMsg: String, errorStringRes: Int) {
        Log.e(TAG, logMsg, e)
        postMain { _errorMessage.value = getApplication<Application>().getString(errorStringRes) }
    }

    @VisibleForTesting
    fun setExecutorService(executor: ExecutorService) {
        if (!this.executor.isShutdown) this.executor.shutdown()
        this.executor = executor
    }

    @VisibleForTesting
    fun setRepositories(participanteRepository: ParticipanteRepository, desejoRepository: DesejoRepository) {
        this.participanteRepository = participanteRepository
        this.desejoRepository = desejoRepository
    }

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }
}
