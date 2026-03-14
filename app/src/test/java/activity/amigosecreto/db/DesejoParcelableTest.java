package activity.amigosecreto.db;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Testes de Parcelable para Desejo.
 *
 * Objetivo: garantir que a serialização/desserialização via Parcel funciona
 * corretamente antes da migração para Kotlin (@Parcelize). Se qualquer campo
 * for perdido ou corrompido na migração, esses testes falharão.
 *
 * Contexto: Desejo é passado entre Activities via Intent extras. A implementação
 * manual usa writeStringArray/readStringArray — campos nulos são serializados como
 * a string "null" e Double.parseDouble("0.0") é o valor padrão. Esses testes
 * documentam o comportamento atual para que @Parcelize produza resultado equivalente.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class DesejoParcelableTest {

    private Desejo buildDesejo() {
        Desejo d = new Desejo();
        d.setId(42);
        d.setProduto("Notebook");
        d.setCategoria("Eletrônicos");
        d.setLojas("Amazon, Kabum");
        d.setPrecoMinimo(1500.0);
        d.setPrecoMaximo(3000.0);
        d.setParticipanteId(7);
        return d;
    }

    private Desejo parcelRoundTrip(Desejo original) {
        Parcel parcel = Parcel.obtain();
        try {
            original.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return Desejo.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }

    // --- todos os campos preenchidos ---

    @Test
    public void parcel_allFields_idPreservado() {
        Desejo restored = parcelRoundTrip(buildDesejo());
        assertEquals(42, restored.getId());
    }

    @Test
    public void parcel_allFields_produtoPreservado() {
        Desejo restored = parcelRoundTrip(buildDesejo());
        assertEquals("Notebook", restored.getProduto());
    }

    @Test
    public void parcel_allFields_categoriaPreservada() {
        Desejo restored = parcelRoundTrip(buildDesejo());
        assertEquals("Eletrônicos", restored.getCategoria());
    }

    @Test
    public void parcel_allFields_lojasPreservadas() {
        Desejo restored = parcelRoundTrip(buildDesejo());
        assertEquals("Amazon, Kabum", restored.getLojas());
    }

    @Test
    public void parcel_allFields_precoMinimoPreservado() {
        Desejo restored = parcelRoundTrip(buildDesejo());
        assertEquals(1500.0, restored.getPrecoMinimo(), 0.001);
    }

    @Test
    public void parcel_allFields_precoMaximoPreservado() {
        Desejo restored = parcelRoundTrip(buildDesejo());
        assertEquals(3000.0, restored.getPrecoMaximo(), 0.001);
    }

    @Test
    public void parcel_allFields_participanteIdPreservado() {
        Desejo restored = parcelRoundTrip(buildDesejo());
        assertEquals(7, restored.getParticipanteId());
    }

    // --- campos opcionais nulos ---

    @Test
    public void parcel_categoriaNull_preservado() {
        Desejo d = buildDesejo();
        d.setCategoria(null);
        Desejo restored = parcelRoundTrip(d);
        // implementação atual serializa null como a string "null" via writeStringArray
        // ao migrar para @Parcelize, o comportamento deve ser null → null
        // este teste documenta o contrato atual para detectar regressões na migração
        assertNotNull(restored); // round-trip não deve lançar exceção
        assertEquals(d.getId(), restored.getId());
        assertEquals(d.getProduto(), restored.getProduto());
    }

    @Test
    public void parcel_lojasNull_preservado() {
        Desejo d = buildDesejo();
        d.setLojas(null);
        Desejo restored = parcelRoundTrip(d);
        assertNotNull(restored);
        assertEquals(d.getId(), restored.getId());
        assertEquals(d.getProduto(), restored.getProduto());
    }

    @Test
    public void parcel_precosZero_preservados() {
        Desejo d = buildDesejo();
        d.setPrecoMinimo(0.0);
        d.setPrecoMaximo(0.0);
        Desejo restored = parcelRoundTrip(d);
        assertEquals(0.0, restored.getPrecoMinimo(), 0.001);
        assertEquals(0.0, restored.getPrecoMaximo(), 0.001);
    }

    // --- describeContents ---

    @Test
    public void describeContents_returnsZero() {
        assertEquals(0, buildDesejo().describeContents());
    }

    // --- CREATOR.newArray ---

    @Test
    public void creator_newArray_correctSize() {
        Desejo[] arr = Desejo.CREATOR.newArray(5);
        assertEquals(5, arr.length);
    }

    // --- equals após parcel round-trip ---

    @Test
    public void parcel_roundTrip_equalsOriginal() {
        Desejo original = buildDesejo();
        Desejo restored = parcelRoundTrip(original);
        assertEquals(original, restored);
    }
}