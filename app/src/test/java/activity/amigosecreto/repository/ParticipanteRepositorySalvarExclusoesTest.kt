package activity.amigosecreto.repository

import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
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

/**
 * Testes diretos para ParticipanteRepository.salvarExclusoes().
 *
 * Esse método executa uma transação atômica que adiciona e remove exclusões
 * em uma única chamada. Esses testes garantem que o contrato da transação
 * é preservado.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ParticipanteRepositorySalvarExclusoesTest {

    private lateinit var db: AppDatabase
    private lateinit var grupoDao: GrupoRoomDao
    private lateinit var participanteRoomDao: ParticipanteRoomDao
    private lateinit var repository: ParticipanteRepository
    private var grupoId = 0

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        grupoDao = db.grupoDao()
        participanteRoomDao = db.participanteDao()

        val grupo = Grupo(nome = "Grupo Exclusões")
        grupoId = grupoDao.inserir(grupo).toInt()

        repository = ParticipanteRepository(participanteRoomDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun inserir(nome: String): Int {
        val p = Participante(nome = nome)
        repository.inserir(p, grupoId)
        return p.id
    }

    private suspend fun exclusoesDeParticipante(participanteId: Int): List<Int> =
        repository.listarPorGrupo(grupoId)
            .firstOrNull { it.id == participanteId }
            ?.idsExcluidos
            ?: emptyList()

    // ===== adicionar exclusões =====

    @Test
    fun salvarExclusoes_adicionar_persisteExclusao() = runTest {
        val idAna = inserir("Ana")
        val idBruno = inserir("Bruno")
        repository.salvarExclusoes(idAna, listOf(idBruno), emptyList())
        assertTrue(exclusoesDeParticipante(idAna).contains(idBruno))
    }

    @Test
    fun salvarExclusoes_adicionarMultiplas_todasPersistidas() = runTest {
        val idCarla = inserir("Carla")
        val idDiego = inserir("Diego")
        val idEva = inserir("Eva")
        repository.salvarExclusoes(idCarla, listOf(idDiego, idEva), emptyList())
        val excl = exclusoesDeParticipante(idCarla)
        assertTrue(excl.contains(idDiego))
        assertTrue(excl.contains(idEva))
    }

    // ===== remover exclusões =====

    @Test
    fun salvarExclusoes_remover_removeExclusaoExistente() = runTest {
        val idFelipe = inserir("Felipe")
        val idGabi = inserir("Gabi")
        repository.salvarExclusoes(idFelipe, listOf(idGabi), emptyList())
        assertTrue(exclusoesDeParticipante(idFelipe).contains(idGabi))
        repository.salvarExclusoes(idFelipe, emptyList(), listOf(idGabi))
        assertFalse(exclusoesDeParticipante(idFelipe).contains(idGabi))
    }

    // ===== adicionar e remover na mesma chamada =====

    @Test
    fun salvarExclusoes_adicionarERemover_transacaoAtomicaCorreta() = runTest {
        val idHugo = inserir("Hugo")
        val idIris = inserir("Iris")
        val idJoao = inserir("João")
        repository.salvarExclusoes(idHugo, listOf(idIris), emptyList())

        repository.salvarExclusoes(idHugo, listOf(idJoao), listOf(idIris))

        val excl = exclusoesDeParticipante(idHugo)
        assertTrue("Hugo deve excluir João", excl.contains(idJoao))
        assertFalse("Hugo não deve mais excluir Iris", excl.contains(idIris))
    }

    // ===== listas vazias =====

    @Test
    fun salvarExclusoes_listasVazias_naoAlteraEstadoAtual() = runTest {
        val idKaren = inserir("Karen")
        val idLucas = inserir("Lucas")
        repository.salvarExclusoes(idKaren, listOf(idLucas), emptyList())
        val antes = exclusoesDeParticipante(idKaren).size
        repository.salvarExclusoes(idKaren, emptyList(), emptyList())
        assertEquals(antes, exclusoesDeParticipante(idKaren).size)
    }

    @Test
    fun salvarExclusoes_ambosVazios_naoLancaExcecao() = runTest {
        val idMaria = inserir("Maria")
        repository.salvarExclusoes(idMaria, emptyList(), emptyList())
    }

    // ===== não contamina outros participantes =====

    @Test
    fun salvarExclusoes_naoAfetaOutrosParticipantes() = runTest {
        val idNadia = inserir("Nadia")
        val idOtto = inserir("Otto")
        val idPaulo = inserir("Paulo")
        repository.salvarExclusoes(idNadia, listOf(idOtto), emptyList())
        assertTrue(exclusoesDeParticipante(idOtto).isEmpty())
        assertTrue(exclusoesDeParticipante(idPaulo).isEmpty())
    }
}
