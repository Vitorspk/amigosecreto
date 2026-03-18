package activity.amigosecreto.repository

import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
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
 * Testes de integração do DesejoRepository via Robolectric + Room in-memory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DesejoRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var grupoDao: GrupoRoomDao
    private lateinit var participanteRoomDao: ParticipanteRoomDao
    private lateinit var desejoRoomDao: DesejoRoomDao
    private lateinit var repository: DesejoRepository
    private var grupoId = 0
    private var participanteId = 0

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        grupoDao = db.grupoDao()
        participanteRoomDao = db.participanteDao()
        desejoRoomDao = db.desejoDao()

        val grupo = Grupo(nome = "Grupo Teste", data = "01/01/2025")
        grupoId = grupoDao.inserir(grupo).toInt()

        val p = Participante(nome = "Participante Teste", grupoId = grupoId)
        participanteId = participanteRoomDao.inserir(p).toInt()

        repository = DesejoRepository(desejoRoomDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun criarDesejo(produto: String) = Desejo(
        produto = produto,
        participanteId = participanteId,
    )

    // =========================================================
    // inserir / listarPorParticipante
    // =========================================================

    @Test
    fun inserir_e_listarPorParticipante_retornaDesejoInserido() = runTest {
        repository.inserir(criarDesejo("Livro"))
        val lista = repository.listarPorParticipante(participanteId)
        assertEquals(1, lista.size)
        assertEquals("Livro", lista[0].produto)
    }

    @Test
    fun listarPorParticipante_semDesejos_retornaListaVazia() = runTest {
        val lista = repository.listarPorParticipante(participanteId)
        assertNotNull(lista)
        assertTrue(lista.isEmpty())
    }

    @Test
    fun inserir_multiplos_listarRetornaTodos() = runTest {
        repository.inserir(criarDesejo("Caneta"))
        repository.inserir(criarDesejo("Caderno"))
        repository.inserir(criarDesejo("Mochila"))
        assertEquals(3, repository.listarPorParticipante(participanteId).size)
    }

    @Test
    fun inserir_atribuiIdGeradoPeloBanco() = runTest {
        val d = criarDesejo("Fone")
        repository.inserir(d)
        assertTrue(d.id > 0)
    }

    // =========================================================
    // contarDesejosPorParticipante
    // =========================================================

    @Test
    fun contarDesejosPorParticipante_semDesejos_retornaZero() = runTest {
        assertEquals(0, repository.contarDesejosPorParticipante(participanteId))
    }

    @Test
    fun contarDesejosPorParticipante_comDesejos_retornaContagem() = runTest {
        repository.inserir(criarDesejo("Item A"))
        repository.inserir(criarDesejo("Item B"))
        assertEquals(2, repository.contarDesejosPorParticipante(participanteId))
    }

    // =========================================================
    // alterar
    // =========================================================

    @Test
    fun alterar_produtoAtualizado_persistido() = runTest {
        val original = criarDesejo("Produto Original")
        repository.inserir(original)

        val atualizado = criarDesejo("Produto Atualizado").also { it.id = original.id }
        repository.alterar(original, atualizado)

        assertEquals("Produto Atualizado", repository.buscarPorId(original.id)!!.produto)
    }

    // =========================================================
    // remover
    // =========================================================

    @Test
    fun remover_desejoExistente_removidoDaLista() = runTest {
        val d = criarDesejo("Item para Remover")
        repository.inserir(d)
        assertEquals(1, repository.contarDesejosPorParticipante(participanteId))

        repository.remover(d)
        assertEquals(0, repository.contarDesejosPorParticipante(participanteId))
    }

    // =========================================================
    // buscarPorId
    // =========================================================

    @Test
    fun buscarPorId_retornaDesejoCorreto() = runTest {
        val d = criarDesejo("Item Busca")
        repository.inserir(d)

        val buscado = repository.buscarPorId(d.id)
        assertNotNull(buscado)
        assertEquals("Item Busca", buscado!!.produto)
        assertEquals(participanteId, buscado.participanteId)
    }

    @Test
    fun buscarPorId_idInexistente_retornaNull() = runTest {
        assertNull(repository.buscarPorId(99999))
    }

    // =========================================================
    // listar (todos)
    // =========================================================

    @Test
    fun listar_retornaDesejosDeTodosParticipantes() = runTest {
        repository.inserir(criarDesejo("Bola"))
        repository.inserir(criarDesejo("Raquete"))
        assertTrue(repository.listar().size >= 2)
    }

    // =========================================================
    // contarDesejosPorGrupo
    // =========================================================

    @Test
    fun contarDesejosPorGrupo_grupoVazio_retornaMapaVazio() = runTest {
        val mapa = repository.contarDesejosPorGrupo(grupoId)
        assertNotNull(mapa)
        assertTrue(mapa.isEmpty())
    }

    @Test
    fun contarDesejosPorGrupo_multiplosPorParticipante_somaCorreta() = runTest {
        repository.inserir(criarDesejo("A"))
        repository.inserir(criarDesejo("B"))
        repository.inserir(criarDesejo("C"))
        assertEquals(3, repository.contarDesejosPorGrupo(grupoId)[participanteId])
    }

    // =========================================================
    // listarDesejosPorGrupo
    // =========================================================

    @Test
    fun listarDesejosPorGrupo_retornaTodosAgrupadosPorParticipante() = runTest {
        repository.inserir(criarDesejo("X"))
        repository.inserir(criarDesejo("Y"))

        val mapa = repository.listarDesejosPorGrupo(grupoId)
        assertNotNull(mapa)
        assertTrue(mapa.containsKey(participanteId))
        assertEquals(2, mapa[participanteId]!!.size)
    }

    @Test
    fun listarDesejosPorGrupo_participanteSemDesejos_naoAparece() = runTest {
        val mapa = repository.listarDesejosPorGrupo(grupoId)
        assertFalse(mapa.containsKey(participanteId))
    }

    @Test
    fun inserir_comPrecoECategoria_persistidosCorretamente() = runTest {
        val d = criarDesejo("Tênis").apply {
            categoria = "Esporte"
            precoMinimo = 100.0
            precoMaximo = 300.0
            lojas = "Decathlon, Netshoes"
        }
        repository.inserir(d)

        val buscado = repository.buscarPorId(d.id)!!
        assertEquals("Esporte", buscado.categoria)
        assertEquals(100.0, buscado.precoMinimo, 0.01)
        assertEquals(300.0, buscado.precoMaximo, 0.01)
        assertEquals("Decathlon, Netshoes", buscado.lojas)
    }
}
