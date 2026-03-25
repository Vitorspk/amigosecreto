package activity.amigosecreto.db

import activity.amigosecreto.db.room.AppDatabase
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
class ParticipanteRoomDaoNovosCamposTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ParticipanteRoomDao
    private lateinit var grupoDao: GrupoRoomDao
    private var grupoId = 0

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        grupoDao = db.grupoDao()
        dao = db.participanteDao()
        grupoId = grupoDao.inserir(Grupo(nome = "Grupo Teste")).toInt()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun inserir(nome: String): Int {
        val p = Participante(nome = nome, grupoId = grupoId)
        return dao.inserir(p).toInt()
    }

    // --- marcarConfirmacaoCompra ---

    @Test
    fun marcarConfirmacaoCompra_atualizaFlagParaVerdadeiro() = runTest {
        val id = inserir("Ana")

        dao.marcarConfirmacaoCompra(id)

        val participantes = dao.listarPorGrupoSemExclusoes(grupoId)
        val ana = participantes.first { it.id == id }
        assertTrue("confirmouPresente deve ser true após marcarConfirmacaoCompra", ana.confirmouPresente)
    }

    @Test
    fun marcarConfirmacaoCompra_naoAfetaOutrosParticipantes() = runTest {
        val idAna = inserir("Ana")
        val idBob = inserir("Bob")

        dao.marcarConfirmacaoCompra(idAna)

        val participantes = dao.listarPorGrupoSemExclusoes(grupoId)
        val bob = participantes.first { it.id == idBob }
        assertFalse("Bob não deve ter confirmouPresente = true", bob.confirmouPresente)
    }

    // --- marcarComoNotificado ---

    @Test
    fun marcarComoNotificado_atualizaFlagFoiNotificado() = runTest {
        val id = inserir("Carlos")

        dao.marcarComoNotificado(id)

        val participantes = dao.listarPorGrupoSemExclusoes(grupoId)
        val carlos = participantes.first { it.id == id }
        assertTrue("foiNotificado deve ser true após marcarComoNotificado", carlos.foiNotificado)
    }

    @Test
    fun marcarComoNotificado_naoAfetaOutrosParticipantes() = runTest {
        val idCarlos = inserir("Carlos")
        val idDiana = inserir("Diana")

        dao.marcarComoNotificado(idCarlos)

        val participantes = dao.listarPorGrupoSemExclusoes(grupoId)
        val diana = participantes.first { it.id == idDiana }
        assertFalse("Diana não deve ter foiNotificado = true", diana.foiNotificado)
    }

    // --- contarConfirmados ---

    @Test
    fun contarConfirmados_semNinguemConfirmado_retornaZero() = runTest {
        inserir("Ana")
        inserir("Bob")

        val total = dao.contarConfirmados(grupoId)

        assertEquals(0, total)
    }

    @Test
    fun contarConfirmados_comUmConfirmado_retornaUm() = runTest {
        val idAna = inserir("Ana")
        inserir("Bob")
        dao.marcarConfirmacaoCompra(idAna)

        val total = dao.contarConfirmados(grupoId)

        assertEquals(1, total)
    }

    @Test
    fun contarConfirmados_todosConfirmados_retornaTodos() = runTest {
        val idAna = inserir("Ana")
        val idBob = inserir("Bob")
        dao.marcarConfirmacaoCompra(idAna)
        dao.marcarConfirmacaoCompra(idBob)

        val total = dao.contarConfirmados(grupoId)

        assertEquals(2, total)
    }

    @Test
    fun contarConfirmados_naoContaConfirmadosDeOutroGrupo() = runTest {
        val grupoId2 = grupoDao.inserir(Grupo(nome = "Outro Grupo")).toInt()
        val idAna = inserir("Ana")
        val idBob = dao.inserir(Participante(nome = "Bob", grupoId = grupoId2)).toInt()
        dao.marcarConfirmacaoCompra(idAna)
        dao.marcarConfirmacaoCompra(idBob)

        val total = dao.contarConfirmados(grupoId)

        assertEquals(1, total)
    }
}
