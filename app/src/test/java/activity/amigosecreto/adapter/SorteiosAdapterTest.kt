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
 * formatarDataHora() é internal @VisibleForTesting — acessada diretamente
 * sem reflection, com benefício de rename safety automático pelo IDE.
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
        assertTrue(adapter.expandidos.isEmpty())
    }

    // =========================================================
    // formatarDataHora (internal @VisibleForTesting — acesso direto)
    // =========================================================

    @Test
    fun formatarDataHora_converte_iso8601_para_formato_pt_br() {
        assertEquals("25/12/2025 às 10:30", adapter.formatarDataHora("2025-12-25T10:30:00"))
    }

    @Test
    fun formatarDataHora_converte_data_inicio_do_ano() {
        assertEquals("01/01/2025 às 00:00", adapter.formatarDataHora("2025-01-01T00:00:00"))
    }

    @Test
    fun formatarDataHora_converte_data_fim_do_ano() {
        assertEquals("31/12/2025 às 23:59", adapter.formatarDataHora("2025-12-31T23:59:00"))
    }

    @Test
    fun formatarDataHora_converte_horario_com_horas_simples() {
        assertEquals("15/06/2024 às 09:05", adapter.formatarDataHora("2024-06-15T09:05:00"))
    }

    @Test
    fun formatarDataHora_retorna_original_quando_formato_invalido() {
        val invalido = "data-invalida"
        assertEquals(invalido, adapter.formatarDataHora(invalido))
    }

    @Test
    fun formatarDataHora_retorna_original_quando_string_vazia() {
        assertEquals("", adapter.formatarDataHora(""))
    }

    @Test
    fun formatarDataHora_retorna_original_para_formato_sem_T() {
        val original = "2025/12/25 10:30"
        assertEquals(original, adapter.formatarDataHora(original))
    }

    @Test
    fun formatarDataHora_contém_palavra_as_no_resultado() {
        val resultado = adapter.formatarDataHora("2025-07-20T14:00:00")
        assertTrue("Esperava 'às' em '$resultado'", resultado.contains("às"))
    }

    @Test
    fun formatarDataHora_formato_saida_corresponde_ao_padrao_dd_MM_yyyy() {
        val resultado = adapter.formatarDataHora("2025-03-08T08:00:00")
        assertTrue("Esperava formato dd/MM/yyyy 'às' HH:mm em '$resultado'",
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
