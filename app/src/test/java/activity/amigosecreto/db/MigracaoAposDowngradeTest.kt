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

    @Test
    fun banco_legado_preserva_exclusoes_e_sorteios_na_migracao() {
        // Cobre a parte mais arriscada da migration: renomear e recriar `exclusao`, `sorteio`
        // e `sorteio_par`, incluindo os filtros WHERE ... IS NOT NULL e os COALESCE.
        MySQLiteOpenHelper.resetInstanceForTesting()
        ctx.deleteDatabase("amigosecreto_v10.db")
        val legado = MySQLiteOpenHelper.getInstance(ctx).writableDatabase
        val nomeLegado = java.io.File(legado.path).name

        legado.execSQL("INSERT INTO grupo (nome, data) VALUES ('Família', '01/01/2026')")
        legado.execSQL("INSERT INTO participante (nome, enviado, grupo_id) VALUES ('Ana', 0, 1)")
        legado.execSQL("INSERT INTO participante (nome, enviado, grupo_id) VALUES ('Bob', 0, 1)")
        legado.execSQL("INSERT INTO exclusao (participante_id, excluido_id) VALUES (1, 2)")
        legado.execSQL("INSERT INTO sorteio (grupo_id, data_hora) VALUES (1, '2026-03-17T19:00:00')")
        // nome_sorteado e enviado nulos no schema legado — o COALESCE precisa dar conta,
        // já que o schema do Room os exige NOT NULL.
        legado.execSQL(
            "INSERT INTO sorteio_par (sorteio_id, participante_id, sorteado_id, " +
                "nome_participante, nome_sorteado, enviado) VALUES (1, 1, 2, 'Ana', NULL, NULL)"
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

        val participantes = runBlocking { db.participanteDao().listarPorGrupo(1) }
        val ana = participantes.first { it.nome == "Ana" }
        val bob = participantes.first { it.nome == "Bob" }
        assertTrue("a exclusão de Ana→Bob não sobreviveu", ana.idsExcluidos.contains(bob.id))

        val sorteios = runBlocking { db.sorteioDao().listarPorGrupo(1) }
        assertEquals(1, sorteios.size)
        assertEquals("2026-03-17T19:00:00", sorteios[0].dataHora)
        val par = sorteios[0].pares.single()
        assertEquals("Ana", par.nomeParticipante)
        assertEquals("", par.nomeSorteado)   // NULL → "" via COALESCE
        assertFalse(par.enviado)             // NULL → 0 via COALESCE
        db.close()
        ctx.deleteDatabase(nomeLegado)
    }

    @Test
    fun estado_parcial_preserva_as_colunas_de_rastreamento_que_existem() {
        // Cenário que motivou filtrar coluna a coluna em vez de exigir as três: o comentário
        // da MIGRATION_11_12 registra dispositivos que ficaram com estado parcial. Aqui
        // `foi_notificado` é removida, deixando só confirmou_presente e observacoes.
        var db = abrirComMigrations()
        val grupoId = runBlocking {
            val gid = db.grupoDao().inserir(Grupo(nome = "Família", data = "17/03/2026")).toInt()
            db.participanteDao().inserir(
                Participante(
                    nome = "Ana", grupoId = gid,
                    confirmouPresente = true, foiNotificado = true,
                    observacoes = "Alergia a nozes",
                )
            )
            gid
        }
        db.close()

        // Reconstrói `participante` sem `foi_notificado`, mantendo as outras duas.
        val caminho = ctx.getDatabasePath(dbName).absolutePath
        SQLiteDatabase.openDatabase(caminho, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
            raw.execSQL("ALTER TABLE participante RENAME TO p_tmp")
            raw.execSQL(
                "CREATE TABLE participante (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`nome` TEXT, `email` TEXT, `telefone` TEXT, `amigo_sorteado_id` INTEGER, " +
                    "`enviado` INTEGER NOT NULL DEFAULT 0, `grupo_id` INTEGER NOT NULL DEFAULT 0, " +
                    "`confirmou_presente` INTEGER NOT NULL DEFAULT 0, `observacoes` TEXT)"
            )
            raw.execSQL(
                "INSERT INTO participante (id, nome, email, telefone, amigo_sorteado_id, " +
                    "enviado, grupo_id, confirmou_presente, observacoes) " +
                    "SELECT id, nome, email, telefone, amigo_sorteado_id, enviado, grupo_id, " +
                    "confirmou_presente, observacoes FROM p_tmp"
            )
            raw.execSQL("DROP TABLE p_tmp")
            raw.version = 10
        }

        db = abrirComMigrations()
        val p = runBlocking { db.participanteDao().listarPorGrupoSemExclusoes(grupoId).single() }
        assertTrue("confirmou_presente existia e deveria ter sido preservada", p.confirmouPresente)
        assertEquals("observacoes existia e deveria ter sido preservada", "Alergia a nozes", p.observacoes)
        assertFalse("foi_notificado não existia; assume o default", p.foiNotificado)
        db.close()
    }
}
