package activity.amigosecreto.db

import activity.amigosecreto.db.room.AppDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica que o schema do Room (AppDatabase) contém as tabelas e colunas
 * esperadas após a migração. Substitui o MySQLiteOpenHelperTest original que
 * testava o schema do helper legado — Room agora gerencia o schema.
 *
 * Usa SupportSQLiteDatabase (PRAGMA table_info) via openHelper do Room.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MySQLiteOpenHelperTest {

    private lateinit var db: AppDatabase
    private lateinit var sdb: SupportSQLiteDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        // Force schema creation by opening the database
        sdb = db.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun getTabelasExistentes(): List<String> {
        val cursor = sdb.query(
            "SELECT name FROM sqlite_master WHERE type='table' " +
            "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' " +
            "AND name NOT LIKE 'room_%'"
        )
        return cursor.use {
            val tabelas = mutableListOf<String>()
            if (it.moveToFirst()) {
                do { tabelas.add(it.getString(0)) } while (it.moveToNext())
            }
            tabelas
        }
    }

    private fun colunaExiste(tabela: String, coluna: String): Boolean {
        val cursor = sdb.query("PRAGMA table_info($tabela)")
        return cursor.use {
            if (it.moveToFirst()) {
                val nomeIdx = it.getColumnIndexOrThrow("name")
                do {
                    if (coluna == it.getString(nomeIdx)) return true
                } while (it.moveToNext())
            }
            false
        }
    }

    // --- tabelas criadas ---

    @Test
    fun onCreate_cria_tabela_grupo() {
        assertTrue(getTabelasExistentes().contains("grupo"))
    }

    @Test
    fun onCreate_cria_tabela_participante() {
        assertTrue(getTabelasExistentes().contains("participante"))
    }

    @Test
    fun onCreate_cria_tabela_exclusao() {
        assertTrue(getTabelasExistentes().contains("exclusao"))
    }

    @Test
    fun onCreate_cria_tabela_desejo() {
        assertTrue(getTabelasExistentes().contains("desejo"))
    }

    @Test
    fun onCreate_cria_tabela_sorteio() {
        assertTrue(getTabelasExistentes().contains("sorteio"))
    }

    @Test
    fun onCreate_cria_tabela_sorteio_par() {
        assertTrue(getTabelasExistentes().contains("sorteio_par"))
    }

    // --- colunas da tabela grupo ---

    @Test
    fun tabela_grupo_tem_colunas_esperadas() {
        assertTrue(colunaExiste("grupo", "id"))
        assertTrue(colunaExiste("grupo", "nome"))
        assertTrue(colunaExiste("grupo", "data"))
    }

    // --- colunas da tabela participante ---

    @Test
    fun tabela_participante_tem_colunas_esperadas() {
        assertTrue(colunaExiste("participante", "id"))
        assertTrue(colunaExiste("participante", "nome"))
        assertTrue(colunaExiste("participante", "email"))
        assertTrue(colunaExiste("participante", "telefone"))
        assertTrue(colunaExiste("participante", "amigo_sorteado_id"))
        assertTrue(colunaExiste("participante", "enviado"))
        assertTrue(colunaExiste("participante", "grupo_id"))
    }

    // --- colunas da tabela desejo ---

    @Test
    fun tabela_desejo_tem_coluna_participante_id() {
        assertTrue(colunaExiste("desejo", "participante_id"))
    }

    // --- colunas da tabela sorteio ---

    @Test
    fun tabela_sorteio_tem_colunas_esperadas() {
        assertTrue(colunaExiste("sorteio", "id"))
        assertTrue(colunaExiste("sorteio", "grupo_id"))
        assertTrue(colunaExiste("sorteio", "data_hora"))
    }

    // --- colunas da tabela sorteio_par ---

    @Test
    fun tabela_sorteio_par_tem_colunas_esperadas() {
        assertTrue(colunaExiste("sorteio_par", "sorteio_id"))
        assertTrue(colunaExiste("sorteio_par", "participante_id"))
        assertTrue(colunaExiste("sorteio_par", "sorteado_id"))
        assertTrue(colunaExiste("sorteio_par", "nome_participante"))
        assertTrue(colunaExiste("sorteio_par", "nome_sorteado"))
        assertTrue(colunaExiste("sorteio_par", "enviado"))
    }

    // --- ON DELETE CASCADE via Room: remover participante apaga exclusoes e desejos ---

    @Test
    fun cascade_remover_participante_apaga_exclusoes_associadas() = runBlocking {
        val grupoDao = db.grupoDao()
        val participanteDao = db.participanteDao()

        val grupoId = grupoDao.inserir(Grupo(nome = "G1")).toInt()
        val p1 = Participante(nome = "Alice", grupoId = grupoId)
        val p1Id = participanteDao.inserir(p1).toInt()
        val p2 = Participante(nome = "Bob", grupoId = grupoId)
        val p2Id = participanteDao.inserir(p2).toInt()
        participanteDao.inserirExclusao(Exclusao(p1Id, p2Id))

        participanteDao.remover(p1Id)

        val count = sdb.query("SELECT COUNT(*) FROM exclusao").use {
            it.moveToFirst(); it.getInt(0)
        }
        assertEquals(0, count)
    }

    @Test
    fun cascade_remover_participante_apaga_desejos_associados() = runBlocking {
        val grupoDao = db.grupoDao()
        val participanteDao = db.participanteDao()
        val desejoDao = db.desejoDao()

        val grupoId = grupoDao.inserir(Grupo(nome = "G2")).toInt()
        val p = Participante(nome = "Carlos", grupoId = grupoId)
        val pId = participanteDao.inserir(p).toInt()
        desejoDao.inserir(Desejo(produto = "Bola", participanteId = pId))
        desejoDao.inserir(Desejo(produto = "Livro", participanteId = pId))

        participanteDao.remover(pId)

        val count = sdb.query("SELECT COUNT(*) FROM desejo").use {
            it.moveToFirst(); it.getInt(0)
        }
        assertEquals(0, count)
    }

    @Test
    fun cascade_remover_grupo_apaga_sorteios_associados() = runBlocking {
        val grupoDao = db.grupoDao()
        val sorteioDao = db.sorteioDao()

        val grupo = Grupo(nome = "Grupo Cascade")
        val grupoId = grupoDao.inserir(grupo).toInt()
        grupo.id = grupoId

        // Insert sorteio directly via raw SQL to avoid needing participantes
        sdb.execSQL(
            "INSERT INTO sorteio (grupo_id, data_hora) VALUES ($grupoId, '2025-12-24 20:00:00')"
        )

        grupoDao.remover(grupo)

        val count = sdb.query("SELECT COUNT(*) FROM sorteio").use {
            it.moveToFirst(); it.getInt(0)
        }
        assertEquals(0, count)
    }
}
