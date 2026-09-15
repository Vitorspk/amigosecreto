package activity.amigosecreto.db

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import activity.amigosecreto.db.room.AppDatabase

/**
 * Reproduz o cenário deixado em produção pelo bug de downgrade do `user_version`.
 *
 * Até a v3.1, abrir a lista de desejos de um participante (ou usar backup) fazia o
 * `MySQLiteOpenHelper` — congelado em `DATABASE_VERSION = 10` — rebaixar o `user_version` do
 * arquivo de 13 para 10. O **schema em disco continuava completo**, com as colunas da v12 e os
 * dados; só o carimbo ficou errado.
 *
 * Na atualização, o Room lê 10 e roda a `MIGRATION_10_11`, que recria `participante`. Se ela
 * copiar apenas as colunas da v10, o rastreamento do participante é descartado justamente na
 * subida para a versão que corrige o bug.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigracaoAposDowngradeTest {

    private lateinit var ctx: Application
    private val dbName = "migracao_downgrade_test.db"

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        ctx.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        ctx.deleteDatabase(dbName)
        MySQLiteOpenHelper.resetInstanceForTesting()
        ctx.deleteDatabase("amigosecreto_v10.db")
    }

    private fun abrirComMigrations(): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
            )
            .allowMainThreadQueries()
            .build()

    /** Rebaixa o carimbo de versão sem tocar no schema — o que o helper legado fazia. */
    private fun rebaixarUserVersionPara10() {
        val caminho = ctx.getDatabasePath(dbName).absolutePath
        SQLiteDatabase.openDatabase(caminho, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
            raw.version = 10
        }
    }

    @Test
    fun migracao_apos_downgrade_preserva_rastreamento_do_participante() {
        // 1. Banco íntegro na v13, com rastreamento preenchido.
        var db = abrirComMigrations()
        val grupoId = runBlocking {
            val gid = db.grupoDao().inserir(Grupo(nome = "Família", data = "17/03/2026")).toInt()
            db.participanteDao().inserir(
                Participante(
                    nome = "Ana",
                    grupoId = gid,
                    confirmouPresente = true,
                    foiNotificado = true,
                    observacoes = "Alergia a nozes",
                )
            )
            gid
        }
        db.close()

        // 2. O carimbo é rebaixado; o schema e os dados continuam intactos no arquivo.
        rebaixarUserVersionPara10()

        // 3. Atualização: o Room vê 10 e roda as migrations.
        db = abrirComMigrations()
        val p = runBlocking { db.participanteDao().listarPorGrupoSemExclusoes(grupoId).single() }

        assertEquals("Ana", p.nome)
        assertTrue(
            "confirmou_presente foi perdido na MIGRATION_10_11 durante a atualização",
            p.confirmouPresente
        )
        assertTrue(
            "foi_notificado foi perdido na MIGRATION_10_11 durante a atualização",
            p.foiNotificado
        )
        assertEquals(
            "observacoes foi perdido na MIGRATION_10_11 durante a atualização",
            "Alergia a nozes", p.observacoes
        )
        db.close()
    }

    @Test
    fun migracao_apos_downgrade_preserva_configuracao_do_grupo() {
        // `grupo` não é recriado pela MIGRATION_10_11 (só recebe ALTER na 11_12), então a
        // configuração deveria sobreviver. Teste de regressão para essa suposição.
        var db = abrirComMigrations()
        runBlocking {
            db.grupoDao().inserir(
                Grupo(
                    nome = "Trabalho",
                    data = "17/03/2026",
                    descricao = "Ceia",
                    valorMinimo = 50.0,
                    permitirVerDesejos = false,
                    exigirConfirmacaoCompra = true,
                )
            )
        }
        db.close()

        rebaixarUserVersionPara10()

        db = abrirComMigrations()
        val g = runBlocking { db.grupoDao().listar().single() }
        assertEquals("Ceia", g.descricao)
        assertEquals(50.0, g.valorMinimo, 0.001)
        assertFalse(g.permitirVerDesejos)
        assertTrue(g.exigirConfirmacaoCompra)
        db.close()
    }

    @Test
    fun banco_genuinamente_v10_ainda_migra_sem_as_colunas_de_rastreamento() {
        // Caminho oposto ao dos testes acima: instalação antiga de verdade, criada pelo
        // MySQLiteOpenHelper na v10, onde as colunas da v12 NÃO existem. A preservação
        // condicional não pode quebrar essa migração — é o caminho da base legada.
        // Arquivos de banco persistem entre testes no Robolectric — e um teste que falha antes
        // do cleanup deixa lixo que mascara o cenário. Isolamento explícito.
        MySQLiteOpenHelper.resetInstanceForTesting()
        ctx.deleteDatabase("amigosecreto_v10.db")
        val helper = MySQLiteOpenHelper.getInstance(ctx)
        val legado = helper.writableDatabase
        val nomeLegado = java.io.File(legado.path).name
        assertEquals(10, legado.version)
        assertFalse(
            "o banco do helper legado não deveria ter colunas da v12",
            temColuna(legado, "participante", "confirmou_presente")
        )
        legado.execSQL("INSERT INTO grupo (nome, data) VALUES ('Antigo', '01/01/2026')")
        legado.execSQL(
            "INSERT INTO participante (nome, email, telefone, enviado, grupo_id) " +
                "VALUES ('Bob', 'b@x.com', '11999', 1, 1)"
        )
        MySQLiteOpenHelper.resetInstanceForTesting()

        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, nomeLegado)
            .addMigrations(
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
            )
            .allowMainThreadQueries()
            .build()

        // A abertura só conclui se as migrations rodarem e o schema final validar na v13.
        val p = runBlocking { db.participanteDao().listarPorGrupoSemExclusoes(1).single() }
        assertEquals("Bob", p.nome)
        assertEquals("b@x.com", p.email)
        assertTrue(p.isEnviado)
        // Colunas da v12 chegam com o default, porque nunca existiram neste banco.
        assertFalse(p.confirmouPresente)
        assertFalse(p.foiNotificado)
        db.close()
        ctx.deleteDatabase(nomeLegado)
    }

    private fun temColuna(db: SQLiteDatabase, tabela: String, coluna: String): Boolean =
        db.rawQuery("PRAGMA table_info(`$tabela`)", null).use { c ->
            val idx = c.getColumnIndex("name")
            while (c.moveToNext()) if (c.getString(idx) == coluna) return true
            false
        }
}
