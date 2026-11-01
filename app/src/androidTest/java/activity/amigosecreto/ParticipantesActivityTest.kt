package activity.amigosecreto

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.not
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

    @Test
    fun menu_configuracoes_abre_o_grupo_atual() {
        // Confere a navegação pelo menu real, não só a Activity lançada isoladamente.
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        onView(withText(R.string.menu_configuracoes_grupo)).perform(click())
        onView(withId(R.id.et_config_nome)).check(matches(withText(grupo.nome)))
        pressBack()
        onView(withId(R.id.fab_add_participante)).check(matches(isDisplayed()))
    }

    @Test
    fun limpar_pelo_menu_remove_participantes_apos_confirmar() {
        adicionarParticipante("Ana")
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        onView(withText(R.string.button_clear_participants)).perform(click())
        onView(withText(R.string.participante_limpar_btn_sim)).perform(click())
        // ListView pode reter filhos reciclados mesmo estando GONE. Validar o
        // estado exibido, o adapter e a persistência evita confundir cache com dados.
        onView(withId(R.id.tv_empty_title)).check(matches(isDisplayed()))
        onView(withId(R.id.lv_participantes)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        scenario.onActivity {
            org.junit.Assert.assertEquals(0, it.findViewById<android.widget.ListView>(R.id.lv_participantes).adapter.count)
        }
        org.junit.Assert.assertEquals(0, runBlocking { db.participanteDao().contarPorGrupo(grupo.id) })
        onView(withId(R.id.tv_count)).check(matches(withText(R.string.label_no_participants)))
    }

    @Test
    fun tocar_participante_abre_revelacao_que_pode_ser_ocultada() {
        adicionarParticipante("Ana")
        adicionarParticipante("Bruno")
        adicionarParticipante("Carlos")
        onView(withId(R.id.btn_sortear)).perform(click())
        onView(withText(R.string.participante_sorteio_btn_agora_nao)).perform(click())
        org.junit.Assert.assertEquals(AppDatabase.SCHEMA_VERSION, db.openHelper.writableDatabase.version)
        onView(withText("Ana")).perform(click())
        onView(withId(R.id.layout_escondido)).check(matches(isDisplayed()))
        onView(withId(R.id.layout_revelado)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.card_revelacao)).perform(click())
        onView(withId(R.id.tv_amigo_sorteado)).check(matches(isDisplayed()))
        onView(withId(R.id.tv_amigo_sorteado)).check(matches(not(withText("Ana"))))
        onView(withId(R.id.card_revelacao)).perform(click())
        onView(withId(R.id.layout_escondido)).check(matches(isDisplayed()))
        onView(withId(R.id.layout_revelado)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.btn_ver_desejos)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        pressBack()
        org.junit.Assert.assertEquals(AppDatabase.SCHEMA_VERSION, db.openHelper.writableDatabase.version)
    }

    @Test
    fun revelacao_oferece_desejos_quando_o_grupo_permite() {
        sortearTresParticipantes()
        onView(withText("Ana")).perform(click())
        onView(withId(R.id.card_revelacao)).perform(click())
        onView(withId(R.id.btn_ver_desejos)).check(matches(isDisplayed()))
    }

    @Test
    fun revelacao_nao_oferece_desejos_quando_o_grupo_nao_permite() {
        scenario.close()
        grupo.permitirVerDesejos = false
        scenario = ActivityScenario.launch(
            Intent(ApplicationProvider.getApplicationContext(), ParticipantesActivity::class.java)
                .putExtra(Grupo.EXTRA_GRUPO, grupo)
        )
        sortearTresParticipantes()
        onView(withText("Ana")).perform(click())
        onView(withId(R.id.card_revelacao)).perform(click())
        onView(withId(R.id.tv_amigo_sorteado)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_ver_desejos)).check(matches(withEffectiveVisibility(Visibility.GONE)))
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

        onData(anything()).inAdapterView(withId(R.id.lv_participantes)).atPosition(0).check(matches(isDisplayed()))
        onData(anything()).inAdapterView(withId(R.id.lv_participantes)).atPosition(1).check(matches(isDisplayed()))
        onData(anything()).inAdapterView(withId(R.id.lv_participantes)).atPosition(2).check(matches(isDisplayed()))
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

    private fun sortearTresParticipantes() {
        adicionarParticipante("Ana")
        adicionarParticipante("Bruno")
        adicionarParticipante("Carlos")
        onView(withId(R.id.btn_sortear)).perform(click())
        onView(withText(R.string.participante_sorteio_btn_agora_nao)).perform(click())
    }

    private fun adicionarParticipante(nome: String) {
        onView(withId(R.id.fab_add_participante)).perform(click())
        onView(withId(R.id.et_nome)).perform(typeText(nome), closeSoftKeyboard())
        onView(withText(R.string.button_add)).perform(click())
    }
}
