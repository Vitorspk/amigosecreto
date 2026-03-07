package activity.amigosecreto;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import activity.amigosecreto.db.GrupoDAO;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class GruposActivityTest {

    private GrupoDAO dao;
    private ActivityScenario<GruposActivity> scenario;

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dao = new GrupoDAO(ctx);
        dao.open();
        dao.limparTudo();
        dao.close();
        // Activity lancada APOS limpar o banco para evitar race condition
        scenario = ActivityScenario.launch(GruposActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
        dao.open();
        dao.limparTudo();
        dao.close();
    }

    // --- Tela inicial ---

    @Test
    public void tela_inicial_exibe_botao_criar_grupo() {
        onView(withId(R.id.btn_criar_grupo)).check(matches(isDisplayed()));
    }

    // --- Criar grupo ---

    @Test
    public void criar_grupo_exibe_dialog() {
        onView(withId(R.id.btn_criar_grupo)).perform(click());
        onView(withId(R.id.et_nome_grupo)).check(matches(isDisplayed()));
    }

    @Test
    public void criar_grupo_com_nome_aparece_na_lista() {
        onView(withId(R.id.btn_criar_grupo)).perform(click());
        onView(withId(R.id.et_nome_grupo)).perform(typeText("Familia Teste"), closeSoftKeyboard());
        onView(withId(R.id.btn_criar)).perform(click());

        onView(withText("Familia Teste")).check(matches(isDisplayed()));
    }

    @Test
    public void criar_grupo_nome_vazio_nao_fecha_dialog() {
        onView(withId(R.id.btn_criar_grupo)).perform(click());
        onView(withId(R.id.btn_criar)).perform(click());
        // Dialog ainda visivel — campo de nome ainda presente
        onView(withId(R.id.et_nome_grupo)).check(matches(isDisplayed()));
    }

    @Test
    public void cancelar_dialog_fecha_sem_criar_grupo() {
        onView(withId(R.id.btn_criar_grupo)).perform(click());
        onView(withId(R.id.btn_cancelar)).perform(click());
        // Botao criar grupo ainda visivel — tela principal
        onView(withId(R.id.btn_criar_grupo)).check(matches(isDisplayed()));
    }

    // --- Chip sugestoes ---

    @Test
    public void chip_familia_preenche_nome() {
        onView(withId(R.id.btn_criar_grupo)).perform(click());
        onView(withId(R.id.chip_familia)).perform(click());
        onView(withId(R.id.et_nome_grupo)).check(matches(withText(R.string.chip_sugestao_familia)));
    }

    @Test
    public void chip_trabalho_preenche_nome() {
        onView(withId(R.id.btn_criar_grupo)).perform(click());
        onView(withId(R.id.chip_trabalho)).perform(click());
        onView(withId(R.id.et_nome_grupo)).check(matches(withText(R.string.chip_sugestao_trabalho)));
    }
}
