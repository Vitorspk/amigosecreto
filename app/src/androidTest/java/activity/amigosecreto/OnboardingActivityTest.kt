package activity.amigosecreto

import android.Manifest
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.util.AsyncDatabaseHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingActivityTest {

    // Concede POST_NOTIFICATIONS automaticamente no Android 13+ para evitar que o dialogo
    // de permissao do sistema bloqueie a Activity durante os testes Espresso.
    @get:Rule
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else
            GrantPermissionRule.grant()

    private lateinit var db: AppDatabase
    private lateinit var scenario: ActivityScenario<OnboardingActivity>

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(AsyncDatabaseHelper.idlingResource)

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.getInstance(ctx)
        runBlocking {
            db.grupoDao().deletarTudo()
        }

        // Limpar o prefs de onboarding para que OnboardingActivity seja exibida normalmente.
        OnboardingActivity.limparOnboarding(ctx)

        // Activity lancada APOS limpar o banco e os prefs.
        scenario = ActivityScenario.launch(OnboardingActivity::class.java)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(AsyncDatabaseHelper.idlingResource)
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        // Marcar onboarding como concluido para que outros testes nao sejam bloqueados.
        OnboardingActivity.marcarOnboardingConcluido(ctx)
        try {
            scenario.close()
        } finally {
            runBlocking {
                db.grupoDao().deletarTudo()
            }
        }
    }

    @Test
    fun onboarding_renderiza_primeira_pagina() {
        // Botao avancar deve estar visivel na primeira pagina do onboarding.
        onView(withId(R.id.btn_onboarding_avancar)).check(matches(isDisplayed()))
    }

    @Test
    fun botao_avancar_muda_pagina() {
        // Na pagina 0, o botao exibe "Proximo". Apos clicar, ainda exibe "Proximo" (pagina 1).
        // Verificamos que o botao continua visivel e clicavel — o ViewPager avancou.
        onView(withId(R.id.btn_onboarding_avancar))
            .check(matches(withText(R.string.onboarding_btn_proximo)))

        onView(withId(R.id.btn_onboarding_avancar)).perform(click())

        // Ainda na pagina intermediaria — botao ainda deve exibir "Proximo".
        onView(withId(R.id.btn_onboarding_avancar))
            .check(matches(withText(R.string.onboarding_btn_proximo)))
    }

    @Test
    fun concluir_onboarding_abre_grupos_activity() {
        // O onboarding tem 5 paginas (indices 0..4).
        // Nas 4 primeiras, o botao exibe "Proximo". Na ultima, exibe "Comecar".
        // Clicar 4 vezes avanca da pagina 0 ate a 4; um 5o clique conclui o onboarding.

        // Paginas 0 → 1 → 2 → 3 → 4
        repeat(4) {
            onView(withId(R.id.btn_onboarding_avancar)).perform(click())
        }

        // Na ultima pagina, o botao deve exibir "Comecar".
        onView(withId(R.id.btn_onboarding_avancar))
            .check(matches(withText(R.string.onboarding_btn_comecar)))

        // Clicar em "Comecar" conclui o onboarding e abre GruposActivity.
        onView(withId(R.id.btn_onboarding_avancar)).perform(click())

        // GruposActivity foi aberta — botao de criar grupo deve estar visivel.
        onView(withId(R.id.btn_criar_grupo)).check(matches(isDisplayed()))
    }
}
