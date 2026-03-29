package activity.amigosecreto.adapter

import activity.amigosecreto.db.Participante
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ParticipantesRecyclerAdapterTest {

    private lateinit var adapter: ParticipantesRecyclerAdapter

    private fun criarParticipante(nome: String, enviado: Boolean = false): Participante {
        val p = Participante(nome = nome, telefone = "11999999999")
        p.isEnviado = enviado
        return p
    }

    @Before
    fun setUp() {
        adapter = ParticipantesRecyclerAdapter(
            ApplicationProvider.getApplicationContext(),
            emptyList()
        )
    }

    // =========================================================
    // getItemCount
    // =========================================================

    @Test
    fun getItemCount_retorna_zero_para_lista_vazia() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun getItemCount_retorna_tamanho_correto_da_lista() {
        val lista = listOf(
            criarParticipante("Ana"),
            criarParticipante("Bruno"),
            criarParticipante("Carlos")
        )
        adapter.updateList(lista)
        assertEquals(3, adapter.itemCount)
    }

    // =========================================================
    // updateList
    // =========================================================

    @Test
    fun updateList_com_lista_vazia_zera_contagem() {
        adapter.updateList(listOf(criarParticipante("Ana")))
        assertEquals(1, adapter.itemCount)

        adapter.updateList(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun updateList_substitui_lista_anterior() {
        adapter.updateList(listOf(criarParticipante("Ana"), criarParticipante("Bruno")))
        assertEquals(2, adapter.itemCount)

        adapter.updateList(listOf(criarParticipante("Carlos")))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun updateList_com_lista_grande_atualiza_corretamente() {
        val lista = (1..50).map { criarParticipante("Participante $it") }
        adapter.updateList(lista)
        assertEquals(50, adapter.itemCount)
    }

    // =========================================================
    // setOnItemClickListener
    // =========================================================

    @Test
    fun setOnItemClickListener_aceita_listener_sem_lancar_excecao() {
        // Verifica que atribuir um listener válido não lança exceção
        adapter.setOnItemClickListener(object : ParticipantesRecyclerAdapter.OnItemClickListener {
            override fun onItemClick(participante: Participante) {}
            override fun onRemoveClick(participante: Participante) {}
            override fun onShareClick(participante: Participante) {}
        })
        // Sucesso se não lançar exceção
    }

    // =========================================================
    // Interface OnItemClickListener
    // =========================================================

    @Test
    fun onItemClickListener_interface_tem_tres_metodos() {
        var itemClicked: Participante? = null
        var removeClicked: Participante? = null
        var shareClicked: Participante? = null

        val listener = object : ParticipantesRecyclerAdapter.OnItemClickListener {
            override fun onItemClick(participante: Participante) { itemClicked = participante }
            override fun onRemoveClick(participante: Participante) { removeClicked = participante }
            override fun onShareClick(participante: Participante) { shareClicked = participante }
        }

        val p = criarParticipante("Ana")
        listener.onItemClick(p)
        listener.onRemoveClick(p)
        listener.onShareClick(p)

        assertEquals(p, itemClicked)
        assertEquals(p, removeClicked)
        assertEquals(p, shareClicked)
    }
}
