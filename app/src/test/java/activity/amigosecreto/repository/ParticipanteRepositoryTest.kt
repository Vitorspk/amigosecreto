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
 * Testes de integração do ParticipanteRepository via Robolectric + SQLite real.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ParticipanteRepositoryTest {

    private lateinit var repository: ParticipanteRepository
    private lateinit var grupoDao: GrupoDAO
    private var grupoId = 0

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        grupoDao = GrupoDAO(ctx)
        grupoDao.open()
        grupoId = grupoDao.inserir(Grupo().apply { nome = "Grupo Teste"; data = "01/01/2025" }).toInt()
        repository = ParticipanteRepository(ctx)
    }

    @After
    fun tearDown() {
        grupoDao.limparTudo()
        grupoDao.close()
    }

    private fun inserir(nome: String): Participante {
        val p = Participante().apply { this.nome = nome; telefone = "11999999999" }
        repository.inserir(p, grupoId)
        return p
    }

    // =========================================================
    // inserir / listarPorGrupo
    // =========================================================

    @Test
    fun inserir_e_listarPorGrupo_retornaParticipanteInserido() {
        inserir("Ana")
        val lista = repository.listarPorGrupo(grupoId)
        assertEquals(1, lista.size)
        assertEquals("Ana", lista[0].nome)
    }

    @Test
    fun listarPorGrupo_grupoVazio_retornaListaVazia() {
        val lista = repository.listarPorGrupo(grupoId)
        assertNotNull(lista)
        assertTrue(lista.isEmpty())
    }

    @Test
    fun inserir_multiplos_listarRetornaTodos() {
        inserir("Bruno"); inserir("Carla"); inserir("Diego")
        assertEquals(3, repository.listarPorGrupo(grupoId).size)
    }

    // =========================================================
    // atualizar
    // =========================================================

    @Test
    fun atualizar_nomeAlterado_persistido() {
        inserir("Eva")
        val inserido = repository.listarPorGrupo(grupoId)[0]
        inserido.nome = "Eva Atualizada"
        assertTrue(repository.atualizar(inserido))
        assertEquals("Eva Atualizada", repository.listarPorGrupo(grupoId)[0].nome)
    }

    @Test
    fun atualizar_idInvalido_retornaFalse() {
        assertFalse(repository.atualizar(Participante().apply { id = 0; nome = "Fantasma" }))
    }

    // =========================================================
    // remover
    // =========================================================

    @Test
    fun remover_participanteExistente_removidoDaLista() {
        inserir("Felipe")
        val id = repository.listarPorGrupo(grupoId)[0].id
        repository.remover(id)
        assertTrue(repository.listarPorGrupo(grupoId).isEmpty())
    }

    // =========================================================
    // deletarTodosDoGrupo
    // =========================================================

    @Test
    fun deletarTodosDoGrupo_removeTodasEntradas() {
        inserir("Gabi"); inserir("Hugo")
        repository.deletarTodosDoGrupo(grupoId)
        assertTrue(repository.listarPorGrupo(grupoId).isEmpty())
    }

    // =========================================================
    // exclusões
    // =========================================================

    @Test
    fun adicionarExclusao_apareceCampoIdsExcluidos() {
        inserir("Iris"); inserir("João")
        val lista = repository.listarPorGrupo(grupoId)
        val id1 = lista[0].id; val id2 = lista[1].id
        repository.adicionarExclusao(id1, id2)
        val p1Apos = repository.listarPorGrupo(grupoId).first { it.id == id1 }
        assertTrue(p1Apos.idsExcluidos.contains(id2))
    }

    @Test
    fun removerExclusao_removeCorretamente() {
        inserir("Karen"); inserir("Lucas")
        val lista = repository.listarPorGrupo(grupoId)
        val id1 = lista[0].id; val id2 = lista[1].id
        repository.adicionarExclusao(id1, id2)
        repository.removerExclusao(id1, id2)
        val p1Apos = repository.listarPorGrupo(grupoId).first { it.id == id1 }
        assertFalse(p1Apos.idsExcluidos.contains(id2))
    }

    // =========================================================
    // marcarComoEnviado
    // =========================================================

    @Test
    fun marcarComoEnviado_setaFlagEnviado() {
        inserir("Maria")
        val id = repository.listarPorGrupo(grupoId)[0].id
        repository.marcarComoEnviado(id)
        assertTrue(repository.listarPorGrupo(grupoId)[0].isEnviado)
    }

    // =========================================================
    // salvarSorteio / limparSorteioDoGrupo
    // =========================================================

    @Test
    fun salvarSorteio_persisteAmigoSorteadoId() {
        inserir("Nadia"); inserir("Otto"); inserir("Paulo")
        val lista = repository.listarPorGrupo(grupoId)
        val sorteados = listOf(lista[1], lista[2], lista[0])
        assertTrue(repository.salvarSorteio(lista, sorteados))

        for (p in repository.listarPorGrupo(grupoId)) {
            assertNotNull(p.amigoSorteadoId)
            assertTrue(p.amigoSorteadoId!! > 0)
        }
    }

    @Test
    fun limparSorteioDoGrupo_resetaAmigoSorteadoEEnviado() {
        inserir("Quesia"); inserir("Rafael"); inserir("Sara")
        val lista = repository.listarPorGrupo(grupoId)
        repository.salvarSorteio(lista, listOf(lista[1], lista[2], lista[0]))
        repository.limparSorteioDoGrupo(grupoId)

        for (p in repository.listarPorGrupo(grupoId)) {
            assertNull(p.amigoSorteadoId)
            assertFalse(p.isEnviado)
        }
    }

    // =========================================================
    // getNomeAmigoSorteado
    // =========================================================

    @Test
    fun getNomeAmigoSorteado_retornaNomedoAmigoSorteado() {
        inserir("Tiago"); inserir("Vanessa")
        val lista = repository.listarPorGrupo(grupoId)
        val tiago = lista.first { it.nome == "Tiago" }
        val vanessa = lista.first { it.nome == "Vanessa" }
        repository.salvarSorteio(listOf(tiago, vanessa), listOf(vanessa, tiago))
        assertEquals("Vanessa", repository.getNomeAmigoSorteado(vanessa.id))
    }

    @Test
    fun getNomeAmigoSorteado_idInexistente_retornaNinguem() {
        assertEquals("Ninguém", repository.getNomeAmigoSorteado(99999))
    }

    // =========================================================
    // contarPorGrupo
    // =========================================================

    @Test
    fun contarPorGrupo_retornaContagemCorreta() {
        inserir("Uma"); inserir("Vitor")
        assertEquals(2, repository.contarPorGrupo()[grupoId])
    }

    // =========================================================
    // buscarPorId
    // =========================================================

    @Test
    fun buscarPorId_retornaParticipanteCorreto() {
        inserir("Wesley")
        val id = repository.listarPorGrupo(grupoId)[0].id
        assertEquals("Wesley", repository.buscarPorId(id)!!.nome)
    }

    @Test
    fun buscarPorId_idInexistente_retornaNull() {
        assertNull(repository.buscarPorId(99999))
    }
}