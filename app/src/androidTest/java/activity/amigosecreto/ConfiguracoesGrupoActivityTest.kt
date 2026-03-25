package activity.amigosecreto

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.util.AsyncDatabaseHelper
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class ConfiguracoesGrupoActivityTest {

    // Concede POST_NOTIFICATIONS automaticamente no Android 13+ para evitar que o dialogo
    // de permissao do sistema bloqueie a Activity durante os testes Espresso.
    @get:Rule
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else
            GrantPermissionRule.grant()

    private lateinit var db: AppDatabase
    private lateinit var scenario: ActivityScenario<ConfiguracoesGrupoActivity>
    private lateinit var grupo: Grupo

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(AsyncDatabaseHelper.idlingResource)

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.getInstance(ctx)

        runBlocking {
            db.grupoDao().deletarTodosParticipantes()
            db.grupoDao().deletarTodosGrupos()
        }

        // Marcar onboarding como concluido para evitar redirecionamento.
        OnboardingActivity.marcarOnboardingConcluido(ctx)

        // Inserir um grupo de teste e recupera-lo com o id gerado pelo banco.
        val grupoId = runBlocking {
            db.grupoDao().inserir(Grupo().apply { nome = "Grupo Teste" })
        }.toInt()
        grupo = runBlocking { db.grupoDao().buscarPorId(grupoId) }!!

        val intent = Intent(ctx, ConfiguracoesGrupoActivity::class.java)
            .putExtra("grupo", grupo)
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(AsyncDatabaseHelper.idlingResource)
        try {
            scenario.close()
        } finally {
            runBlocking {
                db.grupoDao().deletarTodosParticipantes()
                db.grupoDao().deletarTodosGrupos()
            }
        }
    }

    @Test
    fun formulario_populado_com_nome_do_grupo() {
        // O campo de nome deve ser pre-populado com o nome do grupo passado via Intent.
        onView(withId(R.id.et_config_nome)).check(matches(withText("Grupo Teste")))
    }

    @Test
    fun botao_salvar_habilitado_com_nome() {
        // Botao de salvar deve estar visivel e habilitado quando ha um nome no formulario.
        onView(withId(R.id.btn_salvar_configuracoes)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_salvar_configuracoes)).check(matches(isEnabled()))
    }

    @Test
    fun salvar_fecha_activity() {
        // Digitar um nome valido no campo.
        onView(withId(R.id.et_config_nome)).perform(
            replaceText("Grupo Atualizado"),
            closeSoftKeyboard()
        )

        // Fechar teclado e clicar em salvar.
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.btn_salvar_configuracoes)).perform(scrollTo(), click())

        // Aguardar a Activity atingir o estado DESTROYED (finish() chamado no sucesso).
        val deadline = System.currentTimeMillis() + 5000
        while (scenario.state != Lifecycle.State.DESTROYED && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
        }

        // A Activity deve ter fechado apos salvar com sucesso.
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }
}
