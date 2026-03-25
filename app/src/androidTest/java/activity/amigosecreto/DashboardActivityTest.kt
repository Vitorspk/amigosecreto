package activity.amigosecreto

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.util.AsyncDatabaseHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardActivityTest {

    // Concede POST_NOTIFICATIONS automaticamente no Android 13+ para evitar que o dialogo
    // de permissao do sistema bloqueie a Activity durante os testes Espresso.
    @get:Rule
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else
            GrantPermissionRule.grant()

    private lateinit var db: AppDatabase
    private lateinit var scenario: ActivityScenario<DashboardActivity>

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(AsyncDatabaseHelper.idlingResource)

        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.getInstance(ctx)

        runBlocking {
            db.grupoDao().deletarTudo()
        }

        // Marcar onboarding como concluido para evitar redirecionamento.
        OnboardingActivity.marcarOnboardingConcluido(ctx)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(AsyncDatabaseHelper.idlingResource)
        try {
            scenario.close()
        } finally {
            runBlocking {
                db.grupoDao().deletarTudo()
            }
        }
    }

    // --- Helpers ---

    private fun launchComGrupo(nomeGrupo: String): Grupo {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val grupoId = runBlocking {
            db.grupoDao().inserir(Grupo().apply { nome = nomeGrupo })
        }.toInt()
        val grupo = runBlocking { db.grupoDao().buscarPorId(grupoId) }!!

        val intent = Intent(ctx, DashboardActivity::class.java)
            .putExtra(Grupo.EXTRA_GRUPO, grupo)
        scenario = ActivityScenario.launch(intent)

        return grupo
    }

    private fun inserirParticipante(nomeParticipante: String, grupoId: Int) {
        runBlocking {
            db.participanteDao().inserir(
                Participante().apply {
                    nome = nomeParticipante
                    this.grupoId = grupoId
                }
            )
        }
    }

    // --- Testes ---

    @Test
    fun dashboard_exibe_nome_do_grupo() {
        launchComGrupo("Amigos 2024")

        // TextView de nome do grupo deve exibir o nome passado via Intent.
        onView(withId(R.id.tv_dash_nome_grupo)).check(matches(withText("Amigos 2024")))
    }

    @Test
    fun dashboard_exibe_total_participantes() {
        val grupo = launchComGrupo("Grupo Participantes")

        // Inserir 2 participantes diretamente no banco apos lancamento da Activity.
        inserirParticipante("Alice", grupo.id)
        inserirParticipante("Bob", grupo.id)

        // Recarregar dados via onResume simulando retorno de outra Activity.
        scenario.recreate()

        // Total de participantes deve exibir "2".
        onView(withId(R.id.tv_dash_total_participantes)).check(matches(withText("2")))
    }

    @Test
    fun dashboard_sem_sorteio_exibe_nao_realizado() {
        launchComGrupo("Grupo Sem Sorteio")

        // Sem nenhum sorteio registrado, o campo deve exibir o texto "Nao".
        onView(withId(R.id.tv_dash_sorteio_realizado))
            .check(matches(withText(R.string.dashboard_nao)))
    }
}
