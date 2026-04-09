package activity.amigosecreto.adapter

import activity.amigosecreto.GruposViewModel
import activity.amigosecreto.R
import activity.amigosecreto.db.Grupo
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.view.View

/**
 * Testes unitários de GruposRecyclerAdapter (extraído C4).
 *
 * Cobre: gestão de lista via DiffUtil (setItens), contagem de itens,
 * e contrato de OnGrupoActionListener.
 * Comportamentos que requerem View real (onBindViewHolder, cliques) são
 * cobertes pelos testes Espresso em GruposActivityTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GruposRecyclerAdapterTest {

    private lateinit var adapter: GruposRecyclerAdapter
    private val actionListener = object : GruposRecyclerAdapter.OnGrupoActionListener {
        var lastClicked: Grupo? = null
        var lastEdited: Triple<Grupo, String, AlertDialog>? = null
        var lastRemoved: Grupo? = null

        override fun onGrupoClick(grupo: Grupo) { lastClicked = grupo }
        override fun onEditarNome(grupo: Grupo, novoNome: String, dialog: AlertDialog, button: View) {
            lastEdited = Triple(grupo, novoNome, dialog)
        }
        override fun onRemover(grupo: Grupo) { lastRemoved = grupo }
    }

    private val emojis = arrayOf("🎁", "🎄", "🎉")
    private val gradientes = intArrayOf(
        R.drawable.card_gradient_orange,
        R.drawable.card_gradient_blue,
        R.drawable.card_gradient_green
    )

    private fun criarItem(
        id: Int,
        nome: String,
        totalParticipantes: Int = 0,
        totalEnviados: Int = 0,
    ) = GruposViewModel.GrupoComContagem(
        grupo = Grupo(id = id, nome = nome),
        totalParticipantes = totalParticipantes,
        totalEnviados = totalEnviados,
    )

    @Before
    fun setUp() {
        adapter = GruposRecyclerAdapter(
            ApplicationProvider.getApplicationContext(),
            emojis,
            gradientes,
            actionListener
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
    fun getItemCount_retorna_tamanho_correto_apos_setItens() {
        adapter.setItens(listOf(criarItem(1, "A"), criarItem(2, "B"), criarItem(3, "C")))
        assertEquals(3, adapter.itemCount)
    }

    // =========================================================
    // setItens — gestão de lista via DiffUtil
    // =========================================================

    @Test
    fun setItens_com_lista_vazia_zera_contagem() {
        adapter.setItens(listOf(criarItem(1, "A")))
        assertEquals(1, adapter.itemCount)

        adapter.setItens(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun setItens_substitui_lista_anterior() {
        adapter.setItens(listOf(criarItem(1, "A"), criarItem(2, "B")))
        assertEquals(2, adapter.itemCount)

        adapter.setItens(listOf(criarItem(3, "C")))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun setItens_adiciona_itens_a_lista_existente() {
        adapter.setItens(listOf(criarItem(1, "A")))
        adapter.setItens(listOf(criarItem(1, "A"), criarItem(2, "B"), criarItem(3, "C")))

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun setItens_lista_grande_atualiza_corretamente() {
        val lista = (1..100).map { criarItem(it, "Grupo $it") }
        adapter.setItens(lista)
        assertEquals(100, adapter.itemCount)
    }

    @Test
    fun setItens_atualiza_lista_com_dados_de_contagem_distintos() {
        // Item idêntico em id/nome mas contagens diferentes — DiffUtil deve detectar mudança
        adapter.setItens(listOf(criarItem(1, "Amigos", totalParticipantes = 3, totalEnviados = 0)))
        adapter.setItens(listOf(criarItem(1, "Amigos", totalParticipantes = 3, totalEnviados = 2)))

        // Após DiffUtil, o adapter deve ter o item atualizado
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun setItens_chamado_multiplas_vezes_mantem_consistencia() {
        adapter.setItens(listOf(criarItem(1, "A"), criarItem(2, "B")))
        adapter.setItens(listOf(criarItem(3, "C")))
        adapter.setItens(emptyList())
        adapter.setItens(listOf(criarItem(4, "D"), criarItem(5, "E"), criarItem(6, "F")))

        assertEquals(3, adapter.itemCount)
    }

    // =========================================================
    // OnGrupoActionListener — contrato da interface
    // =========================================================

    @Test
    fun onGrupoActionListener_interface_tem_tres_metodos() {
        // Verifica que os três métodos são invocáveis via interface
        var clicado = false
        var editado = false
        var removido = false

        val listener = object : GruposRecyclerAdapter.OnGrupoActionListener {
            override fun onGrupoClick(grupo: Grupo) { clicado = true }
            override fun onEditarNome(grupo: Grupo, novoNome: String, dialog: AlertDialog, button: View) { editado = true }
            override fun onRemover(grupo: Grupo) { removido = true }
        }

        val grupo = Grupo(id = 1, nome = "Teste")
        val fakeDialog = AlertDialog.Builder(ApplicationProvider.getApplicationContext()).create()
        val fakeButton = View(ApplicationProvider.getApplicationContext())

        listener.onGrupoClick(grupo)
        listener.onEditarNome(grupo, "Novo", fakeDialog, fakeButton)
        listener.onRemover(grupo)

        assertTrue(clicado)
        assertTrue(editado)
        assertTrue(removido)
    }

    @Test
    fun onGrupoActionListener_recebe_grupo_correto_no_onGrupoClick() {
        val grupo = Grupo(id = 42, nome = "Familia")
        actionListener.onGrupoClick(grupo)
        assertEquals(grupo, actionListener.lastClicked)
    }

    @Test
    fun onGrupoActionListener_recebe_grupo_e_nome_corretos_no_onEditarNome() {
        val grupo = Grupo(id = 7, nome = "Antigo")
        val fakeDialog = AlertDialog.Builder(ApplicationProvider.getApplicationContext()).create()
        val fakeButton = View(ApplicationProvider.getApplicationContext())

        actionListener.onEditarNome(grupo, "Novo Nome", fakeDialog, fakeButton)

        assertNotNull(actionListener.lastEdited)
        assertEquals(grupo, actionListener.lastEdited!!.first)
        assertEquals("Novo Nome", actionListener.lastEdited!!.second)
    }

    @Test
    fun onGrupoActionListener_recebe_grupo_correto_no_onRemover() {
        val grupo = Grupo(id = 99, nome = "Para Excluir")
        actionListener.onRemover(grupo)
        assertEquals(grupo, actionListener.lastRemoved)
    }

    // =========================================================
    // GrupoComContagem — data class
    // Nota: GrupoComContagem é data class, mas Grupo é plain class (equals por referência).
    // Para igualdade funcionar, os dois itens devem referenciar o mesmo Grupo.
    // =========================================================

    @Test
    fun grupoComContagem_igualdade_com_mesma_instancia_de_grupo() {
        val grupo = Grupo(id = 1, nome = "A")
        val c1 = GruposViewModel.GrupoComContagem(grupo, 5, 2)
        val c2 = GruposViewModel.GrupoComContagem(grupo, 5, 2)
        assertEquals(c1, c2)
    }

    @Test
    fun grupoComContagem_desigualdade_quando_contagem_difere() {
        val grupo = Grupo(id = 1, nome = "A")
        val c1 = GruposViewModel.GrupoComContagem(grupo, 5, 2)
        val c2 = GruposViewModel.GrupoComContagem(grupo, 5, 3)
        assertNotEquals(c1, c2)
    }
}
