package activity.amigosecreto.db

import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.db.room.GrupoRoomDao
import activity.amigosecreto.db.room.ParticipanteRoomDao
import activity.amigosecreto.db.room.SorteioRoomDao
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
class SorteioDAOTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SorteioRoomDao
    private lateinit var participanteDao: ParticipanteRoomDao
    private lateinit var grupoDao: GrupoRoomDao
    private var grupoId = 0

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = db.sorteioDao()
        participanteDao = db.participanteDao()
        grupoDao = db.grupoDao()

        val g = Grupo(nome = "Grupo Natal")
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

    private suspend fun criarTresParticipantes(): Triple<Participante, Participante, Participante> {
        val ana = criarParticipante("Ana")
        val bruno = criarParticipante("Bruno")
        val carlos = criarParticipante("Carlos")
        return Triple(ana, bruno, carlos)
    }

    // --- salvarSorteioCompleto ---

    @Test
    fun salvarSorteioCompleto_retorna_id_positivo() = runTest {
        val (ana, bruno, carlos) = criarTresParticipantes()
        val participantes = listOf(ana, bruno, carlos)
        val sorteados = listOf(bruno, carlos, ana)

        val id = dao.salvarSorteioCompleto(grupoId, participantes, sorteados)

        assertTrue(id > 0)
    }

    @Test
    fun salvarSorteioCompleto_cria_registro_em_sorteio() = runTest {
        val (ana, bruno, carlos) = criarTresParticipantes()

        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val sorteios = dao.listarPorGrupo(grupoId)
        assertEquals(1, sorteios.size)
        assertEquals(grupoId, sorteios[0].grupoId)
    }

    @Test
    fun salvarSorteioCompleto_cria_pares_com_nomes_snapshotados() = runTest {
        val (ana, bruno, carlos) = criarTresParticipantes()

        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val pares = dao.listarPorGrupo(grupoId)[0].pares
        assertEquals(3, pares.size)
        val parAna = pares.first { it.nomeParticipante == "Ana" }
        assertEquals("Bruno", parAna.nomeSorteado)
    }

    @Test
    fun salvarSorteioCompleto_atualiza_amigo_sorteado_id_em_participante() = runTest {
        val (ana, bruno, carlos) = criarTresParticipantes()

        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val participantes = participanteDao.listarPorGrupoSemExclusoes(grupoId)
        val anaAtualizada = participantes.first { it.nome == "Ana" }
        assertEquals(bruno.id, anaAtualizada.amigoSorteadoId ?: -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun salvarSorteioCompleto_listas_tamanhos_diferentes_lanca_excecao() = runTest {
        val ana = criarParticipante("Ana")
        val bruno = criarParticipante("Bruno")
        // require() em salvarSorteioCompleto lança IllegalArgumentException imediatamente.
        dao.salvarSorteioCompleto(grupoId, listOf(ana), listOf(ana, bruno))
    }

    @Test
    fun salvarSorteioCompleto_multiplos_sorteios_ficam_no_historico() = runTest {
        val (ana, bruno, carlos) = criarTresParticipantes()

        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))
        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(carlos, ana, bruno))

        val sorteios = dao.listarPorGrupo(grupoId)
        assertEquals(2, sorteios.size)
    }

    // --- listarPorGrupo ---

    @Test
    fun listarPorGrupo_retorna_lista_vazia_se_nenhum_sorteio() = runTest {
        val sorteios = dao.listarPorGrupo(grupoId)
        assertTrue(sorteios.isEmpty())
    }

    @Test
    fun listarPorGrupo_nao_retorna_sorteios_de_outro_grupo() = runTest {
        val (ana, bruno, carlos) = criarTresParticipantes()
        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val outroGrupoId = grupoDao.inserir(Grupo(nome = "Outro")).toInt()

        val sorteios = dao.listarPorGrupo(outroGrupoId)
        assertTrue(sorteios.isEmpty())
    }

    // --- buscarUltimoPorGrupo ---

    @Test
    fun buscarUltimoPorGrupo_retorna_null_se_nenhum_sorteio() = runTest {
        assertNull(dao.buscarUltimoPorGrupo(grupoId))
    }

    @Test
    fun buscarUltimoPorGrupo_retorna_sorteio_com_pares() = runTest {
        val (ana, bruno, carlos) = criarTresParticipantes()
        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val ultimo = dao.buscarUltimoPorGrupo(grupoId)

        assertNotNull(ultimo)
        assertEquals(3, ultimo!!.pares.size)
    }
}
