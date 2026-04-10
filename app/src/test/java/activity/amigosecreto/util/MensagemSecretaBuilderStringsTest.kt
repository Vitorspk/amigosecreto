package activity.amigosecreto.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica paridade entre MensagemSecretaBuilder.Strings.ptBr() (usado em testes unitários)
 * e Strings.from(Context) (lê de strings.xml localizado — usado em produção).
 *
 * Se strings.xml for atualizado sem atualizar ptBr(), os 24 testes unitários existentes
 * continuariam passando com os valores antigos — este teste pega a divergência.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "b+pt+BR")
class MensagemSecretaBuilderStringsTest {

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun ptBr_greeting_matchesStringsXml() {
        assertEquals(
            MensagemSecretaBuilder.Strings.ptBr().greeting,
            MensagemSecretaBuilder.Strings.from(ctx).greeting
        )
    }

    @Test
    fun ptBr_intro_matchesStringsXml() {
        assertEquals(
            MensagemSecretaBuilder.Strings.ptBr().intro,
            MensagemSecretaBuilder.Strings.from(ctx).intro
        )
    }

    @Test
    fun ptBr_amigoLabel_matchesStringsXml() {
        assertEquals(
            MensagemSecretaBuilder.Strings.ptBr().amigoLabel,
            MensagemSecretaBuilder.Strings.from(ctx).amigoLabel
        )
    }

    @Test
    fun ptBr_wishlistHeader_matchesStringsXml() {
        assertEquals(
            MensagemSecretaBuilder.Strings.ptBr().wishlistHeader,
            MensagemSecretaBuilder.Strings.from(ctx).wishlistHeader
        )
    }

    @Test
    fun ptBr_farewell_matchesStringsXml() {
        assertEquals(
            MensagemSecretaBuilder.Strings.ptBr().farewell,
            MensagemSecretaBuilder.Strings.from(ctx).farewell
        )
    }

    @Test
    fun ptBr_priceRange_matchesStringsXml() {
        assertEquals(
            MensagemSecretaBuilder.Strings.ptBr().priceRange,
            MensagemSecretaBuilder.Strings.from(ctx).priceRange
        )
    }

    @Test
    fun ptBr_priceUpTo_matchesStringsXml() {
        assertEquals(
            MensagemSecretaBuilder.Strings.ptBr().priceUpTo,
            MensagemSecretaBuilder.Strings.from(ctx).priceUpTo
        )
    }

    @Test
    fun ptBr_priceFrom_matchesStringsXml() {
        assertEquals(
            MensagemSecretaBuilder.Strings.ptBr().priceFrom,
            MensagemSecretaBuilder.Strings.from(ctx).priceFrom
        )
    }
}
