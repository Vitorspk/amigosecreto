package activity.amigosecreto.repository

import androidx.test.core.app.ApplicationProvider
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.ParticipanteDAO
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

    private lateinit var repo: SorteioRepository
    private lateinit var participanteDao: ParticipanteDAO
    private lateinit var grupoDao: GrupoDAO
    private var grupoId = 0

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        repo = SorteioRepository(ctx)
        participanteDao = ParticipanteDAO(ctx)
        participanteDao.open()
        grupoDao = GrupoDAO(ctx)
        grupoDao.open()

        val g = Grupo().apply { nome = "Grupo Teste" }
        grupoId = grupoDao.inserir(g).toInt()
    }

    @After
    fun tearDown() {
        grupoDao.limparTudo()
        grupoDao.close()
        participanteDao.close()
    }

    private fun criarParticipante(nome: String): Participante {
        val p = Participante().apply { this.nome = nome }
        participanteDao.inserir(p, grupoId)
        return p
    }

    @Test
    fun salvarSorteioCompleto_retorna_id_positivo() {
        val ana = criarParticipante("Ana")
        val bruno = criarParticipante("Bruno")
        val carlos = criarParticipante("Carlos")

        val id = repo.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        assertTrue(id > 0)
    }

    @Test
    fun listarPorGrupo_retorna_sorteio_salvo() {
        val ana = criarParticipante("Ana")
        val bruno = criarParticipante("Bruno")
        val carlos = criarParticipante("Carlos")
        repo.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val sorteios = repo.listarPorGrupo(grupoId)

        assertEquals(1, sorteios.size)
        assertEquals(3, sorteios[0].pares.size)
    }

    @Test
    fun listarPorGrupo_retorna_vazio_para_grupo_sem_sorteio() {
        val sorteios = repo.listarPorGrupo(grupoId)
        assertTrue(sorteios.isEmpty())
    }

    @Test
    fun buscarUltimoPorGrupo_retorna_null_sem_sorteio() {
        assertNull(repo.buscarUltimoPorGrupo(grupoId))
    }

    @Test
    fun buscarUltimoPorGrupo_retorna_sorteio_com_pares() {
        val ana = criarParticipante("Ana")
        val bruno = criarParticipante("Bruno")
        val carlos = criarParticipante("Carlos")
        repo.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val ultimo = repo.buscarUltimoPorGrupo(grupoId)

        assertNotNull(ultimo)
        assertEquals(3, ultimo!!.pares.size)
    }
}
