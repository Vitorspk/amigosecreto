package activity.amigosecreto;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
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

import activity.amigosecreto.db.Participante;
import activity.amigosecreto.repository.DesejoRepository;
import activity.amigosecreto.repository.ParticipanteRepository;
import activity.amigosecreto.util.SorteioEngine;

public class ParticipantesViewModel extends AndroidViewModel {

    /** Resultado do sorteio — substitui sealed class (Java puro). */
    public static class SorteioResultado {
        public enum Status { SUCCESS, FAILURE_NOT_ENOUGH, FAILURE_IMPOSSIBLE }
        public final Status status;
        public SorteioResultado(Status status) { this.status = status; }
    }

    private final MutableLiveData<List<Participante>> participants =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Map<Integer, Integer>> wishCounts =
            new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<SorteioResultado> sorteioResult = new MutableLiveData<>(null);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

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

    /** Limpa o resultado do sorteio após consumo pela Activity. */
    public void clearSorteioResult() { sorteioResult.setValue(null); }

    /** Limpa a mensagem de erro após exibição pela Activity. */
    public void clearErrorMessage() { errorMessage.setValue(null); }

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
                Map<Integer, Integer> counts = new HashMap<>();
                for (Participante p : lista) {
                    counts.put(p.getId(), desejoRepository.contarDesejosPorParticipante(p.getId()));
                }
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
                    errorMessage.setValue("Erro ao carregar participantes.");
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
                    errorMessage.setValue("Erro ao salvar sorteio. Tente novamente.");
                }
            });
        });
    }

    private void postMain(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    /** Visível para testes — permite substituir executor síncrono em testes unitários. */
    void setExecutorService(ExecutorService executor) {
        this.executor = executor;
    }

    /** Visível para testes — permite injetar repositories substitutos. */
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
