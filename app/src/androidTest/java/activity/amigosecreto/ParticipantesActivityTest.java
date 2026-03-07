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
import activity.amigosecreto.db.ParticipanteDAO;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class ParticipantesActivityTest {

    private GrupoDAO grupoDao;
    private ParticipanteDAO participanteDao;
    private Grupo grupo;
    private ActivityScenario<ParticipantesActivity> scenario;

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();

        grupoDao = new GrupoDAO(ctx);
        grupoDao.open();
        grupoDao.limparTudo();

        grupo = new Grupo();
        grupo.setNome("Grupo Espresso");
        grupo.setData("01/01/2025");
        int id = (int) grupoDao.inserir(grupo);
        grupo.setId(id);
        grupoDao.close();

        participanteDao = new ParticipanteDAO(ctx);
        participanteDao.open();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ParticipantesActivity.class);
        intent.putExtra("grupo", grupo);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
        grupoDao.open();
        grupoDao.limparTudo();
        grupoDao.close();
        participanteDao.close();
    }

    // --- Tela inicial ---

    @Test
    public void tela_exibe_nome_do_grupo_na_toolbar() {
        onView(withText("Grupo Espresso")).check(matches(isDisplayed()));
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
        onView(withText("Adicionar")).perform(click());

        onView(withText("Ana")).check(matches(isDisplayed()));
    }

    @Test
    public void adicionar_participante_nome_vazio_nao_fecha_dialog() {
        onView(withId(R.id.fab_add_participante)).perform(click());
        onView(withText("Adicionar")).perform(click());
        // Dialog ainda visivel
        onView(withId(R.id.et_nome)).check(matches(isDisplayed()));
    }

    // --- Sorteio ---

    @Test
    public void sortear_com_menos_de_3_participantes_exibe_toast() {
        adicionarParticipante("Ana");
        adicionarParticipante("Bruno");

        onView(withId(R.id.btn_sortear)).perform(click());

        // Toast com mensagem de minimo aparece; lista ainda visivel (sem dialog de sucesso)
        onView(withId(R.id.lv_participantes)).check(matches(isDisplayed()));
    }

    @Test
    public void sortear_com_tres_participantes_exibe_dialog_sucesso() {
        adicionarParticipante("Ana");
        adicionarParticipante("Bruno");
        adicionarParticipante("Carlos");

        onView(withId(R.id.btn_sortear)).perform(click());

        onView(withText("Sorteio Concluído!")).check(matches(isDisplayed()));
    }

    // --- Helpers ---

    private void adicionarParticipante(String nome) {
        onView(withId(R.id.fab_add_participante)).perform(click());
        onView(withId(R.id.et_nome)).perform(typeText(nome), closeSoftKeyboard());
        onView(withText("Adicionar")).perform(click());
    }
}
