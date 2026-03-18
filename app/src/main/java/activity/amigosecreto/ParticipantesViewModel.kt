package activity.amigosecreto

import android.app.Application
import timber.log.Timber
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.test.espresso.idling.CountingIdlingResource
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.repository.DesejoRepository
import activity.amigosecreto.repository.ParticipanteRepository
import activity.amigosecreto.repository.SorteioRepository
import activity.amigosecreto.util.MensagemSecretaBuilder
import activity.amigosecreto.util.SorteioEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ParticipantesViewModel @Inject constructor(
    application: Application,
    // var (not val) — required by @VisibleForTesting setRepositories() for fake injection in tests.
    // TODO: convert to val + remove setRepositories() when tests migrate to constructor injection or @TestInstallIn.
    private var participanteRepository: ParticipanteRepository,
    private var desejoRepository: DesejoRepository,
    private var sorteioRepository: SorteioRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ParticipantesViewModel"
        /**
         * CountingIdlingResource for Espresso tests to synchronize with coroutine-based DB operations.
         * Register via IdlingRegistry.getInstance().register(ParticipantesViewModel.idlingResource).
         */
        val idlingResource = CountingIdlingResource("ParticipantesViewModel")
    }

    /**
     * Wraps viewModelScope.launch to keep [idlingResource] in sync with coroutine lifecycle.
     * Espresso waits until counter reaches 0 before performing assertions.
     */
    private fun launchTracked(block: suspend () -> Unit) {
        idlingResource.increment()
        viewModelScope.launch {
            try {
                block()
            } finally {
                idlingResource.decrement()
            }
        }
    }

    /** Resultado do sorteio. */
    class SorteioResultado(val status: Status) {
        enum class Status { SUCCESS, FAILURE_NOT_ENOUGH, FAILURE_IMPOSSIBLE }
    }

    /** Resultado de prepararMensagensSms — lista de participantes com telefone + mapa de mensagens. */
    class MensagensSmsResultado(
        val participantesComTelefone: List<Participante>,
        val mensagens: Map<Int, String>
    )

    /** Resultado de prepararMensagemCompartilhamento — mensagem formatada para um participante. */
    class MensagemCompartilhamentoResultado(
        val participante: Participante,
        val mensagem: String
    )

    /** Resultado de obterNomeAmigoParaQr — participante + nome do amigo para exibir o QR Code. */
    class QrCodeResultado(
        val nomeParticipante: String,
        val nomeAmigo: String
    )

    private val _participants = MutableLiveData<List<Participante>>(emptyList())
    private val _wishCounts = MutableLiveData<Map<Int, Int>>(emptyMap())
    private val _isLoading = MutableLiveData(false)
    private val _sorteioResult = MutableLiveData<SorteioResultado?>(null)
    private val _errorMessage = MutableLiveData<String?>(null)
    private val _mensagensSmsResult = MutableLiveData<MensagensSmsResultado?>(null)
    private val _mensagemCompartilhamentoResult = MutableLiveData<MensagemCompartilhamentoResultado?>(null)
    private val _atualizarSucesso = MutableLiveData<Boolean?>(null)
    private val _qrCodeResult = MutableLiveData<QrCodeResultado?>(null)

    val participants: LiveData<List<Participante>> get() = _participants
    val wishCounts: LiveData<Map<Int, Int>> get() = _wishCounts
    val isLoading: LiveData<Boolean> get() = _isLoading
    val sorteioResult: LiveData<SorteioResultado?> get() = _sorteioResult
    val errorMessage: LiveData<String?> get() = _errorMessage
    val mensagensSmsResult: LiveData<MensagensSmsResultado?> get() = _mensagensSmsResult
    val mensagemCompartilhamentoResult: LiveData<MensagemCompartilhamentoResultado?> get() = _mensagemCompartilhamentoResult
    val atualizarSucesso: LiveData<Boolean?> get() = _atualizarSucesso
    val qrCodeResult: LiveData<QrCodeResultado?> get() = _qrCodeResult

    // Overridable in tests via setIoDispatcher() to use UnconfinedTestDispatcher.
    @VisibleForTesting
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    // @Volatile: written on main thread (init()), read inside withContext(ioDispatcher) on IO thread.
    // TODO: convert to val via constructor injection when setRepositories() is removed.
    @Volatile private var grupoId = -1

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
    fun clearQrCodeResult() { _qrCodeResult.value = null }
    fun clearSorteioResult() { _sorteioResult.value = null }
    fun clearErrorMessage() { _errorMessage.value = null }
    fun clearMensagensSmsResult() { _mensagensSmsResult.value = null }
    fun clearMensagemCompartilhamentoResult() { _mensagemCompartilhamentoResult.value = null }

    /** Marca participante como enviado em background (evita ANR). */
    fun marcarComoEnviado(participanteId: Int) {
        launchTracked {
            try {
                withContext(ioDispatcher) { participanteRepository.marcarComoEnviado(participanteId) }
            } catch (e: Exception) {
                handleDbError(e, "Erro ao marcar como enviado id=$participanteId", R.string.error_save_failed)
            }
        }
    }

    /** Remove participante em background (evita ANR). */
    fun removerParticipante(participanteId: Int) {
        launchTracked {
            try {
                withContext(ioDispatcher) { participanteRepository.remover(participanteId) }
                carregarParticipantes()
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
        launchTracked {
            val sucesso = try {
                withContext(ioDispatcher) { participanteRepository.atualizar(participante) }
            } catch (e: Exception) {
                Timber.e(e, "atualizarParticipante: failed for id=${participante.id}")
                false
            }
            _atualizarSucesso.value = sucesso
            if (sucesso) carregarParticipantes()
        }
    }

    /** Insere participante em background (evita ANR). */
    fun inserirParticipante(participante: Participante, grupoId: Int) {
        launchTracked {
            try {
                withContext(ioDispatcher) { participanteRepository.inserir(participante, grupoId) }
                carregarParticipantes()
            } catch (e: Exception) {
                handleDbError(e, "Erro ao inserir participante grupoId=$grupoId", R.string.error_save_failed)
            }
        }
    }

    /** Deleta todos os participantes de um grupo em background (evita ANR). */
    fun deletarTodosDoGrupo(grupoId: Int) {
        launchTracked {
            try {
                withContext(ioDispatcher) { participanteRepository.deletarTodosDoGrupo(grupoId) }
                carregarParticipantes()
            } catch (e: Exception) {
                handleDbError(e, "Erro ao deletar todos do grupo id=$grupoId", R.string.error_save_failed)
            }
        }
    }

    /** Salva exclusões de um participante em background em transação atômica (evita ANR e falha parcial). */
    fun salvarExclusoes(participanteId: Int, adicionar: List<Int>, remover: List<Int>) {
        launchTracked {
            try {
                withContext(ioDispatcher) { participanteRepository.salvarExclusoes(participanteId, adicionar, remover) }
                carregarParticipantes()
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
        launchTracked {
            try {
                val (lista, counts) = withContext(ioDispatcher) {
                    participanteRepository.listarPorGrupo(grupoId) to desejoRepository.contarDesejosPorGrupo(grupoId)
                }
                _participants.value = lista
                _wishCounts.value = counts
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = getApplication<Application>().getString(R.string.error_load_participants)
            }
        }
    }

    /**
     * Executa o sorteio em background e persiste o resultado no histórico.
     * Usa [SorteioRepository.salvarSorteioCompleto] em transação atômica:
     * insere o evento em `sorteio`, os pares em `sorteio_par` e atualiza
     * `participante.amigo_sorteado_id` — tudo ou nada.
     *
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

        launchTracked {
            val sorteados = withContext(ioDispatcher) {
                var result: List<Participante>? = null
                var tentativas = 0
                while (result == null && tentativas < 100) {
                    tentativas++
                    result = SorteioEngine.tentarSorteio(ArrayList(sortableSnapshot))
                }
                result
            }

            if (sorteados == null) {
                _isLoading.value = false
                _sorteioResult.value = SorteioResultado(SorteioResultado.Status.FAILURE_IMPOSSIBLE)
                return@launchTracked
            }

            // Inline handling (not handleDbError) because the result drives a tri-state block below:
            // success reloads + posts SUCCESS, failure posts error_save_draw.
            val salvo = try {
                withContext(ioDispatcher) {
                    sorteioRepository.salvarSorteioCompleto(grupoId, sortableSnapshot, sorteados) > 0
                }
            } catch (e: Exception) {
                Timber.e(e, "realizarSorteio: failed to save draw result")
                false
            }

            _isLoading.value = false
            if (salvo) {
                // TODO: carregarParticipantes() here causes a brief _isLoading flicker:
                // false → true (reload) → false. Pre-existing from Java version.
                // Future fix: update _participants directly from sorteados instead of re-querying.
                carregarParticipantes()
                _sorteioResult.value = SorteioResultado(SorteioResultado.Status.SUCCESS)
            } else {
                _errorMessage.value = getApplication<Application>().getString(R.string.error_save_draw)
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
        launchTracked {
            try {
                val (participantesAtuais, desejosMap) = withContext(ioDispatcher) {
                    val p = participanteRepository.listarPorGrupo(grupoId)
                    val d = desejoRepository.listarDesejosPorGrupo(grupoId)
                    p to d
                }
                val nomeMap = participantesAtuais.associate { it.id to it.nome }

                val comTelefone = mutableListOf<Participante>()
                val mensagens = mutableMapOf<Int, String>()
                for (p in snapshot) {
                    val tel = p.telefone
                    if (!tel.isNullOrBlank()) {
                        comTelefone.add(p)
                        val validAmigoId = p.amigoSorteadoId?.takeIf { it > 0 }
                        val nomeAmigo = validAmigoId?.let { nomeMap[it] }
                        val desejos: List<Desejo> = validAmigoId?.let { desejosMap[it] } ?: emptyList()
                        mensagens[p.id] = MensagemSecretaBuilder.gerar(p.nome, nomeAmigo, desejos)
                    }
                }
                _mensagensSmsResult.value = MensagensSmsResultado(comTelefone, mensagens)
            } catch (e: Exception) {
                handleDbError(e, "prepararMensagensSms: failed for grupoId=$grupoId", R.string.error_prepare_messages_failed)
            }
        }
    }

    /**
     * Prepara a mensagem de compartilhamento (WhatsApp/share sheet) para um participante.
     * Acessa o banco em background, marca como enviado e posta o resultado em
     * mensagemCompartilhamentoResult.
     *
     * Limitação conhecida: marcarComoEnviado é chamado antes do usuário confirmar
     * o share sheet (a API do ACTION_SEND não oferece callback de confirmação). Se o
     * usuário abrir o chooser e cancelar, o participante ficará marcado como enviado.
     *
     * Ordem: gerar() é chamado antes de marcarComoEnviado(). Se gerar() lançar exceção,
     * o participante não é marcado.
     */
    fun prepararMensagemCompartilhamento(participante: Participante) {
        launchTracked {
            try {
                val mensagem = withContext(ioDispatcher) {
                    val validAmigoId = participante.amigoSorteadoId?.takeIf { it > 0 }
                    val nomeAmigo = validAmigoId?.let { participanteRepository.getNomeAmigoSorteado(it) }
                    val desejos: List<Desejo> = validAmigoId?.let { desejoRepository.listarPorParticipante(it) } ?: emptyList()
                    val msg = MensagemSecretaBuilder.gerar(participante.nome, nomeAmigo, desejos)
                    participanteRepository.marcarComoEnviado(participante.id)
                    msg
                }
                _mensagemCompartilhamentoResult.value = MensagemCompartilhamentoResultado(participante, mensagem)
            } catch (e: Exception) {
                handleDbError(e, "prepararMensagemCompartilhamento: failed for participanteId=${participante.id}", R.string.error_load_share_data)
            }
        }
    }

    /**
     * Obtém o nome do amigo sorteado para gerar o QR Code de um participante.
     * Acessa o banco em background; posta o resultado em qrCodeResult.
     */
    fun obterNomeAmigoParaQr(participante: Participante) {
        val amigoId = participante.amigoSorteadoId?.takeIf { it > 0 } ?: return
        launchTracked {
            try {
                val nomeAmigo = withContext(ioDispatcher) {
                    participanteRepository.getNomeAmigoSorteado(amigoId)
                }
                _qrCodeResult.value = QrCodeResultado(participante.nome ?: "", nomeAmigo)
            } catch (e: Exception) {
                handleDbError(e, "obterNomeAmigoParaQr: failed for participanteId=${participante.id}", R.string.qr_erro_gerar)
            }
        }
    }

    /**
     * Trata exceções de operações de banco:
     * - Loga sempre via Timber.e (stack trace visível no adb logcat em debug)
     * - Posta a mensagem de erro no main thread em todos os casos
     */
    private fun handleDbError(e: Exception, logMsg: String, errorStringRes: Int) {
        Timber.e(e, logMsg)
        _errorMessage.value = getApplication<Application>().getString(errorStringRes)
    }

    @VisibleForTesting
    fun setRepositories(
        participanteRepository: ParticipanteRepository,
        desejoRepository: DesejoRepository,
        sorteioRepository: SorteioRepository = this.sorteioRepository
    ) {
        this.participanteRepository = participanteRepository
        this.desejoRepository = desejoRepository
        this.sorteioRepository = sorteioRepository
    }
}
