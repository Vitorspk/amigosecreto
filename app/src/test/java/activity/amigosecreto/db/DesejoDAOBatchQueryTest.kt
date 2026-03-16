package activity.amigosecreto.db

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes para os métodos de batch query do DesejoDAO:
 *   - contarDesejosPorGrupo(int grupoId)
 *   - listarDesejosPorGrupo(int grupoId)
 *
 * Esses métodos usam INNER JOIN + GROUP BY. São críticos para a migração
 * para Kotlin pois qualquer erro de índice de coluna ou alias SQL causaria
 * NullPointerException silencioso.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DesejoDAOBatchQueryTest {

    private lateinit var desejoDao: DesejoDAO
    private lateinit var participanteDao: ParticipanteDAO
    private lateinit var grupoDao: GrupoDAO
    private var grupoId = 0
    private var grupoId2 = 0

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        desejoDao = DesejoDAO(ctx)
        desejoDao.open()
        participanteDao = ParticipanteDAO(ctx)
        participanteDao.open()
        grupoDao = GrupoDAO(ctx)
        grupoDao.open()

        grupoId = grupoDao.inserir(Grupo().apply { nome = "Grupo A" }).toInt()
        grupoId2 = grupoDao.inserir(Grupo().apply { nome = "Grupo B" }).toInt()
    }

    @After
    fun tearDown() {
        grupoDao.limparTudo()
        grupoDao.close()
        participanteDao.close()
        desejoDao.close()
    }

    private fun inserirParticipante(nome: String, gId: Int): Participante {
        val p = Participante().apply { this.nome = nome }
        participanteDao.inserir(p, gId)
        return p
    }

    private fun inserirDesejo(produto: String, participanteId: Int) {
        val d = Desejo().apply {
            this.produto = produto
            this.categoria = "Cat"
            this.lojas = "Loja"
            this.precoMinimo = 10.0
            this.precoMaximo = 50.0
            this.participanteId = participanteId
        }
        desejoDao.inserir(d)
    }

    // ===== contarDesejosPorGrupo =====

    @Test
    fun contarDesejosPorGrupo_grupoSemDesejos_retornaMapaVazio() {
        inserirParticipante("Ana", grupoId)
        val mapa = desejoDao.contarDesejosPorGrupo(grupoId)
        assertNotNull(mapa)
        assertTrue(mapa.isEmpty())
    }

    @Test
    fun contarDesejosPorGrupo_umParticipanteComDesejos_contagemCorreta() {
        val p = inserirParticipante("Bruno", grupoId)
        inserirDesejo("Item1", p.id)
        inserirDesejo("Item2", p.id)
        inserirDesejo("Item3", p.id)
        assertEquals(3, desejoDao.contarDesejosPorGrupo(grupoId)[p.id])
    }

    @Test
    fun contarDesejosPorGrupo_variosParticipantes_contagemsIndependentes() {
        val p1 = inserirParticipante("Carla", grupoId)
        val p2 = inserirParticipante("Diego", grupoId)
        inserirDesejo("X", p1.id)
        inserirDesejo("Y", p1.id)
        inserirDesejo("Z", p2.id)

        val mapa = desejoDao.contarDesejosPorGrupo(grupoId)
        assertEquals(2, mapa[p1.id])
        assertEquals(1, mapa[p2.id])
    }

    @Test
    fun contarDesejosPorGrupo_naoContaminaOutroGrupo() {
        val pA = inserirParticipante("Eva", grupoId)
        val pB = inserirParticipante("Felipe", grupoId2)
        inserirDesejo("ItemA", pA.id)
        inserirDesejo("ItemB1", pB.id)
        inserirDesejo("ItemB2", pB.id)

        val mapaA = desejoDao.contarDesejosPorGrupo(grupoId)
        val mapaB = desejoDao.contarDesejosPorGrupo(grupoId2)

        assertEquals(1, mapaA[pA.id])
        assertNull(mapaA[pB.id])
        assertEquals(2, mapaB[pB.id])
        assertNull(mapaB[pA.id])
    }

    @Test
    fun contarDesejosPorGrupo_grupoInexistente_retornaMapaVazio() {
        val mapa = desejoDao.contarDesejosPorGrupo(99999)
        assertNotNull(mapa)
        assertTrue(mapa.isEmpty())
    }

    // ===== listarDesejosPorGrupo =====

    @Test
    fun listarDesejosPorGrupo_grupoSemDesejos_retornaMapaVazio() {
        inserirParticipante("Gabi", grupoId)
        val mapa = desejoDao.listarDesejosPorGrupo(grupoId)
        assertNotNull(mapa)
        assertTrue(mapa.isEmpty())
    }

    @Test
    fun listarDesejosPorGrupo_camposPopuladosCorretamente() {
        val p = inserirParticipante("Hugo", grupoId)
        val d = Desejo().apply {
            produto = "Fone"; categoria = "Audio"; lojas = "Amazon"
            precoMinimo = 99.0; precoMaximo = 299.0; participanteId = p.id
        }
        desejoDao.inserir(d)

        val desejos = desejoDao.listarDesejosPorGrupo(grupoId)[p.id]
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
    fun listarDesejosPorGrupo_variosParticipantes_listasSeparadas() {
        val p1 = inserirParticipante("Iris", grupoId)
        val p2 = inserirParticipante("João", grupoId)
        inserirDesejo("Livro A", p1.id)
        inserirDesejo("Livro B", p1.id)
        inserirDesejo("Game", p2.id)

        val mapa = desejoDao.listarDesejosPorGrupo(grupoId)
        assertEquals(2, mapa[p1.id]!!.size)
        assertEquals(1, mapa[p2.id]!!.size)
    }

    @Test
    fun listarDesejosPorGrupo_naoContaminaOutroGrupo() {
        val pA = inserirParticipante("Karen", grupoId)
        val pB = inserirParticipante("Lucas", grupoId2)
        inserirDesejo("ItemA", pA.id)
        inserirDesejo("ItemB", pB.id)

        val mapaA = desejoDao.listarDesejosPorGrupo(grupoId)
        val mapaB = desejoDao.listarDesejosPorGrupo(grupoId2)

        assertTrue(mapaA.containsKey(pA.id))
        assertFalse(mapaA.containsKey(pB.id))
        assertTrue(mapaB.containsKey(pB.id))
        assertFalse(mapaB.containsKey(pA.id))
    }

    @Test
    fun listarDesejosPorGrupo_grupoInexistente_retornaMapaVazio() {
        val mapa = desejoDao.listarDesejosPorGrupo(99999)
        assertNotNull(mapa)
        assertTrue(mapa.isEmpty())
    }

    @Test
    fun listarDesejosPorGrupo_camposOpcionaisNulos_naoLancaExcecao() {
        val p = inserirParticipante("Maria", grupoId)
        val d = Desejo().apply {
            produto = "Item sem extras"; categoria = null; lojas = null
            precoMinimo = 0.0; precoMaximo = 0.0; participanteId = p.id
        }
        desejoDao.inserir(d)

        val desejos = desejoDao.listarDesejosPorGrupo(grupoId)[p.id]
        assertNotNull(desejos)
        assertEquals(1, desejos!!.size)
        assertEquals("Item sem extras", desejos[0].produto)
    }
}