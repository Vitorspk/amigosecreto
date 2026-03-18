package activity.amigosecreto.repository

import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
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
class SorteioRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var grupoDao: GrupoRoomDao
    private lateinit var participanteRoomDao: ParticipanteRoomDao
    private lateinit var sorteioRoomDao: SorteioRoomDao
    private lateinit var repo: SorteioRepository
    private var grupoId = 0

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        grupoDao = db.grupoDao()
        participanteRoomDao = db.participanteDao()
        sorteioRoomDao = db.sorteioDao()

        val g = Grupo(nome = "Grupo Teste")
        grupoId = grupoDao.inserir(g).toInt()

        repo = SorteioRepository(sorteioRoomDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun criarParticipante(nome: String): Participante {
        val p = Participante(nome = nome, grupoId = grupoId)
        val id = participanteRoomDao.inserir(p).toInt()
        p.id = id
        return p
    }

    @Test
    fun salvarSorteioCompleto_retorna_id_positivo() = runTest {
        val ana = criarParticipante("Ana")
        val bruno = criarParticipante("Bruno")
        val carlos = criarParticipante("Carlos")

        val id = repo.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        assertTrue(id > 0)
    }

    @Test
    fun listarPorGrupo_retorna_sorteio_salvo() = runTest {
        val ana = criarParticipante("Ana")
        val bruno = criarParticipante("Bruno")
        val carlos = criarParticipante("Carlos")
        repo.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val sorteios = repo.listarPorGrupo(grupoId)

        assertEquals(1, sorteios.size)
        assertEquals(3, sorteios[0].pares.size)
    }

    @Test
    fun listarPorGrupo_retorna_vazio_para_grupo_sem_sorteio() = runTest {
        val sorteios = repo.listarPorGrupo(grupoId)
        assertTrue(sorteios.isEmpty())
    }

    @Test
    fun buscarUltimoPorGrupo_retorna_null_sem_sorteio() = runTest {
        assertNull(repo.buscarUltimoPorGrupo(grupoId))
    }

    @Test
    fun buscarUltimoPorGrupo_retorna_sorteio_com_pares() = runTest {
        val ana = criarParticipante("Ana")
        val bruno = criarParticipante("Bruno")
        val carlos = criarParticipante("Carlos")
        repo.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val ultimo = repo.buscarUltimoPorGrupo(grupoId)

        assertNotNull(ultimo)
        assertEquals(3, ultimo!!.pares.size)
    }
}
