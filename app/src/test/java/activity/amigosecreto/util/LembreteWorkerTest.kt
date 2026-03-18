package activity.amigosecreto.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.ParticipanteDAO
import activity.amigosecreto.db.Participante
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LembreteWorkerTest {

    private lateinit var context: Context
    private lateinit var grupoDao: GrupoDAO
    private lateinit var participanteDao: ParticipanteDAO
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationHelper.criarCanal(context)
        Shadows.shadowOf(context as android.app.Application)
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        grupoDao = GrupoDAO(context)
        grupoDao.open()
        participanteDao = ParticipanteDAO(context)
        participanteDao.open()
    }

    @After
    fun tearDown() {
        grupoDao.limparTudo()
        participanteDao.close()
        grupoDao.close()
    }

    private fun criarGrupo(nome: String): Grupo {
        val g = Grupo().apply { this.nome = nome; data = "17/03/2026" }
        g.id = grupoDao.inserir(g).toInt()
        return g
    }

    private fun criarParticipante(nome: String, grupoId: Int): Participante {
        val p = Participante().apply { this.nome = nome }
        participanteDao.inserir(p, grupoId)
        return p
    }

    private fun buildWorker(): LembreteWorker {
        participanteDao.close()
        grupoDao.close()
        return TestListenableWorkerBuilder<LembreteWorker>(context).build()
    }

    private fun reabrirDaos() {
        grupoDao.open()
        participanteDao.open()
    }

    @Test
    fun doWork_banco_vazio_retorna_success_sem_notificacao() = runBlocking {
        val worker = buildWorker()
        val result = worker.doWork()
        reabrirDaos()

        assertEquals(ListenableWorker.Result.success(), result)
        val lembrete = notificationManager.activeNotifications
            .firstOrNull { it.id == NotificationHelper.NOTIFICACAO_LEMBRETE_ID }
        assertNull("Banco vazio — nenhuma notificação esperada", lembrete)
    }

    @Test
    fun doWork_grupo_com_menos_de_3_participantes_nao_notifica() = runBlocking {
        val g = criarGrupo("Pequeno")
        criarParticipante("A", g.id)
        criarParticipante("B", g.id)

        val worker = buildWorker()
        val result = worker.doWork()
        reabrirDaos()

        assertEquals(ListenableWorker.Result.success(), result)
        val lembrete = notificationManager.activeNotifications
            .firstOrNull { it.id == NotificationHelper.NOTIFICACAO_LEMBRETE_ID }
        assertNull("Menos de 3 participantes — sem notificação", lembrete)
    }

    @Test
    fun doWork_grupo_pendente_exibe_notificacao() = runBlocking {
        val g = criarGrupo("Família")
        criarParticipante("A", g.id)
        criarParticipante("B", g.id)
        criarParticipante("C", g.id)

        val worker = buildWorker()
        val result = worker.doWork()
        reabrirDaos()

        assertEquals(ListenableWorker.Result.success(), result)
        val lembrete = notificationManager.activeNotifications
            .firstOrNull { it.id == NotificationHelper.NOTIFICACAO_LEMBRETE_ID }
        assertNotNull("Grupo pendente — notificação esperada", lembrete)
    }

    @Test
    fun doWork_grupo_com_sorteio_realizado_nao_notifica() = runBlocking {
        val g = criarGrupo("Trabalho")
        val a = criarParticipante("A", g.id)
        val b = criarParticipante("B", g.id)
        criarParticipante("C", g.id)
        participanteDao.atualizarAmigoSorteado(a.id, b.id)

        val worker = buildWorker()
        val result = worker.doWork()
        reabrirDaos()

        assertEquals(ListenableWorker.Result.success(), result)
        val lembrete = notificationManager.activeNotifications
            .firstOrNull { it.id == NotificationHelper.NOTIFICACAO_LEMBRETE_ID }
        assertNull("Sorteio realizado — sem notificação", lembrete)
    }

    @Test
    fun doWork_sem_grupos_pendentes_cancela_agendamento() = runBlocking {
        // Nenhum grupo no banco → Worker deve cancelar o agendamento
        LembreteScheduler.agendar(context)

        val worker = buildWorker()
        worker.doWork()
        reabrirDaos()

        val trabalhos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(LembreteScheduler.NOME_TRABALHO)
            .get()
        val ativo = trabalhos.any {
            it.state == androidx.work.WorkInfo.State.ENQUEUED ||
                it.state == androidx.work.WorkInfo.State.RUNNING
        }
        assertEquals("Worker deve cancelar agendamento quando sem grupos pendentes", false, ativo)
    }
}
