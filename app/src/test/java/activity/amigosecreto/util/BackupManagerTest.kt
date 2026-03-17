package activity.amigosecreto.util

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
        abrirDaos()
    }

    @After
    fun tearDown() {
        abrirDaos() // garantir que estão abertos para limpar
        grupoDao.limparTudo()
        fecharDaos()
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
        assertEquals(MySQLiteOpenHelper.DATABASE_VERSION_PUBLIC, root.getInt("schema_version"))
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
        val futureVersion = MySQLiteOpenHelper.DATABASE_VERSION_PUBLIC + 1
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
}
