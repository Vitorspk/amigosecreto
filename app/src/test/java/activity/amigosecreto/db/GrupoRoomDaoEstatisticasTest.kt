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
class GrupoRoomDaoEstatisticasTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: GrupoRoomDao
    private lateinit var participanteDao: ParticipanteRoomDao
    private lateinit var desejoDao: activity.amigosecreto.db.room.DesejoRoomDao
    private lateinit var sorteioDao: SorteioRoomDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.grupoDao()
        participanteDao = db.participanteDao()
        desejoDao = db.desejoDao()
        sorteioDao = db.sorteioDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- contarGrupos ---

    @Test
    fun contarGrupos_semGrupos_retornaZero() = runTest {
        assertEquals(0, dao.contarGrupos())
    }

    @Test
    fun contarGrupos_comDoisGrupos_retornaDois() = runTest {
        dao.inserir(Grupo(nome = "G1"))
        dao.inserir(Grupo(nome = "G2"))

        assertEquals(2, dao.contarGrupos())
    }

    // --- contarParticipantes ---

    @Test
    fun contarParticipantes_semParticipantes_retornaZero() = runTest {
        assertEquals(0, dao.contarParticipantes())
    }

    @Test
    fun contarParticipantes_comParticipantes_retornaTodos() = runTest {
        val grupoId = dao.inserir(Grupo(nome = "G")).toInt()
        participanteDao.inserir(Participante(nome = "Ana", grupoId = grupoId))
        participanteDao.inserir(Participante(nome = "Bob", grupoId = grupoId))

        assertEquals(2, dao.contarParticipantes())
    }

    // --- contarSorteios ---

    @Test
    fun contarSorteios_semSorteios_retornaZero() = runTest {
        assertEquals(0, dao.contarSorteios())
    }

    @Test
    fun contarSorteios_comUmSorteio_retornaUm() = runTest {
        val grupoId = dao.inserir(Grupo(nome = "G")).toInt()
        sorteioDao.inserirSorteio(Sorteio(grupoId = grupoId, dataHora = "2025-01-01T00:00:00"))

        assertEquals(1, dao.contarSorteios())
    }

    // --- contarDesejos ---

    @Test
    fun contarDesejos_semDesejos_retornaZero() = runTest {
        assertEquals(0, dao.contarDesejos())
    }

    @Test
    fun contarDesejos_comDesejos_retornaTodos() = runTest {
        val grupoId = dao.inserir(Grupo(nome = "G")).toInt()
        val pId = participanteDao.inserir(Participante(nome = "Ana", grupoId = grupoId)).toInt()
        desejoDao.inserir(Desejo(produto = "Livro", participanteId = pId))
        desejoDao.inserir(Desejo(produto = "Jogo", participanteId = pId))

        assertEquals(2, dao.contarDesejos())
    }

    // --- mediaValorDesejos ---

    @Test
    fun mediaValorDesejos_semDesejos_retornaNull() = runTest {
        assertNull(dao.mediaValorDesejos())
    }

    @Test
    fun mediaValorDesejos_desejoSemPreco_retornaNull() = runTest {
        val grupoId = dao.inserir(Grupo(nome = "G")).toInt()
        val pId = participanteDao.inserir(Participante(nome = "Ana", grupoId = grupoId)).toInt()
        desejoDao.inserir(Desejo(produto = "Livro", participanteId = pId))

        assertNull(dao.mediaValorDesejos())
    }

    @Test
    fun mediaValorDesejos_comMinEMax_retornaMediaDoCasal() = runTest {
        val grupoId = dao.inserir(Grupo(nome = "G")).toInt()
        val pId = participanteDao.inserir(Participante(nome = "Ana", grupoId = grupoId)).toInt()
        // media do par: (100+200)/2 = 150
        desejoDao.inserir(Desejo(produto = "A", precoMinimo = 100.0, precoMaximo = 200.0, participanteId = pId))

        val media = dao.mediaValorDesejos()
        assertNotNull(media)
        assertEquals(150.0, media!!, 0.01)
    }

    @Test
    fun mediaValorDesejos_comSoMinimo_usaMinimo() = runTest {
        val grupoId = dao.inserir(Grupo(nome = "G")).toInt()
        val pId = participanteDao.inserir(Participante(nome = "Ana", grupoId = grupoId)).toInt()
        desejoDao.inserir(Desejo(produto = "A", precoMinimo = 80.0, participanteId = pId))

        val media = dao.mediaValorDesejos()
        assertNotNull(media)
        assertEquals(80.0, media!!, 0.01)
    }
}
