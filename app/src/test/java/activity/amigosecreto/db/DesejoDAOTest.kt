package activity.amigosecreto.db

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DesejoDAOTest {

    private lateinit var dao: DesejoDAO
    private lateinit var participanteDao: ParticipanteDAO
    private lateinit var grupoDao: GrupoDAO
    private var grupoId = 0

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        dao = DesejoDAO(ctx)
        dao.open()
        participanteDao = ParticipanteDAO(ctx)
        participanteDao.open()
        grupoDao = GrupoDAO(ctx)
        grupoDao.open()

        val g = Grupo().apply { nome = "Grupo Teste" }
        grupoId = grupoDao.inserir(g).toInt()
    }

    @After
    fun tearDown() {
        grupoDao.limparTudo()
        grupoDao.close()
        participanteDao.close()
        dao.close()
    }

    private fun criarParticipante(nome: String): Participante {
        val p = Participante().apply { this.nome = nome }
        participanteDao.inserir(p, grupoId)
        assertTrue("ParticipanteDAO.inserir deve atribuir ID > 0", p.id > 0)
        return p
    }

    private fun buildDesejo(produto: String, participanteId: Int) = Desejo().apply {
        this.produto = produto
        this.categoria = "Eletrônicos"
        this.lojas = "Amazon"
        this.precoMinimo = 100.0
        this.precoMaximo = 200.0
        this.participanteId = participanteId
    }

    // --- inserir ---

    @Test
    fun inserir_assignsId() {
        val p = criarParticipante("Ana")
        val d = buildDesejo("Fone", p.id)
        dao.inserir(d)
        assertTrue("ID deve ser > 0 após inserir", d.id > 0)
    }

    @Test
    fun inserir_persists_listar() {
        val p = criarParticipante("Bruno")
        dao.inserir(buildDesejo("Teclado", p.id))
        assertTrue(dao.listar().any { it.produto == "Teclado" })
    }

    @Test
    fun inserir_multiple_allPersisted() {
        val p = criarParticipante("Carla")
        dao.inserir(buildDesejo("Mouse", p.id))
        dao.inserir(buildDesejo("Monitor", p.id))
        dao.inserir(buildDesejo("Webcam", p.id))
        assertEquals(3, dao.listarPorParticipante(p.id).size)
    }

    @Test
    fun inserir_failurePath_idRemainsZero() {
        val p = criarParticipante("Zara")
        val d = Desejo().apply { produto = null; participanteId = p.id }
        dao.inserir(d)
        assertEquals("ID deve permanecer 0 quando inserção falha", 0, d.id)
    }

    // --- listar ---

    @Test
    fun listar_emptyDatabase_returnsEmptyList() {
        val lista = dao.listar()
        assertNotNull(lista)
        assertTrue(lista.isEmpty())
    }

    @Test
    fun listar_retorna_todos_os_desejos() {
        val p1 = criarParticipante("Diana")
        val p2 = criarParticipante("Eduardo")
        dao.inserir(buildDesejo("Ítema", p1.id))
        dao.inserir(buildDesejo("ItemB", p2.id))
        assertEquals(2, dao.listar().size)
    }

    // --- listarPorParticipante ---

    @Test
    fun listarPorParticipante_returnsOnlyParticipantDesejos() {
        val p1 = criarParticipante("Felipe")
        val p2 = criarParticipante("Gabriela")
        dao.inserir(buildDesejo("Anel", p1.id))
        dao.inserir(buildDesejo("Bolsa", p2.id))

        val lista = dao.listarPorParticipante(p1.id)
        assertEquals(1, lista.size)
        assertEquals("Anel", lista[0].produto)
    }

    @Test
    fun listarPorParticipante_noDesejos_returnsEmpty() {
        val p = criarParticipante("Hugo")
        assertTrue(dao.listarPorParticipante(p.id).isEmpty())
    }

    // --- contarDesejosPorParticipante ---

    @Test
    fun contarDesejosPorParticipante_retornaContagemCorreta() {
        val p = criarParticipante("Ines")
        dao.inserir(buildDesejo("X", p.id))
        dao.inserir(buildDesejo("Y", p.id))
        assertEquals(2, dao.contarDesejosPorParticipante(p.id))
    }

    @Test
    fun contarDesejosPorParticipante_noDesejo_returnsZero() {
        val p = criarParticipante("Iris")
        assertEquals(0, dao.contarDesejosPorParticipante(p.id))
    }

    // --- buscarPorId ---

    @Test
    fun buscarPorId_existingId_returnsDesejo() {
        val p = criarParticipante("João")
        val d = buildDesejo("Câmera", p.id)
        dao.inserir(d)

        val found = dao.buscarPorId(d.id)
        assertNotNull(found)
        assertEquals("Câmera", found!!.produto)
        assertEquals("Eletrônicos", found.categoria)
        assertEquals(100.0, found.precoMinimo, 0.001)
        assertEquals(200.0, found.precoMaximo, 0.001)
        assertEquals("Amazon", found.lojas)
        assertEquals(p.id, found.participanteId)
    }

    @Test
    fun buscarPorId_nonExistingId_returnsNull() {
        assertNull(dao.buscarPorId(99999))
    }

    // --- alterar ---

    @Test
    fun alterar_updatesProduto() {
        val p = criarParticipante("Karen")
        val original = buildDesejo("Impressora", p.id)
        dao.inserir(original)

        val paraAtualizar = dao.buscarPorId(original.id)!!
        paraAtualizar.produto = "Impressora 3D"
        dao.alterar(original, paraAtualizar)

        val found = dao.buscarPorId(original.id)!!
        assertEquals("Impressora 3D", found.produto)
        assertEquals(paraAtualizar.categoria, found.categoria)
        assertEquals(paraAtualizar.lojas, found.lojas)
        assertEquals(paraAtualizar.precoMinimo, found.precoMinimo, 0.001)
        assertEquals(paraAtualizar.precoMaximo, found.precoMaximo, 0.001)
    }

    @Test
    fun alterar_updatesAllFields() {
        val p = criarParticipante("Lucas")
        val original = buildDesejo("Produto X", p.id)
        dao.inserir(original)

        val updated = Desejo().apply {
            id = original.id
            produto = "Produto Y"
            categoria = "Games"
            lojas = "Nuuvem"
            precoMinimo = 50.0
            precoMaximo = 150.0
            participanteId = p.id
        }
        dao.alterar(original, updated)

        val found = dao.buscarPorId(original.id)!!
        assertEquals("Produto Y", found.produto)
        assertEquals("Games", found.categoria)
        assertEquals("Nuuvem", found.lojas)
        assertEquals(50.0, found.precoMinimo, 0.001)
        assertEquals(150.0, found.precoMaximo, 0.001)
        assertEquals(p.id, found.participanteId)
    }

    // --- remover ---

    @Test
    fun remover_deletesFromDatabase() {
        val p = criarParticipante("Maria")
        val d = buildDesejo("Perfume", p.id)
        dao.inserir(d)
        dao.remover(d)
        assertNull(dao.buscarPorId(d.id))
    }

    @Test
    fun remover_onlyRemovesTarget() {
        val p = criarParticipante("Nadia")
        val d1 = buildDesejo("Bolsa", p.id)
        val d2 = buildDesejo("Carteira", p.id)
        dao.inserir(d1)
        dao.inserir(d2)
        dao.remover(d1)
        assertNull(dao.buscarPorId(d1.id))
        assertNotNull(dao.buscarPorId(d2.id))
    }

    // --- alterar edge cases ---

    @Test
    fun alterar_nonExistentId_isNoOp() {
        val p = criarParticipante("Paulo")
        val existing = buildDesejo("Livro", p.id)
        dao.inserir(existing)

        val ghost = buildDesejo("Fantasma", p.id).also { it.id = 99999 }
        val updated = buildDesejo("Fantasma Atualizado", p.id).also { it.id = 99999 }
        dao.alterar(ghost, updated)

        assertEquals("Livro", dao.buscarPorId(existing.id)!!.produto)
    }

    // --- remover edge cases ---

    @Test
    fun remover_nonExistentId_isNoOp() {
        val p = criarParticipante("Quesia")
        val existing = buildDesejo("Caneta", p.id)
        dao.inserir(existing)

        dao.remover(Desejo().also { it.id = 99999 })

        assertNotNull(dao.buscarPorId(existing.id))
    }

}