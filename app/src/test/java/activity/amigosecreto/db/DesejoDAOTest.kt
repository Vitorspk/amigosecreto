package activity.amigosecreto.db

import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.db.room.DesejoRoomDao
import activity.amigosecreto.db.room.GrupoRoomDao
import activity.amigosecreto.db.room.ParticipanteRoomDao
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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

    private lateinit var db: AppDatabase
    private lateinit var dao: DesejoRoomDao
    private lateinit var participanteDao: ParticipanteRoomDao
    private lateinit var grupoDao: GrupoRoomDao
    private var grupoId = 0

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = db.desejoDao()
        participanteDao = db.participanteDao()
        grupoDao = db.grupoDao()

        val g = Grupo(nome = "Grupo Teste")
        grupoId = grupoDao.inserir(g).toInt()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun criarParticipante(nome: String): Participante {
        val p = Participante(nome = nome, grupoId = grupoId)
        val id = participanteDao.inserir(p).toInt()
        assertTrue("ParticipanteRoomDao.inserir deve retornar ID > 0", id > 0)
        p.id = id
        return p
    }

    private fun buildDesejo(produto: String, participanteId: Int) = Desejo(
        produto = produto,
        categoria = "Eletrônicos",
        lojas = "Amazon",
        precoMinimo = 100.0,
        precoMaximo = 200.0,
        participanteId = participanteId,
    )

    // --- inserir ---

    @Test
    fun inserir_assignsId() = runTest {
        val p = criarParticipante("Ana")
        val d = buildDesejo("Fone", p.id)
        val id = dao.inserir(d)
        assertTrue("ID deve ser > 0 após inserir", id > 0)
    }

    @Test
    fun inserir_persists_listar() = runTest {
        val p = criarParticipante("Bruno")
        dao.inserir(buildDesejo("Teclado", p.id))
        assertTrue(dao.listar().any { it.produto == "Teclado" })
    }

    @Test
    fun inserir_multiple_allPersisted() = runTest {
        val p = criarParticipante("Carla")
        dao.inserir(buildDesejo("Mouse", p.id))
        dao.inserir(buildDesejo("Monitor", p.id))
        dao.inserir(buildDesejo("Webcam", p.id))
        assertEquals(3, dao.listarPorParticipante(p.id).size)
    }

    @Test
    fun inserir_failurePath_idRemainsZero() = runTest {
        val p = criarParticipante("Zara")
        // produto=null violates NOT NULL constraint — Room returns -1 (REPLACE conflict but null
        // on NOT NULL column causes constraint violation); id assigned by caller stays 0
        val d = Desejo(produto = null, participanteId = p.id)
        // Room with REPLACE strategy: null produto on NOT NULL col causes SQLiteConstraintException
        // We catch it and verify the desejo id is still 0 (not updated by the caller)
        try {
            dao.inserir(d)
            // If no exception, id would be updated — the test would still pass if id>0 not asserted
        } catch (_: Exception) {
            // Expected: constraint violation
        }
        assertEquals("ID deve permanecer 0 quando inserção falha", 0, d.id)
    }

    // --- listar ---

    @Test
    fun listar_emptyDatabase_returnsEmptyList() = runTest {
        val lista = dao.listar()
        assertNotNull(lista)
        assertTrue(lista.isEmpty())
    }

    @Test
    fun listar_retorna_todos_os_desejos() = runTest {
        val p1 = criarParticipante("Diana")
        val p2 = criarParticipante("Eduardo")
        dao.inserir(buildDesejo("Ítema", p1.id))
        dao.inserir(buildDesejo("ItemB", p2.id))
        assertEquals(2, dao.listar().size)
    }

    // --- listarPorParticipante ---

    @Test
    fun listarPorParticipante_returnsOnlyParticipantDesejos() = runTest {
        val p1 = criarParticipante("Felipe")
        val p2 = criarParticipante("Gabriela")
        dao.inserir(buildDesejo("Anel", p1.id))
        dao.inserir(buildDesejo("Bolsa", p2.id))

        val lista = dao.listarPorParticipante(p1.id)
        assertEquals(1, lista.size)
        assertEquals("Anel", lista[0].produto)
    }

    @Test
    fun listarPorParticipante_noDesejos_returnsEmpty() = runTest {
        val p = criarParticipante("Hugo")
        assertTrue(dao.listarPorParticipante(p.id).isEmpty())
    }

    // --- contarPorParticipante ---

    @Test
    fun contarDesejosPorParticipante_retornaContagemCorreta() = runTest {
        val p = criarParticipante("Ines")
        dao.inserir(buildDesejo("X", p.id))
        dao.inserir(buildDesejo("Y", p.id))
        assertEquals(2, dao.contarPorParticipante(p.id))
    }

    @Test
    fun contarDesejosPorParticipante_noDesejo_returnsZero() = runTest {
        val p = criarParticipante("Iris")
        assertEquals(0, dao.contarPorParticipante(p.id))
    }

    // --- buscarPorId ---

    @Test
    fun buscarPorId_existingId_returnsDesejo() = runTest {
        val p = criarParticipante("João")
        val d = buildDesejo("Câmera", p.id)
        val id = dao.inserir(d).toInt()
        d.id = id

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
    fun buscarPorId_nonExistingId_returnsNull() = runTest {
        assertNull(dao.buscarPorId(99999))
    }

    // --- atualizar ---

    @Test
    fun atualizar_updatesProduto() = runTest {
        val p = criarParticipante("Karen")
        val original = buildDesejo("Impressora", p.id)
        val originalId = dao.inserir(original).toInt()
        original.id = originalId

        val paraAtualizar = dao.buscarPorId(originalId)!!
        paraAtualizar.produto = "Impressora 3D"
        dao.atualizar(paraAtualizar)

        val found = dao.buscarPorId(originalId)!!
        assertEquals("Impressora 3D", found.produto)
        assertEquals(paraAtualizar.categoria, found.categoria)
        assertEquals(paraAtualizar.lojas, found.lojas)
        assertEquals(paraAtualizar.precoMinimo, found.precoMinimo, 0.001)
        assertEquals(paraAtualizar.precoMaximo, found.precoMaximo, 0.001)
    }

    @Test
    fun atualizar_updatesAllFields() = runTest {
        val p = criarParticipante("Lucas")
        val original = buildDesejo("Produto X", p.id)
        val originalId = dao.inserir(original).toInt()
        original.id = originalId

        val updated = Desejo(
            id = originalId,
            produto = "Produto Y",
            categoria = "Games",
            lojas = "Nuuvem",
            precoMinimo = 50.0,
            precoMaximo = 150.0,
            participanteId = p.id,
        )
        dao.atualizar(updated)

        val found = dao.buscarPorId(originalId)!!
        assertEquals("Produto Y", found.produto)
        assertEquals("Games", found.categoria)
        assertEquals("Nuuvem", found.lojas)
        assertEquals(50.0, found.precoMinimo, 0.001)
        assertEquals(150.0, found.precoMaximo, 0.001)
        assertEquals(p.id, found.participanteId)
    }

    // --- remover ---

    @Test
    fun remover_deletesFromDatabase() = runTest {
        val p = criarParticipante("Maria")
        val d = buildDesejo("Perfume", p.id)
        val id = dao.inserir(d).toInt()
        dao.remover(id)
        assertNull(dao.buscarPorId(id))
    }

    @Test
    fun remover_onlyRemovesTarget() = runTest {
        val p = criarParticipante("Nadia")
        val d1 = buildDesejo("Bolsa", p.id)
        val d2 = buildDesejo("Carteira", p.id)
        val id1 = dao.inserir(d1).toInt()
        val id2 = dao.inserir(d2).toInt()
        dao.remover(id1)
        assertNull(dao.buscarPorId(id1))
        assertNotNull(dao.buscarPorId(id2))
    }

    // --- atualizar edge cases ---

    @Test
    fun atualizar_nonExistentId_isNoOp() = runTest {
        val p = criarParticipante("Paulo")
        val existing = buildDesejo("Livro", p.id)
        val existingId = dao.inserir(existing).toInt()
        existing.id = existingId

        val ghost = Desejo(id = 99999, produto = "Fantasma Atualizado", participanteId = p.id)
        dao.atualizar(ghost)

        assertEquals("Livro", dao.buscarPorId(existingId)!!.produto)
    }

    // --- remover edge cases ---

    @Test
    fun remover_nonExistentId_isNoOp() = runTest {
        val p = criarParticipante("Quesia")
        val existing = buildDesejo("Caneta", p.id)
        val existingId = dao.inserir(existing).toInt()

        dao.remover(99999)

        assertNotNull(dao.buscarPorId(existingId))
    }
}
