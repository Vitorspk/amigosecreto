package activity.amigosecreto.repository

import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.db.room.GrupoRoomDao
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
 * Testes de integração de GruposRepository contra Room in-memory.
 *
 * Valida que o repositório delega corretamente ao DAO e que os
 * contratos de retorno (Long, Boolean, List) estão corretos.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GruposRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: GrupoRoomDao
    private lateinit var repo: GruposRepository

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = db.grupoDao()
        repo = GruposRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // =========================================================
    // listar
    // =========================================================

    @Test
    fun listar_retorna_lista_vazia_quando_nao_ha_grupos() = runTest {
        val lista = repo.listar()
        assertTrue(lista.isEmpty())
    }

    @Test
    fun listar_retorna_grupos_inseridos_em_ordem_decrescente_de_id() = runTest {
        repo.inserir(Grupo(nome = "A"))
        repo.inserir(Grupo(nome = "B"))
        repo.inserir(Grupo(nome = "C"))

        val lista = repo.listar()

        assertEquals(3, lista.size)
        // DAO retorna por id DESC — "C" deve vir primeiro
        assertEquals("C", lista[0].nome)
        assertEquals("B", lista[1].nome)
        assertEquals("A", lista[2].nome)
    }

    // =========================================================
    // inserir
    // =========================================================

    @Test
    fun inserir_retorna_id_positivo() = runTest {
        val id = repo.inserir(Grupo(nome = "Amigos"))
        assertTrue(id > 0)
    }

    @Test
    fun inserir_persiste_grupo_recuperavel_via_listar() = runTest {
        repo.inserir(Grupo(nome = "Familia"))

        val lista = repo.listar()

        assertEquals(1, lista.size)
        assertEquals("Familia", lista[0].nome)
    }

    @Test
    fun inserir_grupos_multiplos_todos_persistidos() = runTest {
        repeat(5) { i -> repo.inserir(Grupo(nome = "Grupo $i")) }

        assertEquals(5, repo.listar().size)
    }

    // =========================================================
    // atualizar
    // =========================================================

    @Test
    fun atualizar_retorna_true_para_grupo_existente() = runTest {
        val id = repo.inserir(Grupo(nome = "Original")).toInt()
        val grupo = repo.listar().first { it.id == id }
        grupo.nome = "Atualizado"

        val resultado = repo.atualizar(grupo)

        assertTrue(resultado)
    }

    @Test
    fun atualizar_persiste_novo_nome() = runTest {
        val id = repo.inserir(Grupo(nome = "Original")).toInt()
        val grupo = repo.listar().first { it.id == id }
        grupo.nome = "Novo Nome"

        repo.atualizar(grupo)

        val atualizado = repo.listar().first { it.id == id }
        assertEquals("Novo Nome", atualizado.nome)
    }

    @Test
    fun atualizar_retorna_false_para_grupo_inexistente() = runTest {
        val grupoFantasma = Grupo(id = 9999, nome = "Fantasma")

        val resultado = repo.atualizar(grupoFantasma)

        assertFalse(resultado)
    }

    // =========================================================
    // remover
    // =========================================================

    @Test
    fun remover_elimina_grupo_da_lista() = runTest {
        val id = repo.inserir(Grupo(nome = "Temporario")).toInt()
        val grupo = repo.listar().first { it.id == id }

        repo.remover(grupo)

        assertTrue(repo.listar().none { it.id == id })
    }

    @Test
    fun remover_um_de_varios_mantem_os_demais() = runTest {
        val idA = repo.inserir(Grupo(nome = "A")).toInt()
        repo.inserir(Grupo(nome = "B"))
        val grupoA = repo.listar().first { it.id == idA }

        repo.remover(grupoA)

        val restantes = repo.listar()
        assertEquals(1, restantes.size)
        assertEquals("B", restantes[0].nome)
    }

    // =========================================================
    // limparTudo
    // =========================================================

    @Test
    fun limparTudo_remove_todos_os_grupos() = runTest {
        repo.inserir(Grupo(nome = "A"))
        repo.inserir(Grupo(nome = "B"))
        repo.inserir(Grupo(nome = "C"))

        repo.limparTudo()

        assertTrue(repo.listar().isEmpty())
    }

    @Test
    fun limparTudo_em_banco_vazio_nao_lanca_excecao() = runTest {
        // Não deve lançar exceção
        repo.limparTudo()
        assertTrue(repo.listar().isEmpty())
    }
}
