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

/**
 * Testes para os métodos de batch query do DesejoRoomDao:
 *   - contarDesejosPorGrupo(grupoId)
 *   - listarDesejosPorGrupo(grupoId)
 *
 * Esses métodos usam INNER JOIN + GROUP BY. São críticos para verificar
 * que Room processa corretamente os aliases SQL e os mapeamentos de coluna.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DesejoDAOBatchQueryTest {

    private lateinit var db: AppDatabase
    private lateinit var desejoDao: DesejoRoomDao
    private lateinit var participanteDao: ParticipanteRoomDao
    private lateinit var grupoDao: GrupoRoomDao
    private var grupoId = 0
    private var grupoId2 = 0

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        desejoDao = db.desejoDao()
        participanteDao = db.participanteDao()
        grupoDao = db.grupoDao()

        grupoId = grupoDao.inserir(Grupo(nome = "Grupo A")).toInt()
        grupoId2 = grupoDao.inserir(Grupo(nome = "Grupo B")).toInt()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun inserirParticipante(nome: String, gId: Int): Participante {
        val p = Participante(nome = nome, grupoId = gId)
        val id = participanteDao.inserir(p).toInt()
        p.id = id
        return p
    }

    private suspend fun inserirDesejo(produto: String, participanteId: Int) {
        val d = Desejo(
            produto = produto,
            categoria = "Cat",
            lojas = "Loja",
            precoMinimo = 10.0,
            precoMaximo = 50.0,
            participanteId = participanteId,
        )
        desejoDao.inserir(d)
    }

    // ===== contarDesejosPorGrupo =====

    @Test
    fun contarDesejosPorGrupo_grupoSemDesejos_retornaMapaVazio() = runTest {
        inserirParticipante("Ana", grupoId)
        val mapa = desejoDao.contarDesejosPorGrupo(grupoId).associate { it.participanteId to it.count }
        assertNotNull(mapa)
        assertTrue(mapa.isEmpty())
    }

    @Test
    fun contarDesejosPorGrupo_umParticipanteComDesejos_contagemCorreta() = runTest {
        val p = inserirParticipante("Bruno", grupoId)
        inserirDesejo("Item1", p.id)
        inserirDesejo("Item2", p.id)
        inserirDesejo("Item3", p.id)
        val mapa = desejoDao.contarDesejosPorGrupo(grupoId).associate { it.participanteId to it.count }
        assertEquals(3, mapa[p.id])
    }

    @Test
    fun contarDesejosPorGrupo_variosParticipantes_contagemsIndependentes() = runTest {
        val p1 = inserirParticipante("Carla", grupoId)
        val p2 = inserirParticipante("Diego", grupoId)
        inserirDesejo("X", p1.id)
        inserirDesejo("Y", p1.id)
        inserirDesejo("Z", p2.id)

        val mapa = desejoDao.contarDesejosPorGrupo(grupoId).associate { it.participanteId to it.count }
        assertEquals(2, mapa[p1.id])
        assertEquals(1, mapa[p2.id])
    }

    @Test
    fun contarDesejosPorGrupo_naoContaminaOutroGrupo() = runTest {
        val pA = inserirParticipante("Eva", grupoId)
        val pB = inserirParticipante("Felipe", grupoId2)
        inserirDesejo("ItemA", pA.id)
        inserirDesejo("ItemB1", pB.id)
        inserirDesejo("ItemB2", pB.id)

        val mapaA = desejoDao.contarDesejosPorGrupo(grupoId).associate { it.participanteId to it.count }
        val mapaB = desejoDao.contarDesejosPorGrupo(grupoId2).associate { it.participanteId to it.count }

        assertEquals(1, mapaA[pA.id])
        assertNull(mapaA[pB.id])
        assertEquals(2, mapaB[pB.id])
        assertNull(mapaB[pA.id])
    }

    @Test
    fun contarDesejosPorGrupo_grupoInexistente_retornaMapaVazio() = runTest {
        val mapa = desejoDao.contarDesejosPorGrupo(99999).associate { it.participanteId to it.count }
        assertNotNull(mapa)
        assertTrue(mapa.isEmpty())
    }

    // ===== listarDesejosPorGrupo =====

    @Test
    fun listarDesejosPorGrupo_grupoSemDesejos_retornaMapaVazio() = runTest {
        inserirParticipante("Gabi", grupoId)
        val mapa = desejoDao.listarDesejosPorGrupo(grupoId).groupBy { it.participanteId }
        assertNotNull(mapa)
        assertTrue(mapa.isEmpty())
    }

    @Test
    fun listarDesejosPorGrupo_camposPopuladosCorretamente() = runTest {
        val p = inserirParticipante("Hugo", grupoId)
        val d = Desejo(
            produto = "Fone",
            categoria = "Audio",
            lojas = "Amazon",
            precoMinimo = 99.0,
            precoMaximo = 299.0,
            participanteId = p.id,
        )
        desejoDao.inserir(d)

        val desejos = desejoDao.listarDesejosPorGrupo(grupoId).groupBy { it.participanteId }[p.id]
        assertNotNull(desejos)
        assertEquals(1, desejos!!.size)
        val found = desejos[0]
        assertEquals("Fone", found.produto)
        assertEquals("Audio", found.categoria)
        assertEquals("Amazon", found.lojas)
        assertEquals(99.0, found.precoMinimo, 0.001)
        assertEquals(299.0, found.precoMaximo, 0.001)
        assertEquals(p.id, found.participanteId)
    }

    @Test
    fun listarDesejosPorGrupo_variosParticipantes_listasSeparadas() = runTest {
        val p1 = inserirParticipante("Iris", grupoId)
        val p2 = inserirParticipante("João", grupoId)
        inserirDesejo("Livro A", p1.id)
        inserirDesejo("Livro B", p1.id)
        inserirDesejo("Game", p2.id)

        val mapa = desejoDao.listarDesejosPorGrupo(grupoId).groupBy { it.participanteId }
        assertEquals(2, mapa[p1.id]!!.size)
        assertEquals(1, mapa[p2.id]!!.size)
    }

    @Test
    fun listarDesejosPorGrupo_naoContaminaOutroGrupo() = runTest {
        val pA = inserirParticipante("Karen", grupoId)
        val pB = inserirParticipante("Lucas", grupoId2)
        inserirDesejo("ItemA", pA.id)
        inserirDesejo("ItemB", pB.id)

        val mapaA = desejoDao.listarDesejosPorGrupo(grupoId).groupBy { it.participanteId }
        val mapaB = desejoDao.listarDesejosPorGrupo(grupoId2).groupBy { it.participanteId }

        assertTrue(mapaA.containsKey(pA.id))
        assertFalse(mapaA.containsKey(pB.id))
        assertTrue(mapaB.containsKey(pB.id))
        assertFalse(mapaB.containsKey(pA.id))
    }

    @Test
    fun listarDesejosPorGrupo_grupoInexistente_retornaMapaVazio() = runTest {
        val mapa = desejoDao.listarDesejosPorGrupo(99999).groupBy { it.participanteId }
        assertNotNull(mapa)
        assertTrue(mapa.isEmpty())
    }

    @Test
    fun listarDesejosPorGrupo_camposOpcionaisNulos_naoLancaExcecao() = runTest {
        val p = inserirParticipante("Maria", grupoId)
        val d = Desejo(
            produto = "Item sem extras",
            categoria = null,
            lojas = null,
            precoMinimo = 0.0,
            precoMaximo = 0.0,
            participanteId = p.id,
        )
        desejoDao.inserir(d)

        val desejos = desejoDao.listarDesejosPorGrupo(grupoId).groupBy { it.participanteId }[p.id]
        assertNotNull(desejos)
        assertEquals(1, desejos!!.size)
        assertEquals("Item sem extras", desejos[0].produto)
    }
}
