package activity.amigosecreto.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
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
class NotificationHelperTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Concede permissão POST_NOTIFICATIONS para os testes
        Shadows.shadowOf(context as android.app.Application)
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun criarCanal_cria_canal_com_id_correto() {
        NotificationHelper.criarCanal(context)

        val canal = notificationManager.getNotificationChannel(NotificationHelper.CANAL_LEMBRETES)
        assertNotNull("Canal deve ser criado", canal)
    }

    @Test
    fun criarCanal_e_idempotente() {
        NotificationHelper.criarCanal(context)
        NotificationHelper.criarCanal(context) // segunda chamada não deve lançar exceção

        val canal = notificationManager.getNotificationChannel(NotificationHelper.CANAL_LEMBRETES)
        assertNotNull(canal)
    }

    @Test
    fun exibirLembreteSorteio_grupos_zero_nao_exibe_notificacao() {
        NotificationHelper.criarCanal(context)
        NotificationHelper.exibirLembreteSorteio(context, 0)

        // Com 0 grupos pendentes, nenhuma notificação deve ser postada
        val notificacoes = notificationManager.activeNotifications
        val lembrete = notificacoes.firstOrNull { it.id == NotificationHelper.NOTIFICACAO_LEMBRETE_ID }
        assertNull("Não deve exibir notificação para 0 grupos", lembrete)
    }

    @Test
    fun exibirLembreteSorteio_exibe_notificacao_para_grupos_pendentes() {
        NotificationHelper.criarCanal(context)
        NotificationHelper.exibirLembreteSorteio(context, 2)

        val notificacoes = notificationManager.activeNotifications
        val lembrete = notificacoes.firstOrNull { it.id == NotificationHelper.NOTIFICACAO_LEMBRETE_ID }
        assertNotNull("Deve exibir notificação para grupos pendentes", lembrete)
    }

    @Test
    fun exibirLembreteSorteio_grupos_negativos_nao_exibe_notificacao() {
        NotificationHelper.criarCanal(context)
        NotificationHelper.exibirLembreteSorteio(context, -1)

        val notificacoes = notificationManager.activeNotifications
        val lembrete = notificacoes.firstOrNull { it.id == NotificationHelper.NOTIFICACAO_LEMBRETE_ID }
        assertNull("Não deve exibir notificação para grupos negativos", lembrete)
    }

    @Test
    fun exibirLembreteSorteio_sem_permissao_nao_exibe_notificacao() {
        // Revoga a permissão
        Shadows.shadowOf(context as android.app.Application)
            .denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationHelper.criarCanal(context)
        NotificationHelper.exibirLembreteSorteio(context, 3)

        val notificacoes = notificationManager.activeNotifications
        val lembrete = notificacoes.firstOrNull { it.id == NotificationHelper.NOTIFICACAO_LEMBRETE_ID }
        assertNull("Não deve exibir notificação sem permissão", lembrete)
    }
}
