package activity.amigosecreto

import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testa restoreParticipantListFromBundle em ParticipantesActivity.
 * Usa Robolectric para instanciar a Activity sem emulador.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RestoreParticipantBundleTest {

    private fun buildActivity(): ParticipantesActivity =
        Robolectric.buildActivity(ParticipantesActivity::class.java).get()

    // ---- Bug 2: arrays com tamanhos diferentes devem retornar null ----

    @Test
    fun restoreComTelefonesAMaiorQueIds_retornaNull() {
        val activity = buildActivity()
        val bundle = Bundle().apply {
            putIntArray("testIds", intArrayOf(1))
            putStringArray("testTelefones", arrayOf("11999", "99999")) // tamanho diferente
            putStringArray("testNomes", arrayOf("Alice"))
        }
        assertNull(activity.restoreParticipantListFromBundle(bundle, "test"))
    }

    @Test
    fun restoreComNomesAMenorQueIds_retornaNull() {
        val activity = buildActivity()
        val bundle = Bundle().apply {
            putIntArray("testIds", intArrayOf(1, 2))
            putStringArray("testTelefones", arrayOf("11999", "22999"))
            putStringArray("testNomes", arrayOf("Alice")) // tamanho diferente
        }
        assertNull(activity.restoreParticipantListFromBundle(bundle, "test"))
    }

    @Test
    fun restoreComArraysAusentesRetornaNull() {
        val activity = buildActivity()
        assertNull(activity.restoreParticipantListFromBundle(Bundle(), "test"))
    }

    // ---- Caminho feliz: arrays consistentes devem retornar a lista correta ----

    @Test
    fun restoreComArraysConsistentes_retornaListaCorreta() {
        val activity = buildActivity()
        val bundle = Bundle().apply {
            putIntArray("testIds", intArrayOf(10, 20))
            putStringArray("testTelefones", arrayOf("11999", "22999"))
            putStringArray("testNomes", arrayOf("Alice", "Bob"))
        }
        val result = activity.restoreParticipantListFromBundle(bundle, "test")!!
        assertEquals(2, result.size)
        assertEquals(10, result[0].id)
        assertEquals("Alice", result[0].nome)
        assertEquals("11999", result[0].telefone)
        assertEquals(20, result[1].id)
        assertEquals("Bob", result[1].nome)
    }
}
