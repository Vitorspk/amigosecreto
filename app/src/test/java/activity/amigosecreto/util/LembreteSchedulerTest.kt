package activity.amigosecreto.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LembreteSchedulerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun agendar_enfileira_trabalho_periodico() {
        LembreteScheduler.agendar(context)

        val trabalhos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(LembreteScheduler.NOME_TRABALHO)
            .get()

        assertTrue("Deve haver ao menos um trabalho agendado", trabalhos.isNotEmpty())
        val estado = trabalhos[0].state
        // PeriodicWorkRequest em modo de teste pode estar ENQUEUED, RUNNING ou BLOCKED
        // (BLOCKED é o estado inicial válido para trabalho periódico aguardando janela)
        assertTrue(
            "Trabalho deve estar enfileirado, rodando ou bloqueado",
            estado == WorkInfo.State.ENQUEUED || estado == WorkInfo.State.RUNNING
                    || estado == WorkInfo.State.BLOCKED
        )
    }

    @Test
    fun agendar_duas_vezes_mantem_um_trabalho() {
        LembreteScheduler.agendar(context)
        LembreteScheduler.agendar(context)

        val trabalhos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(LembreteScheduler.NOME_TRABALHO)
            .get()

        assertEquals("Deve haver exatamente um trabalho agendado", 1, trabalhos.size)
    }

    @Test
    fun cancelar_remove_trabalho_agendado() {
        LembreteScheduler.agendar(context)
        LembreteScheduler.cancelar(context)

        val trabalhos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(LembreteScheduler.NOME_TRABALHO)
            .get()

        val ativo = trabalhos.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
        assertTrue("Trabalho deve estar cancelado ou não existir", !ativo)
    }

    @Test
    fun cancelar_sem_agendar_nao_lanca_excecao() {
        // Não deve lançar nenhuma exceção
        LembreteScheduler.cancelar(context)
    }

    @Test
    fun trabalho_usa_intervalo_de_24_horas() {
        assertEquals(24L, LembreteScheduler.INTERVALO_HORAS)
    }
}
