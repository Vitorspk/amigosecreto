package activity.amigosecreto.util

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.DesejoDAO
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.MySQLiteOpenHelper
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.ParticipanteDAO
import activity.amigosecreto.db.SorteioDAO
import activity.amigosecreto.db.room.AppDatabase

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BackupManagerTest {

    private lateinit var ctx: android.app.Application
    private lateinit var grupoDao: GrupoDAO
    private lateinit var participanteDao: ParticipanteDAO
    private lateinit var desejoDao: DesejoDAO
    private lateinit var sorteioDao: SorteioDAO

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        // Inicializa o AppDatabase no contexto Robolectric para que BackupManager.importarDeJson
        // possa usar sua conexão em vez de abrir uma conexão paralela via MySQLiteOpenHelper.
        AppDatabase.initForTesting(ctx)
        abrirDaos()
    }

    @After
    fun tearDown() {
        abrirDaos() // garantir que estão abertos para limpar
        grupoDao.limparTudo()
        fecharDaos()
        AppDatabase.closeForTesting()
        MySQLiteOpenHelper.resetInstanceForTesting()
    }

    private fun abrirDaos() {
        grupoDao = GrupoDAO(ctx)
        grupoDao.open()
        participanteDao = ParticipanteDAO(ctx)
        participanteDao.open()
        desejoDao = DesejoDAO(ctx)
        desejoDao.open()
        sorteioDao = SorteioDAO(ctx)
        sorteioDao.open()
    }

    private fun fecharDaos() {
        grupoDao.close()
        participanteDao.close()
        desejoDao.close()
        sorteioDao.close()
    }

    /** Fecha os DAOs, executa o bloco (import/export), e reabre os DAOs para verificações. */
    private fun <T> semDaosAbertos(block: () -> T): T {
        fecharDaos()
        return try { block() } finally { abrirDaos() }
    }

    // --- Helpers ---

    private fun criarGrupo(nome: String): Grupo {
        val g = Grupo(); g.nome = nome; g.data = "17/03/2026"
        g.id = grupoDao.inserir(g).toInt()
        return g
    }

    private fun criarParticipante(nome: String, grupoId: Int): Participante {
        val p = Participante(); p.nome = nome
        participanteDao.inserir(p, grupoId)
        return p
    }

    // --- Exportar ---

    @Test
    fun exportar_banco_vazio_retorna_json_valido_com_grupos_vazio() {
        val json = BackupManager.exportarParaJson(ctx)
        assertTrue(json.contains("\"grupos\""))
        assertTrue(json.contains("\"version\""))
        assertTrue(json.contains("\"schema_version\""))
        val root = org.json.JSONObject(json)
        assertEquals(0, root.getJSONArray("grupos").length())
    }

    @Test
    fun exportar_inclui_schema_version_correto() {
        val json = BackupManager.exportarParaJson(ctx)
        val root = org.json.JSONObject(json)
        // O backup carrega dados do schema gerenciado pelo Room (v13+), não da versão
        // congelada do MySQLiteOpenHelper (10). Ver AppDatabase.SCHEMA_VERSION.
        assertEquals(AppDatabase.SCHEMA_VERSION, root.getInt("schema_version"))
    }

    @Test
    fun exportar_declara_backup_version_2() {
        val root = org.json.JSONObject(BackupManager.exportarParaJson(ctx))
        assertEquals(2, root.getInt("version"))
    }

    @Test
    fun exportar_inclui_timestamp_no_json() {
        val json = BackupManager.exportarParaJson(ctx)
        val root = org.json.JSONObject(json)
        val exportedAt = root.optString("exported_at", "")
        assertTrue("exported_at deve estar presente", exportedAt.isNotEmpty())
        assertTrue("formato ISO esperado", exportedAt.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
    }

    @Test
    fun exportar_um_grupo_serializa_nome_e_data() {
        criarGrupo("Família")
        val root = org.json.JSONObject(BackupManager.exportarParaJson(ctx))
        val grupos = root.getJSONArray("grupos")
        assertEquals(1, grupos.length())
        val g = grupos.getJSONObject(0)
        assertEquals("Família", g.getString("nome"))
        assertEquals("17/03/2026", g.getString("data"))
    }

    @Test
    fun exportar_grupo_com_participantes_inclui_todos() {
        val g = criarGrupo("Trabalho")
        criarParticipante("Ana", g.id)
        criarParticipante("Bob", g.id)

        val root = org.json.JSONObject(BackupManager.exportarParaJson(ctx))
        val participantes = root.getJSONArray("grupos").getJSONObject(0).getJSONArray("participantes")
        assertEquals(2, participantes.length())
        val nomes = (0 until participantes.length()).map { participantes.getJSONObject(it).getString("nome") }
        assertTrue(nomes.contains("Ana"))
        assertTrue(nomes.contains("Bob"))
    }

    @Test
    fun exportar_grupo_com_exclusoes_inclui_lista() {
        val g = criarGrupo("Amigos")
        val p1 = criarParticipante("P1", g.id)
        val p2 = criarParticipante("P2", g.id)
        participanteDao.adicionarExclusao(p1.id, p2.id)

        val root = org.json.JSONObject(BackupManager.exportarParaJson(ctx))
        val parts = root.getJSONArray("grupos").getJSONObject(0).getJSONArray("participantes")
        val p1Json = (0 until parts.length()).map { parts.getJSONObject(it) }
            .first { it.getString("nome") == "P1" }
        val exclusoes = p1Json.getJSONArray("exclusoes")
        assertEquals(1, exclusoes.length())
        assertEquals(p2.id, exclusoes.getInt(0))
    }

    @Test
    fun exportar_grupo_com_sorteio_inclui_historico() {
        val g = criarGrupo("Sorteio")
        val p1 = criarParticipante("Ana", g.id)
        val p2 = criarParticipante("Bob", g.id)
        val sorteioId = sorteioDao.inserirSorteio(g.id, "2026-03-17T19:00:00")
        sorteioDao.inserirPar(sorteioId, p1.id, p2.id, "Ana", "Bob", 0)

        val root = org.json.JSONObject(BackupManager.exportarParaJson(ctx))
        val sorteios = root.getJSONArray("grupos").getJSONObject(0).getJSONArray("sorteios")
        assertEquals(1, sorteios.length())
        val pares = sorteios.getJSONObject(0).getJSONArray("pares")
        assertEquals(1, pares.length())
        assertEquals("Ana", pares.getJSONObject(0).getString("nome_participante"))
        assertEquals("Bob", pares.getJSONObject(0).getString("nome_sorteado"))
    }

    @Test
    fun exportar_grupo_com_desejos_inclui_desejos() {
        val g = criarGrupo("Desejos")
        val p = criarParticipante("Ana", g.id)
        val d = Desejo(); d.produto = "Livro"; d.participanteId = p.id
        desejoDao.inserir(d)

        val root = org.json.JSONObject(BackupManager.exportarParaJson(ctx))
        val parts = root.getJSONArray("grupos").getJSONObject(0).getJSONArray("participantes")
        val desejos = parts.getJSONObject(0).getJSONArray("desejos")
        assertEquals(1, desejos.length())
        assertEquals("Livro", desejos.getJSONObject(0).getString("produto"))
    }

    // --- Importar ---

    @Test
    fun importar_json_malformado_retorna_failure() {
        val result = semDaosAbertos { BackupManager.importarDeJson(ctx, "{ isso nao e json valido") }
        assertTrue(result is BackupManager.ImportResult.Failure)
    }

    @Test
    fun importar_json_sem_version_retorna_failure() {
        val json = """{"schema_version":10,"grupos":[]}"""
        val result = semDaosAbertos { BackupManager.importarDeJson(ctx, json) }
        assertTrue(result is BackupManager.ImportResult.Failure)
    }

    @Test
    fun importar_json_schema_version_maior_que_atual_retorna_failure() {
        val futureVersion = AppDatabase.SCHEMA_VERSION + 1
        val json = """{"version":1,"schema_version":$futureVersion,"grupos":[]}"""
        val result = semDaosAbertos { BackupManager.importarDeJson(ctx, json) }
        assertTrue(result is BackupManager.ImportResult.Failure)
    }

    @Test
    fun importar_json_valido_sem_grupos_retorna_success_zero() {
        val json = """{"version":1,"schema_version":10,"grupos":[]}"""
        val result = semDaosAbertos { BackupManager.importarDeJson(ctx, json) }
        assertTrue(result is BackupManager.ImportResult.Success)
        assertEquals(0, (result as BackupManager.ImportResult.Success).gruposImportados)
    }

    @Test
    fun importar_json_valido_restaura_grupos() {
        val json = """{"version":1,"schema_version":10,"grupos":[
            {"nome":"Família","data":"17/03/2026","participantes":[],"sorteios":[]},
            {"nome":"Trabalho","data":"17/03/2026","participantes":[],"sorteios":[]}
        ]}"""
        val result = semDaosAbertos { BackupManager.importarDeJson(ctx, json) }
        assertTrue(result is BackupManager.ImportResult.Success)
        assertEquals(2, (result as BackupManager.ImportResult.Success).gruposImportados)
        val grupos = grupoDao.listar()
        assertEquals(2, grupos.size)
        assertTrue(grupos.any { it.nome == "Família" })
        assertTrue(grupos.any { it.nome == "Trabalho" })
    }

    @Test
    fun importar_json_valido_restaura_participantes() {
        val json = """{"version":1,"schema_version":10,"grupos":[{
            "nome":"G","data":"","participantes":[
                {"id":1,"nome":"Ana","email":"ana@x.com","telefone":"11999","amigo_sorteado_id":0,"enviado":0,"exclusoes":[],"desejos":[]},
                {"id":2,"nome":"Bob","email":"","telefone":"","amigo_sorteado_id":0,"enviado":0,"exclusoes":[],"desejos":[]}
            ],"sorteios":[]}]}"""
        semDaosAbertos { BackupManager.importarDeJson(ctx, json) }
        val grupos = grupoDao.listar()
        val participantes = participanteDao.listarPorGrupo(grupos[0].id)
        assertEquals(2, participantes.size)
        assertTrue(participantes.any { it.nome == "Ana" && it.email == "ana@x.com" })
        assertTrue(participantes.any { it.nome == "Bob" })
    }

    @Test
    fun importar_limpa_dados_anteriores() {
        criarGrupo("Antigo")
        assertEquals(1, grupoDao.listar().size)

        val json = """{"version":1,"schema_version":10,"grupos":[
            {"nome":"Novo","data":"","participantes":[],"sorteios":[]}
        ]}"""
        semDaosAbertos { BackupManager.importarDeJson(ctx, json) }

        val grupos = grupoDao.listar()
        assertEquals(1, grupos.size)
        assertEquals("Novo", grupos[0].nome)
    }

    @Test
    fun importar_remapeia_amigo_sorteado_id_corretamente() {
        val json = """{"version":1,"schema_version":10,"grupos":[{
            "nome":"G","data":"","participantes":[
                {"id":10,"nome":"Ana","email":"","telefone":"","amigo_sorteado_id":20,"enviado":0,"exclusoes":[],"desejos":[]},
                {"id":20,"nome":"Bob","email":"","telefone":"","amigo_sorteado_id":10,"enviado":0,"exclusoes":[],"desejos":[]}
            ],"sorteios":[]}]}"""
        semDaosAbertos { BackupManager.importarDeJson(ctx, json) }

        val grupos = grupoDao.listar()
        val participantes = participanteDao.listarPorGrupo(grupos[0].id)
        val ana = participantes.first { it.nome == "Ana" }
        val bob = participantes.first { it.nome == "Bob" }
        // amigo_sorteado_id deve ser remapeado para os novos IDs
        assertEquals(bob.id, ana.amigoSorteadoId)
        assertEquals(ana.id, bob.amigoSorteadoId)
    }

    @Test
    fun importar_remapeia_exclusoes_corretamente() {
        val json = """{"version":1,"schema_version":10,"grupos":[{
            "nome":"G","data":"","participantes":[
                {"id":1,"nome":"Ana","email":"","telefone":"","amigo_sorteado_id":0,"enviado":0,"exclusoes":[2],"desejos":[]},
                {"id":2,"nome":"Bob","email":"","telefone":"","amigo_sorteado_id":0,"enviado":0,"exclusoes":[],"desejos":[]}
            ],"sorteios":[]}]}"""
        semDaosAbertos { BackupManager.importarDeJson(ctx, json) }

        val grupos = grupoDao.listar()
        val participantes = participanteDao.listarPorGrupo(grupos[0].id)
        val ana = participantes.first { it.nome == "Ana" }
        val bob = participantes.first { it.nome == "Bob" }
        assertTrue("Ana deve excluir Bob", ana.idsExcluidos.contains(bob.id))
    }

    @Test
    fun roundtrip_exportar_importar_preserva_dados() {
        val g = criarGrupo("Original")
        val p1 = criarParticipante("Ana", g.id)
        val p2 = criarParticipante("Bob", g.id)
        participanteDao.adicionarExclusao(p1.id, p2.id)
        val d = Desejo(); d.produto = "Presente"; d.participanteId = p1.id; desejoDao.inserir(d)
        val sid = sorteioDao.inserirSorteio(g.id, "2026-03-17T19:00:00")
        sorteioDao.inserirPar(sid, p1.id, p2.id, "Ana", "Bob", 0)

        val json = semDaosAbertos { BackupManager.exportarParaJson(ctx) }
        val result = semDaosAbertos { BackupManager.importarDeJson(ctx, json) }

        assertTrue(result is BackupManager.ImportResult.Success)
        val grupos = grupoDao.listar()
        assertEquals(1, grupos.size)
        assertEquals("Original", grupos[0].nome)
        val participantes = participanteDao.listarPorGrupo(grupos[0].id)
        assertEquals(2, participantes.size)
        val ana = participantes.first { it.nome == "Ana" }
        val bob = participantes.first { it.nome == "Bob" }
        assertTrue(ana.idsExcluidos.contains(bob.id))
        val desejos = desejoDao.listarPorParticipante(ana.id)
        assertEquals(1, desejos.size)
        assertEquals("Presente", desejos[0].produto)
        val sorteios = sorteioDao.listarPorGrupo(grupos[0].id)
        assertEquals(1, sorteios.size)
        assertEquals(1, sorteios[0].pares.size)
    }

    // --- Configurações do grupo e rastreamento do participante (colunas v12) ---

    /** Insere um grupo com TODAS as colunas de configuração v12 preenchidas, via Room. */
    private fun criarGrupoConfigurado(): Grupo {
        val g = Grupo(
            nome = "Família",
            data = "17/03/2026",
            descricao = "Ceia de Natal",
            dataEvento = "24/12/2026",
            localEvento = "Casa da vovó",
            dataLimiteSorteio = "20/12/2026",
            valorMinimo = 50.0,
            valorMaximo = 150.0,
            regras = "Sem meias",
            permitirVerDesejos = false,
            exigirConfirmacaoCompra = true,
        )
        g.id = kotlinx.coroutines.runBlocking {
            AppDatabase.getInstance(ctx).grupoDao().inserir(g).toInt()
        }
        return g
    }

    private fun lerGrupoViaRoom(): Grupo = kotlinx.coroutines.runBlocking {
        AppDatabase.getInstance(ctx).grupoDao().listar().single()
    }

    @Test
    fun exportar_grupo_inclui_configuracoes_v12() {
        criarGrupoConfigurado()
        val root = org.json.JSONObject(semDaosAbertos { BackupManager.exportarParaJson(ctx) })
        val g = root.getJSONArray("grupos").getJSONObject(0)

        assertEquals("Ceia de Natal", g.getString("descricao"))
        assertEquals("24/12/2026", g.getString("data_evento"))
        assertEquals("Casa da vovó", g.getString("local_evento"))
        assertEquals("20/12/2026", g.getString("data_limite_sorteio"))
        assertEquals(50.0, g.getDouble("valor_minimo"), 0.001)
        assertEquals(150.0, g.getDouble("valor_maximo"), 0.001)
        assertEquals("Sem meias", g.getString("regras"))
        assertEquals(0, g.getInt("permitir_ver_desejos"))
        assertEquals(1, g.getInt("exigir_confirmacao_compra"))
    }

    @Test
    fun roundtrip_preserva_configuracoes_do_grupo() {
        criarGrupoConfigurado()

        val json = semDaosAbertos { BackupManager.exportarParaJson(ctx) }
        val result = semDaosAbertos { BackupManager.importarDeJson(ctx, json) }
        assertTrue("import falhou: $result", result is BackupManager.ImportResult.Success)

        val g = lerGrupoViaRoom()
        assertEquals("Ceia de Natal", g.descricao)
        assertEquals("24/12/2026", g.dataEvento)
        assertEquals("Casa da vovó", g.localEvento)
        assertEquals("20/12/2026", g.dataLimiteSorteio)
        assertEquals(50.0, g.valorMinimo, 0.001)
        assertEquals(150.0, g.valorMaximo, 0.001)
        assertEquals("Sem meias", g.regras)
        // permitirVerDesejos=false é justamente o caso perigoso: o default da coluna é 1,
        // então perder o campo reabriria os desejos de um grupo que os havia ocultado.
        assertFalse(g.permitirVerDesejos)
        assertTrue(g.exigirConfirmacaoCompra)
    }

    @Test
    fun roundtrip_preserva_null_nos_campos_opcionais_do_grupo() {
        // A UI grava null (não "") para campos de texto vazios — ver
        // ConfiguracoesGrupoActivity.salvar(): takeIf { it.isNotEmpty() }.
        val g = Grupo(nome = "Simples", data = "17/03/2026")
        kotlinx.coroutines.runBlocking { AppDatabase.getInstance(ctx).grupoDao().inserir(g) }

        val json = semDaosAbertos { BackupManager.exportarParaJson(ctx) }
        assertTrue(semDaosAbertos { BackupManager.importarDeJson(ctx, json) }
            is BackupManager.ImportResult.Success)

        val lido = lerGrupoViaRoom()
        assertNull(lido.descricao)
        assertNull(lido.dataEvento)
        assertNull(lido.localEvento)
        assertNull(lido.dataLimiteSorteio)
        assertNull(lido.regras)
    }

    @Test
    fun roundtrip_preserva_rastreamento_do_participante() {
        val g = criarGrupoConfigurado()
        kotlinx.coroutines.runBlocking {
            AppDatabase.getInstance(ctx).participanteDao().inserir(
                Participante(
                    nome = "Ana",
                    grupoId = g.id,
                    confirmouPresente = true,
                    foiNotificado = true,
                    observacoes = "Alergia a nozes",
                )
            )
        }

        val json = semDaosAbertos { BackupManager.exportarParaJson(ctx) }
        assertTrue(semDaosAbertos { BackupManager.importarDeJson(ctx, json) }
            is BackupManager.ImportResult.Success)

        val p = kotlinx.coroutines.runBlocking {
            AppDatabase.getInstance(ctx).participanteDao().listarPorGrupoSemExclusoes(
                lerGrupoViaRoom().id
            ).single()
        }
        assertTrue(p.confirmouPresente)
        assertTrue(p.foiNotificado)
        assertEquals("Alergia a nozes", p.observacoes)
    }

    @Test
    fun importar_formato_antigo_sem_campos_v12_aplica_defaults() {
        // Backup gerado pela versão anterior do app: version=1, schema_version=10,
        // sem nenhuma das colunas v12. Deve importar aplicando os defaults do schema.
        val antigo = """
            {
              "version": 1,
              "schema_version": 10,
              "exported_at": "2026-03-17T14:30:00",
              "grupos": [
                {
                  "id": 1, "nome": "Antigo", "data": "01/01/2026",
                  "participantes": [
                    { "id": 10, "nome": "Ana", "email": "", "telefone": "",
                      "amigo_sorteado_id": 0, "enviado": 0,
                      "exclusoes": [], "desejos": [] }
                  ],
                  "sorteios": []
                }
              ]
            }
        """.trimIndent()

        val result = semDaosAbertos { BackupManager.importarDeJson(ctx, antigo) }
        assertTrue("import de formato antigo falhou: $result",
            result is BackupManager.ImportResult.Success)

        val g = lerGrupoViaRoom()
        assertEquals("Antigo", g.nome)
        assertTrue("default da coluna permitir_ver_desejos é 1", g.permitirVerDesejos)
        assertFalse("default da coluna exigir_confirmacao_compra é 0", g.exigirConfirmacaoCompra)
        assertEquals(0.0, g.valorMinimo, 0.001)
        assertNull(g.descricao)

        val p = kotlinx.coroutines.runBlocking {
            AppDatabase.getInstance(ctx).participanteDao()
                .listarPorGrupoSemExclusoes(g.id).single()
        }
        assertFalse(p.confirmouPresente)
        assertFalse(p.foiNotificado)
        assertNull(p.observacoes)
    }
}
