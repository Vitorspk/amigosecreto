package activity.amigosecreto;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import activity.amigosecreto.R;
import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.Participante;
import activity.amigosecreto.repository.DesejoRepository;
import activity.amigosecreto.repository.ParticipanteRepository;
import activity.amigosecreto.util.MensagemSecretaBuilder;
import activity.amigosecreto.util.SorteioEngine;

public class ParticipantesViewModel extends AndroidViewModel {

    /** Resultado do sorteio — substitui sealed class (Java puro). */
    public static class SorteioResultado {
        public enum Status { SUCCESS, FAILURE_NOT_ENOUGH, FAILURE_IMPOSSIBLE }
        public final Status status;
        public SorteioResultado(Status status) { this.status = status; }
    }

    /** Resultado de prepararMensagensSms — lista de participantes com telefone + mapa de mensagens. */
    public static class MensagensSmsResultado {
        public final List<Participante> participantesComTelefone;
        public final Map<Integer, String> mensagens;

        public MensagensSmsResultado(List<Participante> participantesComTelefone,
                                     Map<Integer, String> mensagens) {
            this.participantesComTelefone = participantesComTelefone;
            this.mensagens = mensagens;
        }
    }

    /** Resultado de prepararMensagemCompartilhamento — mensagem formatada para um participante. */
    public static class MensagemCompartilhamentoResultado {
        public final Participante participante;
        public final String mensagem;

        public MensagemCompartilhamentoResultado(Participante participante, String mensagem) {
            this.participante = participante;
            this.mensagem = mensagem;
        }
    }

    private final MutableLiveData<List<Participante>> participants =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Map<Integer, Integer>> wishCounts =
            new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<SorteioResultado> sorteioResult = new MutableLiveData<>(null);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<MensagensSmsResultado> mensagensSmsResult = new MutableLiveData<>(null);
    private final MutableLiveData<MensagemCompartilhamentoResultado> mensagemCompartilhamentoResult =
            new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> atualizarSucesso = new MutableLiveData<>(null);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private int grupoId = -1;

    private ParticipanteRepository participanteRepository;
    private DesejoRepository desejoRepository;

    public ParticipantesViewModel(@NonNull Application application) {
        super(application);
        participanteRepository = new ParticipanteRepository(application);
        desejoRepository = new DesejoRepository(application);
    }

    /**
     * Deve ser chamado uma vez em onCreate() após obter o ViewModel.
     * A guarda por grupoId evita recarregar após rotação de tela.
     */
    public void init(int grupoId) {
        if (this.grupoId == grupoId) return;
        this.grupoId = grupoId;
        carregarParticipantes();
    }

    public LiveData<List<Participante>> getParticipants() { return participants; }
    public LiveData<Map<Integer, Integer>> getWishCounts() { return wishCounts; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<SorteioResultado> getSorteioResult() { return sorteioResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<MensagensSmsResultado> getMensagensSmsResult() { return mensagensSmsResult; }
    public LiveData<MensagemCompartilhamentoResultado> getMensagemCompartilhamentoResult() {
        return mensagemCompartilhamentoResult;
    }

    public LiveData<Boolean> getAtualizarSucesso() { return atualizarSucesso; }
    public void clearAtualizarSucesso() { atualizarSucesso.setValue(null); }

    /** Marca participante como enviado em background (evita ANR). */
    public void marcarComoEnviado(int participanteId) {
        executor.execute(() -> {
            try {
                participanteRepository.marcarComoEnviado(participanteId);
            } catch (Exception e) {
                postMain(() -> errorMessage.setValue(getApplication().getString(R.string.error_save_failed)));
            }
        });
    }

    /** Remove participante em background (evita ANR). */
    public void removerParticipante(int participanteId) {
        executor.execute(() -> {
            try {
                participanteRepository.remover(participanteId);
                postMain(this::carregarParticipantes);
            } catch (Exception e) {
                postMain(() -> errorMessage.setValue(getApplication().getString(R.string.error_save_failed)));
            }
        });
    }

    /**
     * Atualiza participante em background (evita ANR).
     * Posta true em atualizarSucesso se bem-sucedido, false caso contrário.
     * A Activity observa para fechar o dialog ou restaurar o estado original.
     */
    public void atualizarParticipante(Participante participante) {
        executor.execute(() -> {
            boolean ok;
            try {
                ok = participanteRepository.atualizar(participante);
            } catch (Exception e) {
                ok = false;
            }
            final boolean sucesso = ok;
            postMain(() -> {
                atualizarSucesso.setValue(sucesso);
                if (sucesso) carregarParticipantes();
            });
        });
    }

    /** Insere participante em background (evita ANR). */
    public void inserirParticipante(Participante participante, int grupoId) {
        executor.execute(() -> {
            try {
                participanteRepository.inserir(participante, grupoId);
                postMain(this::carregarParticipantes);
            } catch (Exception e) {
                postMain(() -> errorMessage.setValue(getApplication().getString(R.string.error_save_failed)));
            }
        });
    }

    /** Deleta todos os participantes de um grupo em background (evita ANR). */
    public void deletarTodosDoGrupo(int grupoId) {
        executor.execute(() -> {
            try {
                participanteRepository.deletarTodosDoGrupo(grupoId);
                postMain(this::carregarParticipantes);
            } catch (Exception e) {
                postMain(() -> errorMessage.setValue(getApplication().getString(R.string.error_save_failed)));
            }
        });
    }

    /** Salva exclusões de um participante em background em transação atômica (evita ANR e falha parcial). */
    public void salvarExclusoes(int participanteId, List<Integer> adicionar, List<Integer> remover) {
        executor.execute(() -> {
            try {
                participanteRepository.salvarExclusoes(participanteId, adicionar, remover);
                postMain(this::carregarParticipantes);
            } catch (Exception e) {
                postMain(() -> errorMessage.setValue(getApplication().getString(R.string.error_save_failed)));
            }
        });
    }

    /** Limpa o resultado do sorteio após consumo pela Activity. */
    public void clearSorteioResult() { sorteioResult.setValue(null); }

    /** Limpa a mensagem de erro após exibição pela Activity. */
    public void clearErrorMessage() { errorMessage.setValue(null); }

    /** Limpa o resultado de mensagens SMS após consumo pela Activity. */
    public void clearMensagensSmsResult() { mensagensSmsResult.setValue(null); }

    /** Limpa o resultado de compartilhamento após consumo pela Activity. */
    public void clearMensagemCompartilhamentoResult() { mensagemCompartilhamentoResult.setValue(null); }

    /**
     * Carrega participantes e contagens de desejos do banco em background.
     * Pode ser chamado a qualquer momento para forçar atualização (ex: após voltar de outra tela).
     */
    public void carregarParticipantes() {
        if (grupoId == -1) return;
        isLoading.setValue(true);
        executor.execute(() -> {
            try {
                List<Participante> lista = participanteRepository.listarPorGrupo(grupoId);
                Map<Integer, Integer> counts = desejoRepository.contarDesejosPorGrupo(grupoId);
                final List<Participante> finalLista = lista;
                final Map<Integer, Integer> finalCounts = counts;
                postMain(() -> {
                    participants.setValue(finalLista);
                    wishCounts.setValue(finalCounts);
                    isLoading.setValue(false);
                });
            } catch (Exception e) {
                postMain(() -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(getApplication().getString(R.string.error_load_participants));
                });
            }
        });
    }

    /**
     * Executa o sorteio em background.
     * Posta o resultado em sorteioResult; a Activity observa e exibe o dialog/toast adequado.
     */
    public void realizarSorteio() {
        List<Participante> snapshot = participants.getValue();
        if (snapshot == null || snapshot.size() < 3) {
            sorteioResult.setValue(new SorteioResultado(SorteioResultado.Status.FAILURE_NOT_ENOUGH));
            return;
        }

        isLoading.setValue(true);
        final List<Participante> sortableSnapshot = new ArrayList<>(snapshot);

        executor.execute(() -> {
            List<Participante> sorteados = null;
            int tentativas = 0;
            while (sorteados == null && tentativas < 100) {
                tentativas++;
                sorteados = SorteioEngine.tentarSorteio(new ArrayList<>(sortableSnapshot));
            }

            if (sorteados == null) {
                postMain(() -> {
                    isLoading.setValue(false);
                    sorteioResult.setValue(new SorteioResultado(SorteioResultado.Status.FAILURE_IMPOSSIBLE));
                });
                return;
            }

            boolean sucesso;
            try {
                sucesso = participanteRepository.salvarSorteio(sortableSnapshot, sorteados);
            } catch (Exception e) {
                sucesso = false;
            }

            final boolean salvo = sucesso;
            postMain(() -> {
                isLoading.setValue(false);
                if (salvo) {
                    carregarParticipantes();
                    sorteioResult.setValue(new SorteioResultado(SorteioResultado.Status.SUCCESS));
                } else {
                    errorMessage.setValue(getApplication().getString(R.string.error_save_draw));
                }
            });
        });
    }

    /**
     * Prepara as mensagens SMS para todos os participantes com telefone.
     * Acessa o banco em background; posta o resultado em mensagensSmsResult.
     * Aceita uma lista de participantes para suportar tanto o fluxo normal (snapshot da lista)
     * quanto a reconstrução após rotação (lista restaurada do bundle).
     */
    public void prepararMensagensSms(final List<Participante> snapshot) {
        executor.execute(() -> {
            try {
                // Nomes frescos do banco — evita dados desatualizados se alguém editou um
                // participante em outra tela após o snapshot ter sido capturado.
                List<Participante> participantesAtuais = participanteRepository.listarPorGrupo(grupoId);
                Map<Integer, String> nomeMap = new HashMap<>();
                for (Participante p : participantesAtuais) nomeMap.put(p.getId(), p.getNome());

                // Uma única query traz todos os desejos do grupo de uma vez.
                Map<Integer, List<Desejo>> desejosMap =
                        desejoRepository.listarDesejosPorGrupo(grupoId);

                List<Participante> comTelefone = new ArrayList<>();
                Map<Integer, String> mensagens = new HashMap<>();
                for (Participante p : snapshot) {
                    if (p.getTelefone() != null && !p.getTelefone().trim().isEmpty()) {
                        comTelefone.add(p);
                        Integer amigoId = p.getAmigoSorteadoId();
                        String nomeAmigo = (amigoId != null && amigoId > 0)
                                ? nomeMap.get(amigoId) : null;
                        List<Desejo> desejosList = (amigoId != null && amigoId > 0)
                                ? desejosMap.get(amigoId) : null;
                        List<Desejo> desejos = desejosList != null ? desejosList : new ArrayList<>();
                        mensagens.put(p.getId(), MensagemSecretaBuilder.gerar(p.getNome(), nomeAmigo, desejos));
                    }
                }
                final MensagensSmsResultado resultado = new MensagensSmsResultado(comTelefone, mensagens);
                postMain(() -> mensagensSmsResult.setValue(resultado));
            } catch (Exception e) {
                postMain(() -> errorMessage.setValue(getApplication().getString(R.string.error_prepare_messages_failed)));
            }
        });
    }

    /**
     * Prepara a mensagem de compartilhamento (WhatsApp/share sheet) para um participante.
     * Acessa o banco em background, marca como enviado e posta o resultado em
     * mensagemCompartilhamentoResult.
     *
     * Limitação conhecida: marcarComoEnviado é chamado antes do usuário confirmar
     * o share sheet (a API do ACTION_SEND não oferece callback de confirmação).
     */
    public void prepararMensagemCompartilhamento(final Participante participante) {
        executor.execute(() -> {
            try {
                Integer amigoId = participante.getAmigoSorteadoId();
                String nomeAmigo = (amigoId != null && amigoId > 0)
                        ? participanteRepository.getNomeAmigoSorteado(amigoId) : null;
                List<Desejo> desejos = (amigoId != null && amigoId > 0)
                        ? desejoRepository.listarPorParticipante(amigoId)
                        : new ArrayList<>();
                participanteRepository.marcarComoEnviado(participante.getId());
                String mensagem = MensagemSecretaBuilder.gerar(participante.getNome(), nomeAmigo, desejos);
                postMain(() -> mensagemCompartilhamentoResult.setValue(
                        new MensagemCompartilhamentoResultado(participante, mensagem)));
            } catch (Exception e) {
                postMain(() -> errorMessage.setValue(getApplication().getString(R.string.error_load_share_data)));
            }
        });
    }

    private void postMain(Runnable r) {
        mainHandler.post(r);
    }

    @VisibleForTesting
    void setExecutorService(ExecutorService executor) {
        if (!this.executor.isShutdown()) this.executor.shutdown();
        this.executor = executor;
    }

    @VisibleForTesting
    void setRepositories(ParticipanteRepository participanteRepository, DesejoRepository desejoRepository) {
        this.participanteRepository = participanteRepository;
        this.desejoRepository = desejoRepository;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
