package activity.amigosecreto;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.GrupoDAO;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class ParticipantesActivityTest {

    private GrupoDAO grupoDao;
    private Grupo grupo;
    private ActivityScenario<ParticipantesActivity> scenario;

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Limpar banco completamente antes de cada teste para garantir isolamento.
        // GrupoDAO.limparTudo() apaga grupos, participantes e exclusoes em transacao atomica.
        grupoDao = new GrupoDAO(ctx);
        grupoDao.open();
        grupoDao.limparTudo();

        grupo = new Grupo();
        grupo.setNome("Grupo Espresso");
        grupo.setData("01/01/2025");
        int id = (int) grupoDao.inserir(grupo);
        grupo.setId(id);
        grupoDao.close();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ParticipantesActivity.class);
        intent.putExtra("grupo", grupo);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        try {
            if (scenario != null) scenario.close();
        } finally {
            // Cria novo DAO para tearDown — evita reuso de DAO potencialmente em estado invalido
            // caso setUp tenha falhado antes de concluir a inicializacao.
            Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
            GrupoDAO cleanupDao = new GrupoDAO(ctx);
            cleanupDao.open();
            cleanupDao.limparTudo();
            cleanupDao.close();
        }
    }

    // --- Tela inicial ---

    @Test
    public void tela_exibe_contagem_de_participantes() {
        // tv_count exibe o estado atual da lista (acessivel via view hierarchy, ao contrario do ActionBar title)
        onView(withId(R.id.tv_count)).check(matches(isDisplayed()));
    }

    @Test
    public void tela_exibe_lista_de_participantes() {
        onView(withId(R.id.lv_participantes)).check(matches(isDisplayed()));
    }

    @Test
    public void tela_exibe_fab_adicionar() {
        onView(withId(R.id.fab_add_participante)).check(matches(isDisplayed()));
    }

    // --- Adicionar participante ---

    @Test
    public void fab_add_abre_dialog() {
        onView(withId(R.id.fab_add_participante)).perform(click());
        onView(withId(R.id.et_nome)).check(matches(isDisplayed()));
    }

    @Test
    public void adicionar_participante_aparece_na_lista() {
        onView(withId(R.id.fab_add_participante)).perform(click());
        onView(withId(R.id.et_nome)).perform(typeText("Ana"), closeSoftKeyboard());
        onView(withText(R.string.button_add)).perform(click());

        onView(withText("Ana")).check(matches(isDisplayed()));
    }

    @Test
    public void adicionar_participante_nome_vazio_nao_fecha_dialog() {
        onView(withId(R.id.fab_add_participante)).perform(click());
        onView(withText(R.string.button_add)).perform(click());
        // Dialog ainda visivel — campo de nome presente
        onView(withId(R.id.et_nome)).check(matches(isDisplayed()));
    }

    // --- Sorteio ---

    @Test
    public void sortear_com_menos_de_3_participantes_nao_abre_dialog_sucesso() {
        adicionarParticipante("Ana");
        adicionarParticipante("Bruno");

        onView(withId(R.id.btn_sortear)).perform(click());

        // O Toast de aviso nao e verificado aqui: Toasts nao sao parte da view hierarchy do Espresso
        // e requerem espresso-contrib ToastMatcher (flaky em CI). A ausencia do dialog de sucesso
        // e prova suficiente de que a regra de negocio foi aplicada.
        onView(withText(R.string.participante_sorteio_titulo)).check(doesNotExist());
    }

    @Test
    public void sortear_com_tres_participantes_exibe_dialog_sucesso() {
        adicionarParticipante("Ana");
        adicionarParticipante("Bruno");
        adicionarParticipante("Carlos");

        onView(withId(R.id.btn_sortear)).perform(click());

        onView(withText(R.string.participante_sorteio_titulo)).check(matches(isDisplayed()));
    }

    // --- Helpers ---

    private void adicionarParticipante(String nome) {
        onView(withId(R.id.fab_add_participante)).perform(click());
        onView(withId(R.id.et_nome)).perform(typeText(nome), closeSoftKeyboard());
        onView(withText(R.string.button_add)).perform(click());
    }
}
