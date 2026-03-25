package activity.amigosecreto

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.util.AsyncDatabaseHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParticipantesActivityTest {

    private lateinit var db: AppDatabase
    private lateinit var grupo: Grupo
    private lateinit var scenario: ActivityScenario<ParticipantesActivity>

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(AsyncDatabaseHelper.idlingResource)
        IdlingRegistry.getInstance().register(ParticipantesViewModel.idlingResource)

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.getInstance(ctx)

        // Limpar banco completamente antes de cada teste para garantir isolamento.
        runBlocking {
            db.grupoDao().deletarTudo()
        }

        grupo = Grupo(nome = "Grupo Espresso", data = "01/01/2025")
        runBlocking {
            val id = db.grupoDao().inserir(grupo)
            grupo.id = id.toInt()
        }

        val intent = Intent(ApplicationProvider.getApplicationContext(), ParticipantesActivity::class.java)
        intent.putExtra(Grupo.EXTRA_GRUPO, grupo)
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(AsyncDatabaseHelper.idlingResource)
        IdlingRegistry.getInstance().unregister(ParticipantesViewModel.idlingResource)
        try {
            scenario.close()
        } finally {
            runBlocking {
                db.grupoDao().deletarTudo()
            }
        }
    }

    // --- Tela inicial ---

    @Test
    fun tela_exibe_contagem_de_participantes() {
        // tv_count exibe o estado atual da lista (acessivel via view hierarchy, ao contrario do ActionBar title)
        onView(withId(R.id.tv_count)).check(matches(isDisplayed()))
    }

    @Test
    fun tela_vazia_exibe_estado_empty() {
        // setUp insere grupo sem participantes — estado empty deve estar visível
        onView(withId(R.id.scroll_participantes)).check(matches(isDisplayed()))
    }

    @Test
    fun tela_exibe_fab_adicionar() {
        onView(withId(R.id.fab_add_participante)).check(matches(isDisplayed()))
    }

    @Test
    fun tela_exibe_botao_sortear() {
        onView(withId(R.id.btn_sortear)).check(matches(isDisplayed()))
    }

    // --- Adicionar participante ---

    @Test
    fun fab_add_abre_dialog() {
        onView(withId(R.id.fab_add_participante)).perform(click())
        onView(withId(R.id.et_nome)).check(matches(isDisplayed()))
    }

    @Test
    fun adicionar_participante_aparece_na_lista() {
        onView(withId(R.id.fab_add_participante)).perform(click())
        onView(withId(R.id.et_nome)).perform(typeText("Ana"), closeSoftKeyboard())
        onView(withText(R.string.button_add)).perform(click())

        onView(withText("Ana")).check(matches(isDisplayed()))
    }

    @Test
    fun adicionar_participante_nome_vazio_nao_fecha_dialog() {
        onView(withId(R.id.fab_add_participante)).perform(click())
        onView(withText(R.string.button_add)).perform(click())
        // Dialog ainda visivel — campo de nome presente
        onView(withId(R.id.et_nome)).check(matches(isDisplayed()))
    }

    @Test
    fun adicionar_multiplos_participantes_todos_aparecem() {
        adicionarParticipante("Ana")
        adicionarParticipante("Bruno")
        adicionarParticipante("Carlos")

        onView(withText("Ana")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Bruno")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Carlos")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun cancelar_dialog_add_nao_adiciona_participante() {
        onView(withId(R.id.fab_add_participante)).perform(click())
        onView(withId(R.id.et_nome)).perform(typeText("Fantasma"), closeSoftKeyboard())
        onView(withText(R.string.button_cancel)).perform(click())

        onView(withText("Fantasma")).check(doesNotExist())
    }

    // --- Sorteio ---

    @Test
    fun sortear_com_menos_de_3_participantes_nao_abre_dialog_sucesso() {
        adicionarParticipante("Ana")
        adicionarParticipante("Bruno")

        onView(withId(R.id.btn_sortear)).perform(click())

        // O Toast de aviso nao e verificado aqui: Toasts nao sao parte da view hierarchy do Espresso
        // e requerem espresso-contrib ToastMatcher (flaky em CI). A ausencia do dialog de sucesso
        // e prova suficiente de que a regra de negocio foi aplicada.
        onView(withText(R.string.participante_sorteio_titulo)).check(doesNotExist())
    }

    @Test
    fun sortear_com_tres_participantes_exibe_dialog_sucesso() {
        adicionarParticipante("Ana")
        adicionarParticipante("Bruno")
        adicionarParticipante("Carlos")

        onView(withId(R.id.btn_sortear)).perform(click())

        onView(withText(R.string.participante_sorteio_titulo)).check(matches(isDisplayed()))
    }

    @Test
    fun sortear_sem_participantes_nao_abre_dialog_sucesso() {
        onView(withId(R.id.btn_sortear)).perform(click())

        onView(withText(R.string.participante_sorteio_titulo)).check(doesNotExist())
    }

    // --- Helpers ---

    private fun adicionarParticipante(nome: String) {
        onView(withId(R.id.fab_add_participante)).perform(click())
        onView(withId(R.id.et_nome)).perform(typeText(nome), closeSoftKeyboard())
        onView(withText(R.string.button_add)).perform(click())
    }
}
