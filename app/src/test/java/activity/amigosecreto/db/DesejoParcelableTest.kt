package activity.amigosecreto.db

import android.os.Parcel
import android.os.Parcelable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes de Parcelable para Desejo.
 *
 * Garante que a serialização/desserialização via Parcel funciona corretamente
 * após migração para Kotlin (@Parcelize). Campos nulos devem ser propagados
 * como null no Parcel, não como a string "null".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DesejoParcelableTest {

    private fun buildDesejo() = Desejo().apply {
        id = 42
        produto = "Notebook"
        categoria = "Eletrônicos"
        lojas = "Amazon, Kabum"
        precoMinimo = 1500.0
        precoMaximo = 3000.0
        participanteId = 7
    }

    // Desejo.CREATOR não resolve em compile-time no contexto Robolectric porque é
    // gerado sinteticamente pelo plugin @Parcelize — acessível como campo estático de Java
    // mas não como membro Kotlin reconhecido pelo compilador. Reflexão é a alternativa segura.
    // Risco: se o nome do campo gerado mudar entre versões do plugin kotlinx.parcelize,
    // o teste falha em runtime com NoSuchFieldException (sem erro de compile-time).
    @Suppress("UNCHECKED_CAST")
    private fun creator(): Parcelable.Creator<Desejo> {
        val field = Desejo::class.java.getField("CREATOR")
        return field.get(null) as Parcelable.Creator<Desejo>
    }

    private fun parcelRoundTrip(original: Desejo): Desejo {
        val parcel = Parcel.obtain()
        return try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            creator().createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun parcel_categoriaNull_preservaNulo() {
        val d = buildDesejo().also { it.categoria = null }
        val restored = parcelRoundTrip(d)
        assertNotNull(restored)
        assertEquals(d.id, restored.id)
        assertEquals(d.produto, restored.produto)
        assertNull("categoria null deve ser restaurada como null", restored.categoria)
    }

    @Test
    fun parcel_lojasNull_preservaNulo() {
        val d = buildDesejo().also { it.lojas = null }
        val restored = parcelRoundTrip(d)
        assertNotNull(restored)
        assertEquals(d.id, restored.id)
        assertEquals(d.produto, restored.produto)
        assertNull("lojas null deve ser restaurada como null", restored.lojas)
    }

    @Test
    fun parcel_precosZero_preservados() {
        val d = buildDesejo().apply { precoMinimo = 0.0; precoMaximo = 0.0 }
        val restored = parcelRoundTrip(d)
        assertEquals(0.0, restored.precoMinimo, 0.001)
        assertEquals(0.0, restored.precoMaximo, 0.001)
    }

    @Test
    fun describeContents_returnsZero() {
        assertEquals(0, buildDesejo().describeContents())
    }

    @Test
    fun creator_newArray_correctSize() {
        val arr = creator().newArray(5)
        assertEquals(5, arr.size)
    }

    @Test
    fun parcel_roundTrip_equalsOriginal() {
        val original = buildDesejo()
        val restored = parcelRoundTrip(original)
        assertEquals(original, restored)
    }
}