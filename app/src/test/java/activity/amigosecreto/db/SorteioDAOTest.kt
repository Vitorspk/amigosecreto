package activity.amigosecreto.db

import androidx.test.core.app.ApplicationProvider
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

    private lateinit var dao: SorteioDAO
    private lateinit var participanteDao: ParticipanteDAO
    private lateinit var grupoDao: GrupoDAO
    private var grupoId = 0

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        dao = SorteioDAO(ctx)
        dao.open()
        participanteDao = ParticipanteDAO(ctx)
        participanteDao.open()
        grupoDao = GrupoDAO(ctx)
        grupoDao.open()

        val g = Grupo().apply { nome = "Grupo Natal" }
        grupoId = grupoDao.inserir(g).toInt()
    }

    @After
    fun tearDown() {
        grupoDao.limparTudo()
        grupoDao.close()
        participanteDao.close()
        dao.close()
    }

    private fun criarParticipante(nome: String): Participante {
        val p = Participante().apply { this.nome = nome }
        participanteDao.inserir(p, grupoId)
        assertTrue("ParticipanteDAO.inserir deve atribuir ID > 0", p.id > 0)
        return p
    }

    private fun criarTresParticipantes(): Triple<Participante, Participante, Participante> {
        val ana = criarParticipante("Ana")
        val bruno = criarParticipante("Bruno")
        val carlos = criarParticipante("Carlos")
        return Triple(ana, bruno, carlos)
    }

    // --- salvarSorteioCompleto ---

    @Test
    fun salvarSorteioCompleto_retorna_id_positivo() {
        val (ana, bruno, carlos) = criarTresParticipantes()
        val participantes = listOf(ana, bruno, carlos)
        val sorteados = listOf(bruno, carlos, ana)

        val id = dao.salvarSorteioCompleto(grupoId, participantes, sorteados)

        assertTrue(id > 0)
    }

    @Test
    fun salvarSorteioCompleto_cria_registro_em_sorteio() {
        val (ana, bruno, carlos) = criarTresParticipantes()

        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val sorteios = dao.listarPorGrupo(grupoId)
        assertEquals(1, sorteios.size)
        assertEquals(grupoId, sorteios[0].grupoId)
    }

    @Test
    fun salvarSorteioCompleto_cria_pares_com_nomes_snapshotados() {
        val (ana, bruno, carlos) = criarTresParticipantes()

        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val pares = dao.listarPorGrupo(grupoId)[0].pares
        assertEquals(3, pares.size)
        val parAna = pares.first { it.nomeParticipante == "Ana" }
        assertEquals("Bruno", parAna.nomeSorteado)
    }

    @Test
    fun salvarSorteioCompleto_atualiza_amigo_sorteado_id_em_participante() {
        val (ana, bruno, carlos) = criarTresParticipantes()

        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val participantes = participanteDao.listarPorGrupo(grupoId)
        val anaAtualizada = participantes.first { it.nome == "Ana" }
        assertEquals(bruno.id, anaAtualizada.amigoSorteadoId ?: -1)
    }

    @Test
    fun salvarSorteioCompleto_listas_diferentes_retorna_menos_um() {
        val ana = criarParticipante("Ana")
        val bruno = criarParticipante("Bruno")

        val id = dao.salvarSorteioCompleto(grupoId, listOf(ana), listOf(ana, bruno))

        assertEquals(-1, id)
    }

    @Test
    fun salvarSorteioCompleto_multiplos_sorteios_ficam_no_historico() {
        val (ana, bruno, carlos) = criarTresParticipantes()

        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))
        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(carlos, ana, bruno))

        val sorteios = dao.listarPorGrupo(grupoId)
        assertEquals(2, sorteios.size)
    }

    // --- listarPorGrupo ---

    @Test
    fun listarPorGrupo_retorna_lista_vazia_se_nenhum_sorteio() {
        val sorteios = dao.listarPorGrupo(grupoId)
        assertTrue(sorteios.isEmpty())
    }

    @Test
    fun listarPorGrupo_nao_retorna_sorteios_de_outro_grupo() {
        val (ana, bruno, carlos) = criarTresParticipantes()
        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val outroGrupo = Grupo().apply { nome = "Outro" }
        val outroGrupoId = grupoDao.inserir(outroGrupo).toInt()

        val sorteios = dao.listarPorGrupo(outroGrupoId)
        assertTrue(sorteios.isEmpty())
    }

    // --- buscarUltimoPorGrupo ---

    @Test
    fun buscarUltimoPorGrupo_retorna_null_se_nenhum_sorteio() {
        assertNull(dao.buscarUltimoPorGrupo(grupoId))
    }

    @Test
    fun buscarUltimoPorGrupo_retorna_sorteio_com_pares() {
        val (ana, bruno, carlos) = criarTresParticipantes()
        dao.salvarSorteioCompleto(grupoId, listOf(ana, bruno, carlos), listOf(bruno, carlos, ana))

        val ultimo = dao.buscarUltimoPorGrupo(grupoId)

        assertNotNull(ultimo)
        assertEquals(3, ultimo!!.pares.size)
    }
}
