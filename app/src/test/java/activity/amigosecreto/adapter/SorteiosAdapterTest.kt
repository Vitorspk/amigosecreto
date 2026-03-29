package activity.amigosecreto.adapter

import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.db.SorteioPar
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes unitários de SorteiosAdapter via Robolectric.
 *
 * Como formatarDataHora() é private, sua lógica é coberta indiretamente
 * via reflexão para garantir a conversão ISO 8601 → dd/MM/yyyy 'às' HH:mm.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SorteiosAdapterTest {

    private lateinit var adapter: SorteiosAdapter

    private fun criarSorteio(
        id: Int = 1,
        grupoId: Int = 1,
        dataHora: String = "2025-12-25T10:30:00",
        numPares: Int = 0
    ): Sorteio {
        val s = Sorteio(id = id, grupoId = grupoId, dataHora = dataHora)
        if (numPares > 0) {
            s.pares = (1..numPares).map { i ->
                SorteioPar(
                    sorteioId = id,
                    participanteId = i,
                    sorteadoId = i + 1,
                    nomeParticipante = "P$i",
                    nomeSorteado = "P${i + 1}"
                )
            }
        }
        return s
    }

    @Before
    fun setUp() {
        adapter = SorteiosAdapter(
            ApplicationProvider.getApplicationContext(),
            emptyList()
        )
    }

    // =========================================================
    // getItemCount
    // =========================================================

    @Test
    fun getItemCount_lista_vazia_retorna_zero() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun getItemCount_lista_com_um_item_retorna_um() {
        adapter = SorteiosAdapter(
            ApplicationProvider.getApplicationContext(),
            listOf(criarSorteio())
        )
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun getItemCount_lista_com_tres_itens_retorna_tres() {
        val lista = listOf(
            criarSorteio(id = 1),
            criarSorteio(id = 2),
            criarSorteio(id = 3)
        )
        adapter = SorteiosAdapter(ApplicationProvider.getApplicationContext(), lista)
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun getItemCount_lista_grande_retorna_tamanho_correto() {
        val lista = (1..50).map { criarSorteio(id = it) }
        adapter = SorteiosAdapter(ApplicationProvider.getApplicationContext(), lista)
        assertEquals(50, adapter.itemCount)
    }

    // =========================================================
    // expand/collapse state (expandidos set)
    // =========================================================

    @Test
    fun expandidos_inicialmente_vazio_nenhum_item_expandido() {
        val lista = listOf(criarSorteio(id = 1), criarSorteio(id = 2))
        adapter = SorteiosAdapter(ApplicationProvider.getApplicationContext(), lista)

        val expandidosField = SorteiosAdapter::class.java.getDeclaredField("expandidos")
        expandidosField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val expandidos = expandidosField.get(adapter) as MutableSet<*>
        assertTrue(expandidos.isEmpty())
    }

    // =========================================================
    // formatarDataHora (via reflexão — private)
    // =========================================================

    private fun formatarDataHora(dataHora: String): String {
        val method = SorteiosAdapter::class.java.getDeclaredMethod("formatarDataHora", String::class.java)
        method.isAccessible = true
        return method.invoke(adapter, dataHora) as String
    }

    @Test
    fun formatarDataHora_converte_iso8601_para_formato_pt_br() {
        val resultado = formatarDataHora("2025-12-25T10:30:00")
        assertEquals("25/12/2025 às 10:30", resultado)
    }

    @Test
    fun formatarDataHora_converte_data_inicio_do_ano() {
        val resultado = formatarDataHora("2025-01-01T00:00:00")
        assertEquals("01/01/2025 às 00:00", resultado)
    }

    @Test
    fun formatarDataHora_converte_data_fim_do_ano() {
        val resultado = formatarDataHora("2025-12-31T23:59:00")
        assertEquals("31/12/2025 às 23:59", resultado)
    }

    @Test
    fun formatarDataHora_converte_horario_com_horas_simples() {
        val resultado = formatarDataHora("2024-06-15T09:05:00")
        assertEquals("15/06/2024 às 09:05", resultado)
    }

    @Test
    fun formatarDataHora_retorna_original_quando_formato_invalido() {
        val invalido = "data-invalida"
        val resultado = formatarDataHora(invalido)
        assertEquals(invalido, resultado)
    }

    @Test
    fun formatarDataHora_retorna_original_quando_string_vazia() {
        val resultado = formatarDataHora("")
        assertEquals("", resultado)
    }

    @Test
    fun formatarDataHora_retorna_original_para_formato_americano_sem_T() {
        val original = "2025/12/25 10:30"
        val resultado = formatarDataHora(original)
        assertEquals(original, resultado)
    }

    @Test
    fun formatarDataHora_contém_palavra_as_no_resultado() {
        val resultado = formatarDataHora("2025-07-20T14:00:00")
        assertTrue("Esperava 'às' em '$resultado'", resultado.contains("às"))
    }

    @Test
    fun formatarDataHora_formato_saida_tem_dd_MM_yyyy() {
        val resultado = formatarDataHora("2025-03-08T08:00:00")
        // Formato esperado: dd/MM/yyyy 'às' HH:mm → "08/03/2025 às 08:00"
        assertTrue("Esperava formato dd/MM/yyyy em '$resultado'",
            resultado.matches(Regex("\\d{2}/\\d{2}/\\d{4} às \\d{2}:\\d{2}")))
    }

    // =========================================================
    // Construção com sorteios que têm pares
    // =========================================================

    @Test
    fun adapter_com_sorteio_com_pares_mantem_tamanho_correto() {
        val s = criarSorteio(id = 1, numPares = 3)
        assertEquals(3, s.pares.size)

        adapter = SorteiosAdapter(ApplicationProvider.getApplicationContext(), listOf(s))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun adapter_com_multiplos_sorteios_com_pares_mantem_contagem() {
        val lista = listOf(
            criarSorteio(id = 1, numPares = 3),
            criarSorteio(id = 2, numPares = 5),
            criarSorteio(id = 3, numPares = 2)
        )
        adapter = SorteiosAdapter(ApplicationProvider.getApplicationContext(), lista)
        assertEquals(3, adapter.itemCount)
    }
}
