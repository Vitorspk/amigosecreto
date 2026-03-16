package activity.amigosecreto.repository

import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.Participante
import androidx.test.core.app.ApplicationProvider
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

    private lateinit var repository: ParticipanteRepository
    private lateinit var grupoDao: GrupoDAO
    private var grupoId = 0

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        grupoDao = GrupoDAO(ctx)
        grupoDao.open()
        grupoId = grupoDao.inserir(Grupo().apply { nome = "Grupo Exclusões" }).toInt()
        repository = ParticipanteRepository(ctx)
    }

    @After
    fun tearDown() {
        grupoDao.limparTudo()
        grupoDao.close()
    }

    private fun inserir(nome: String): Int {
        val p = Participante().apply { this.nome = nome }
        repository.inserir(p, grupoId)
        return p.id
    }

    private fun exclusoesDeParticipante(participanteId: Int): List<Int> =
        repository.listarPorGrupo(grupoId)
            .firstOrNull { it.id == participanteId }
            ?.idsExcluidos
            ?: emptyList()

    // ===== adicionar exclusões =====

    @Test
    fun salvarExclusoes_adicionar_persisteExclusao() {
        val idAna = inserir("Ana")
        val idBruno = inserir("Bruno")
        repository.salvarExclusoes(idAna, listOf(idBruno), emptyList())
        assertTrue(exclusoesDeParticipante(idAna).contains(idBruno))
    }

    @Test
    fun salvarExclusoes_adicionarMultiplas_todasPersistidas() {
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
    fun salvarExclusoes_remover_removeExclusaoExistente() {
        val idFelipe = inserir("Felipe")
        val idGabi = inserir("Gabi")
        repository.salvarExclusoes(idFelipe, listOf(idGabi), emptyList())
        assertTrue(exclusoesDeParticipante(idFelipe).contains(idGabi))
        repository.salvarExclusoes(idFelipe, emptyList(), listOf(idGabi))
        assertFalse(exclusoesDeParticipante(idFelipe).contains(idGabi))
    }

    // ===== adicionar e remover na mesma chamada =====

    @Test
    fun salvarExclusoes_adicionarERemover_transacaoAtomicaCorreta() {
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
    fun salvarExclusoes_listasVazias_naoAlteraEstadoAtual() {
        val idKaren = inserir("Karen")
        val idLucas = inserir("Lucas")
        repository.salvarExclusoes(idKaren, listOf(idLucas), emptyList())
        val antes = exclusoesDeParticipante(idKaren).size
        repository.salvarExclusoes(idKaren, emptyList(), emptyList())
        assertEquals(antes, exclusoesDeParticipante(idKaren).size)
    }

    @Test
    fun salvarExclusoes_ambosVazios_naoLancaExcecao() {
        val idMaria = inserir("Maria")
        repository.salvarExclusoes(idMaria, emptyList(), emptyList())
    }

    // ===== não contamina outros participantes =====

    @Test
    fun salvarExclusoes_naoAfetaOutrosParticipantes() {
        val idNadia = inserir("Nadia")
        val idOtto = inserir("Otto")
        val idPaulo = inserir("Paulo")
        repository.salvarExclusoes(idNadia, listOf(idOtto), emptyList())
        assertTrue(exclusoesDeParticipante(idOtto).isEmpty())
        assertTrue(exclusoesDeParticipante(idPaulo).isEmpty())
    }
}