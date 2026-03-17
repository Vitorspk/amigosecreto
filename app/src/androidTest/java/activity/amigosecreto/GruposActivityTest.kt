package activity.amigosecreto

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import activity.amigosecreto.db.GrupoDAO
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GruposActivityTest {

    private lateinit var dao: GrupoDAO
    private lateinit var scenario: ActivityScenario<GruposActivity>

    @Before
    fun setUp() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        dao = GrupoDAO(ctx)
        dao.open()
        dao.limparTudo()
        dao.close()
        // Activity lancada APOS limpar o banco para evitar race condition
        scenario = ActivityScenario.launch(GruposActivity::class.java)
    }

    @After
    fun tearDown() {
        try {
            scenario.close()
        } finally {
            // Cria novo DAO para tearDown — evita reuso de DAO potencialmente em estado invalido
            // caso setUp tenha falhado antes de concluir a inicializacao.
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            val cleanupDao = GrupoDAO(ctx)
            cleanupDao.open()
            cleanupDao.limparTudo()
            cleanupDao.close()
        }
    }

    // --- Tela inicial ---

    @Test
    fun tela_inicial_exibe_botao_criar_grupo() {
        onView(withId(R.id.btn_criar_grupo)).check(matches(isDisplayed()))
    }

    @Test
    fun tela_inicial_sem_grupos_nao_exibe_itens_na_lista() {
        // ListView vazio — nenhum item com texto de grupo existe
        onView(withText("Familia Teste")).check(doesNotExist())
    }

    // --- Criar grupo ---

    @Test
    fun criar_grupo_exibe_dialog() {
        onView(withId(R.id.btn_criar_grupo)).perform(click())
        onView(withId(R.id.et_nome_grupo)).check(matches(isDisplayed()))
    }

    @Test
    fun criar_grupo_com_nome_aparece_na_lista() {
        onView(withId(R.id.btn_criar_grupo)).perform(click())
        onView(withId(R.id.et_nome_grupo)).perform(typeText("Familia Teste"), closeSoftKeyboard())
        onView(withId(R.id.btn_criar)).perform(click())

        onView(withText("Familia Teste")).check(matches(isDisplayed()))
    }

    @Test
    fun criar_grupo_nome_vazio_nao_fecha_dialog() {
        onView(withId(R.id.btn_criar_grupo)).perform(click())
        onView(withId(R.id.btn_criar)).perform(click())
        // Dialog ainda visivel — campo de nome ainda presente
        onView(withId(R.id.et_nome_grupo)).check(matches(isDisplayed()))
    }

    @Test
    fun cancelar_dialog_fecha_sem_criar_grupo() {
        onView(withId(R.id.btn_criar_grupo)).perform(click())
        onView(withId(R.id.btn_cancelar)).perform(click())
        // Botao criar grupo ainda visivel — tela principal
        onView(withId(R.id.btn_criar_grupo)).check(matches(isDisplayed()))
    }

    @Test
    fun criar_dois_grupos_ambos_aparecem_na_lista() {
        criarGrupo("Amigos")
        criarGrupo("Trabalho")

        onView(withText("Amigos")).check(matches(isDisplayed()))
        onView(withText("Trabalho")).check(matches(isDisplayed()))
    }

    // --- Editar grupo (long press → menu contextual) ---

    @Test
    fun editar_nome_grupo_atualiza_lista() {
        criarGrupo("Nome Antigo")

        onView(withText("Nome Antigo")).perform(longClick())
        onView(withText(R.string.grupo_menu_editar_nome)).check(matches(isDisplayed()))
        onView(withText(R.string.grupo_menu_editar_nome)).perform(click())
        onView(withId(R.id.et_nome_grupo)).perform(
            replaceText("Nome Novo"), closeSoftKeyboard()
        )
        onView(withId(R.id.btn_criar)).perform(click())

        onView(withText("Nome Novo")).check(matches(isDisplayed()))
        onView(withText("Nome Antigo")).check(doesNotExist())
    }

    // --- Chip sugestoes ---

    @Test
    fun chip_familia_preenche_nome() {
        onView(withId(R.id.btn_criar_grupo)).perform(click())
        onView(withId(R.id.chip_familia)).perform(click())
        onView(withId(R.id.et_nome_grupo)).check(matches(withText(R.string.chip_sugestao_familia)))
    }

    @Test
    fun chip_trabalho_preenche_nome() {
        onView(withId(R.id.btn_criar_grupo)).perform(click())
        onView(withId(R.id.chip_trabalho)).perform(click())
        onView(withId(R.id.et_nome_grupo)).check(matches(withText(R.string.chip_sugestao_trabalho)))
    }

    @Test
    fun chip_amigos_preenche_nome() {
        onView(withId(R.id.btn_criar_grupo)).perform(click())
        onView(withId(R.id.chip_amigos)).perform(click())
        onView(withId(R.id.et_nome_grupo)).check(matches(withText(R.string.chip_sugestao_amigos)))
    }

    // --- Excluir grupo ---

    @Test
    fun excluir_grupo_confirmado_remove_da_lista() {
        criarGrupo("Grupo Temporario")

        onView(withText("Grupo Temporario")).perform(longClick())
        onView(withText(R.string.grupo_menu_excluir)).perform(click())
        onView(withText(R.string.button_remove_yes)).perform(click())

        onView(withText("Grupo Temporario")).check(doesNotExist())
    }

    @Test
    fun excluir_grupo_cancelado_mantem_na_lista() {
        criarGrupo("Grupo Permanente")

        onView(withText("Grupo Permanente")).perform(longClick())
        onView(withText(R.string.grupo_menu_excluir)).perform(click())
        onView(withText(R.string.button_cancel)).perform(click())

        onView(withText("Grupo Permanente")).check(matches(isDisplayed()))
    }

    // --- Navegação ---

    @Test
    fun clicar_grupo_abre_tela_de_participantes() {
        criarGrupo("Grupo Nav")

        onView(withText("Grupo Nav")).perform(click())

        // ParticipantesActivity exibe o FAB de adicionar participante
        onView(withId(R.id.fab_add_participante)).check(matches(isDisplayed()))
    }

    // --- Helpers ---

    private fun criarGrupo(nome: String) {
        onView(withId(R.id.btn_criar_grupo)).perform(click())
        onView(withId(R.id.et_nome_grupo)).perform(typeText(nome), closeSoftKeyboard())
        onView(withId(R.id.btn_criar)).perform(click())
    }
}
