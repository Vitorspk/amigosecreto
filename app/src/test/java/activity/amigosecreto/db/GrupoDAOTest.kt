package activity.amigosecreto.db

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GrupoDAOTest {

    private lateinit var dao: GrupoDAO
    private lateinit var participanteDao: ParticipanteDAO

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        dao = GrupoDAO(ctx)
        dao.open()
        participanteDao = ParticipanteDAO(ctx)
        participanteDao.open()
    }

    @After
    fun tearDown() {
        dao.limparTudo()
        dao.close()
        participanteDao.close()
    }

    private fun criarGrupo(nome: String): Grupo {
        val g = Grupo()
        g.nome = nome
        g.data = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val id = dao.inserir(g)
        g.id = id.toInt()
        return g
    }

    // --- inserir ---

    @Test
    fun inserir_retorna_id_valido() {
        val g = Grupo().apply { nome = "Familia"; data = "01/01/2025" }
        assertTrue(dao.inserir(g) > 0)
    }

    // --- listar ---

    @Test
    fun listar_retorna_lista_vazia_quando_sem_dados() {
        assertTrue(dao.listar().isEmpty())
    }

    @Test
    fun listar_retorna_grupo_inserido() {
        criarGrupo("Trabalho")
        val lista = dao.listar()
        assertEquals(1, lista.size)
        assertEquals("Trabalho", lista[0].nome)
    }

    @Test
    fun listar_ordem_desc_por_id() {
        criarGrupo("Primeiro")
        criarGrupo("Segundo")
        criarGrupo("Terceiro")
        val lista = dao.listar()
        assertEquals(3, lista.size)
        assertEquals("Terceiro", lista[0].nome)
        assertEquals("Primeiro", lista[2].nome)
    }

    // --- atualizarNome ---

    @Test
    fun atualizarNome_retorna_1_row_afetada() {
        val g = criarGrupo("Nome Antigo")
        g.nome = "Nome Novo"
        assertEquals(1, dao.atualizarNome(g))
    }

    @Test
    fun atualizarNome_persiste_novo_nome() {
        val g = criarGrupo("Nome Antigo")
        val dataOriginal = g.data
        g.nome = "Nome Novo"
        dao.atualizarNome(g)

        val lista = dao.listar()
        assertEquals(1, lista.size)
        assertEquals("Nome Novo", lista[0].nome)
        assertEquals(dataOriginal, lista[0].data)
    }

    @Test
    fun atualizarNome_id_inexistente_retorna_0() {
        val g = Grupo().apply { id = 999; nome = "Fantasma" }
        assertEquals(0, dao.atualizarNome(g))
    }

    // --- remover ---

    @Test
    fun remover_apaga_grupo() {
        val g = criarGrupo("Para Remover")
        dao.remover(g.id)
        assertTrue(dao.listar().isEmpty())
    }

    @Test
    fun remover_apaga_participantes_do_grupo() {
        val g = criarGrupo("Grupo Com Participantes")
        val p = Participante().apply { nome = "Membro" }
        participanteDao.inserir(p, g.id)

        dao.remover(g.id)

        assertTrue(participanteDao.listarPorGrupo(g.id).isEmpty())
    }

    @Test
    fun remover_nao_afeta_outros_grupos() {
        val g1 = criarGrupo("Grupo 1")
        criarGrupo("Grupo 2")
        dao.remover(g1.id)

        val lista = dao.listar()
        assertEquals(1, lista.size)
        assertEquals("Grupo 2", lista[0].nome)
    }

    // --- limparTudo ---

    @Test
    fun limparTudo_remove_todos_os_grupos() {
        criarGrupo("G1")
        criarGrupo("G2")
        dao.limparTudo()
        assertTrue(dao.listar().isEmpty())
    }

    @Test
    fun limparTudo_remove_participantes_tambem() {
        val g = criarGrupo("Grupo")
        val p = Participante().apply { nome = "Pessoa" }
        participanteDao.inserir(p, g.id)

        dao.limparTudo()

        assertTrue(participanteDao.listarPorGrupo(g.id).isEmpty())
    }
}