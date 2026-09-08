package activity.amigosecreto.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import timber.log.Timber
import org.json.JSONArray
import org.json.JSONObject
import activity.amigosecreto.db.DesejoDAO
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.MySQLiteOpenHelper
import activity.amigosecreto.db.ParticipanteDAO
import activity.amigosecreto.db.SorteioDAO
import activity.amigosecreto.db.room.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitário stateless para exportar e importar todos os dados do app em formato JSON.
 *
 * Formato do arquivo:
 * ```json
 * {
 *   "version": 2,
 *   "schema_version": 13,
 *   "exported_at": "2026-03-17T14:30:00",
 *   "grupos": [...]
 * }
 * ```
 *
 * ## Versões do formato
 *
 * - **1** — grupo com `nome`/`data`; participante com `nome`/`email`/`telefone`/
 *   `amigo_sorteado_id`/`enviado`. As colunas de configuração da v12 do schema não eram
 *   gravadas, então um ciclo exportar/importar as perdia silenciosamente.
 * - **2** — adiciona as colunas de configuração do grupo e de rastreamento do participante
 *   introduzidas na v12. Arquivos da versão 1 continuam sendo importados: os campos ausentes
 *   assumem os defaults do schema.
 *
 * Campos de texto opcionais são **omitidos** quando nulos, em vez de serem gravados como `""`.
 * A UI grava `null` para campos vazios (ver `ConfiguracoesGrupoActivity.salvar()`), então
 * omitir preserva a distinção e mantém o ciclo exportar/importar uma identidade.
 *
 * Todos os métodos são síncronos e devem ser chamados a partir de uma thread de background.
 */
object BackupManager {

    private const val TAG = "BackupManager"
    private const val BACKUP_VERSION = 2

    /** Grava [valor] em [chave] apenas se não for nulo — preserva null vs "" no round-trip. */
    private fun JSONObject.putSeNaoNulo(chave: String, valor: String?) {
        if (valor != null) put(chave, valor)
    }

    /** Lê uma string opcional: chave ausente significa `null` (e não `""`). */
    private fun JSONObject.optStringOuNulo(chave: String): String? =
        if (has(chave) && !isNull(chave)) getString(chave) else null

    fun exportarParaJson(context: Context): String {
        val grupoDao = GrupoDAO(context)
        val participanteDao = ParticipanteDAO(context)
        val desejoDao = DesejoDAO(context)
        val sorteioDao = SorteioDAO(context)

        grupoDao.open()
        participanteDao.open()
        desejoDao.open()
        sorteioDao.open()
        return try {
            val root = JSONObject()
            root.put("version", BACKUP_VERSION)
            // Os dados vêm do schema gerenciado pelo Room, não da versão congelada
            // do MySQLiteOpenHelper (10) — ver AppDatabase.SCHEMA_VERSION.
            root.put("schema_version", AppDatabase.SCHEMA_VERSION)
            root.put("exported_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()))

            val gruposJson = JSONArray()
            for (grupo in grupoDao.listar()) {
                val gJson = JSONObject()
                gJson.put("id", grupo.id)
                gJson.put("nome", grupo.nome ?: "")
                gJson.put("data", grupo.data ?: "")

                // Configuração do grupo (colunas da v12)
                gJson.putSeNaoNulo("descricao", grupo.descricao)
                gJson.putSeNaoNulo("data_evento", grupo.dataEvento)
                gJson.putSeNaoNulo("local_evento", grupo.localEvento)
                gJson.putSeNaoNulo("data_limite_sorteio", grupo.dataLimiteSorteio)
                gJson.put("valor_minimo", grupo.valorMinimo)
                gJson.put("valor_maximo", grupo.valorMaximo)
                gJson.putSeNaoNulo("regras", grupo.regras)
                gJson.put("permitir_ver_desejos", if (grupo.permitirVerDesejos) 1 else 0)
                gJson.put("exigir_confirmacao_compra", if (grupo.exigirConfirmacaoCompra) 1 else 0)

                // Participantes (listarPorGrupo já inclui exclusões via idsExcluidos)
                val participantes = participanteDao.listarPorGrupo(grupo.id)
                val partJson = JSONArray()
                for (p in participantes) {
                    val pJson = JSONObject()
                    pJson.put("id", p.id)
                    pJson.put("nome", p.nome ?: "")
                    pJson.put("email", p.email ?: "")
                    pJson.put("telefone", p.telefone ?: "")
                    pJson.put("amigo_sorteado_id", p.amigoSorteadoId ?: 0)
                    pJson.put("enviado", if (p.isEnviado) 1 else 0)

                    // Rastreamento do participante (colunas da v12)
                    pJson.put("confirmou_presente", if (p.confirmouPresente) 1 else 0)
                    pJson.put("foi_notificado", if (p.foiNotificado) 1 else 0)
                    pJson.putSeNaoNulo("observacoes", p.observacoes)

                    val excJson = JSONArray()
                    p.idsExcluidos.forEach { excJson.put(it) }
                    pJson.put("exclusoes", excJson)

                    val desejosJson = JSONArray()
                    for (d in desejoDao.listarPorParticipante(p.id)) {
                        val dJson = JSONObject()
                        dJson.put("produto", d.produto ?: "")
                        dJson.put("categoria", d.categoria ?: "")
                        dJson.put("preco_minimo", d.precoMinimo)
                        dJson.put("preco_maximo", d.precoMaximo)
                        dJson.put("lojas", d.lojas ?: "")
                        desejosJson.put(dJson)
                    }
                    pJson.put("desejos", desejosJson)
                    partJson.put(pJson)
                }
                gJson.put("participantes", partJson)

                // Sorteios
                val sorteiosJson = JSONArray()
                for (s in sorteioDao.listarPorGrupo(grupo.id)) {
                    val sJson = JSONObject()
                    sJson.put("id", s.id)
                    sJson.put("data_hora", s.dataHora)
                    val paresJson = JSONArray()
                    for (par in s.pares) {
                        val parJson = JSONObject()
                        parJson.put("participante_id", par.participanteId)
                        parJson.put("sorteado_id", par.sorteadoId)
                        parJson.put("nome_participante", par.nomeParticipante)
                        parJson.put("nome_sorteado", par.nomeSorteado)
                        parJson.put("enviado", if (par.enviado) 1 else 0)
                        paresJson.put(parJson)
                    }
                    sJson.put("pares", paresJson)
                    sorteiosJson.put(sJson)
                }
                gJson.put("sorteios", sorteiosJson)

                gruposJson.put(gJson)
            }
            root.put("grupos", gruposJson)
            root.toString(2)
        } finally {
            grupoDao.close()
            participanteDao.close()
            desejoDao.close()
            sorteioDao.close()
        }
    }

    /**
     * Importa dados de uma string JSON, substituindo todos os dados existentes.
     *
     * A operação é completamente atômica: toda a limpeza e reinserção ocorre dentro de uma
     * única transação SQLite. Se qualquer erro acontecer, a transação faz rollback automático
     * e os dados originais são preservados.
     *
     * @return [ImportResult.Success] com o número de grupos importados, ou [ImportResult.Failure]
     */
    fun importarDeJson(context: Context, jsonString: String): ImportResult {
        // Fase 1: parse e validação completa ANTES de tocar no banco
        val root: JSONObject
        try {
            root = JSONObject(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "importarDeJson: JSON malformado")
            return ImportResult.Failure("JSON inválido: ${e.message}")
        }

        val version = root.optInt("version", -1)
        if (version < 1) return ImportResult.Failure("Campo 'version' ausente ou inválido")

        // Compara com a versão do Room (13+), não com a do MySQLiteOpenHelper (10):
        // desde a v11 é o Room que gerencia o schema, e um backup da v12 em diante
        // carrega colunas que a versão congelada do helper não conhece.
        val schemaVersion = root.optInt("schema_version", -1)
        if (schemaVersion > AppDatabase.SCHEMA_VERSION) {
            return ImportResult.Failure(
                "schema_version $schemaVersion é maior que a versão atual ${AppDatabase.SCHEMA_VERSION}"
            )
        }

        val gruposJson = root.optJSONArray("grupos")
            ?: return ImportResult.Failure("Campo 'grupos' ausente no JSON")

        // Fase 2: inserção atômica via MySQLiteOpenHelper.
        // Usamos o mesmo helper dos DAOs legados — o SQLiteOpenHelper do Android gerencia
        // um único connection pool por arquivo. Não fechamos o helper após o uso para não
        // invalidar conexões abertas em outros DAOs do mesmo processo.
        val helper = MySQLiteOpenHelper.getInstance(context)
        val db = helper.writableDatabase
        db.execSQL("PRAGMA foreign_keys = ON")
        db.beginTransaction()
        return try {
            // Limpar tudo dentro da transação — deletar em ordem inversa das FKs
            db.delete(MySQLiteOpenHelper.TABLE_SORTEIO_PAR, null, null)
            db.delete(MySQLiteOpenHelper.TABLE_SORTEIO, null, null)
            db.delete(MySQLiteOpenHelper.TABLE_EXCLUSAO, null, null)
            db.delete(MySQLiteOpenHelper.TABLE_DESEJO, null, null)
            db.delete(MySQLiteOpenHelper.TABLE_PARTICIPANTE, null, null)
            db.delete(MySQLiteOpenHelper.TABLE_GRUPO, null, null)

            var gruposImportados = 0
            for (i in 0 until gruposJson.length()) {
                val gJson = gruposJson.getJSONObject(i)

                val grupoValues = ContentValues().apply {
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_NOME, gJson.optString("nome", ""))
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_DATA, gJson.optString("data", ""))

                    // Configuração do grupo (v12). Backups no formato 1 não têm estes campos:
                    // as strings ficam null e os demais assumem o default do schema
                    // (permitir_ver_desejos = 1, exigir_confirmacao_compra = 0, valores = 0.0).
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_DESCRICAO, gJson.optStringOuNulo("descricao"))
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_DATA_EVENTO, gJson.optStringOuNulo("data_evento"))
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_LOCAL_EVENTO, gJson.optStringOuNulo("local_evento"))
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_DATA_LIMITE_SORTEIO, gJson.optStringOuNulo("data_limite_sorteio"))
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_VALOR_MINIMO, gJson.optDouble("valor_minimo", 0.0))
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_VALOR_MAXIMO, gJson.optDouble("valor_maximo", 0.0))
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_REGRAS, gJson.optStringOuNulo("regras"))
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_PERMITIR_VER_DESEJOS, gJson.optInt("permitir_ver_desejos", 1))
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_EXIGIR_CONFIRMACAO_COMPRA, gJson.optInt("exigir_confirmacao_compra", 0))
                }
                val novoGrupoId = db.insertOrThrow(MySQLiteOpenHelper.TABLE_GRUPO, null, grupoValues)
                if (novoGrupoId == -1L) throw IllegalStateException("Falha ao inserir grupo")

                // Mapa id_antigo -> id_novo para remapear referências FK
                val idMap = mutableMapOf<Int, Int>()

                val partJson = gJson.optJSONArray("participantes") ?: JSONArray()

                // Primeira passagem: inserir participantes e desejos
                for (j in 0 until partJson.length()) {
                    val pJson = partJson.getJSONObject(j)

                    val partValues = ContentValues().apply {
                        put(MySQLiteOpenHelper.COLUMN_NOME, pJson.optString("nome", ""))
                        put(MySQLiteOpenHelper.COLUMN_EMAIL, pJson.optString("email", ""))
                        put(MySQLiteOpenHelper.COLUMN_TELEFONE, pJson.optString("telefone", ""))
                        put(MySQLiteOpenHelper.COLUMN_ENVIADO, pJson.optInt("enviado", 0))
                        put(MySQLiteOpenHelper.COLUMN_FK_GRUPO_ID, novoGrupoId)

                        // Rastreamento do participante (v12) — ausente em backups do formato 1.
                        put(MySQLiteOpenHelper.COLUMN_CONFIRMOU_PRESENTE, pJson.optInt("confirmou_presente", 0))
                        put(MySQLiteOpenHelper.COLUMN_FOI_NOTIFICADO, pJson.optInt("foi_notificado", 0))
                        put(MySQLiteOpenHelper.COLUMN_OBSERVACOES, pJson.optStringOuNulo("observacoes"))
                    }
                    val novoPartId = db.insertOrThrow(MySQLiteOpenHelper.TABLE_PARTICIPANTE, null, partValues)
                    if (novoPartId == -1L) throw IllegalStateException("Falha ao inserir participante")
                    idMap[pJson.optInt("id", -1)] = novoPartId.toInt()

                    val desejosJson = pJson.optJSONArray("desejos") ?: JSONArray()
                    for (k in 0 until desejosJson.length()) {
                        val dJson = desejosJson.getJSONObject(k)
                        val desejoValues = ContentValues().apply {
                            put(MySQLiteOpenHelper.COLUMN_PRODUTO, dJson.optString("produto", ""))
                            put(MySQLiteOpenHelper.COLUMN_CATEGORIA, dJson.optString("categoria", ""))
                            put(MySQLiteOpenHelper.COLUMN_PRECO_MINIMO, dJson.optDouble("preco_minimo", 0.0))
                            put(MySQLiteOpenHelper.COLUMN_PRECO_MAXIMO, dJson.optDouble("preco_maximo", 0.0))
                            put(MySQLiteOpenHelper.COLUMN_LOJAS, dJson.optString("lojas", ""))
                            put(MySQLiteOpenHelper.COLUMN_DESEJO_PARTICIPANTE_ID, novoPartId)
                        }
                        db.insertOrThrow(MySQLiteOpenHelper.TABLE_DESEJO, null, desejoValues)
                    }
                }

                // Segunda passagem: remapear amigo_sorteado_id e exclusões
                for (j in 0 until partJson.length()) {
                    val pJson = partJson.getJSONObject(j)
                    val novoPartId = idMap[pJson.optInt("id", -1)] ?: continue

                    val oldAmigoId = pJson.optInt("amigo_sorteado_id", 0)
                    if (oldAmigoId > 0) {
                        val novoAmigoId = idMap[oldAmigoId] ?: 0
                        if (novoAmigoId > 0) {
                            val v = ContentValues().apply {
                                put(MySQLiteOpenHelper.COLUMN_AMIGO_SORTEADO_ID, novoAmigoId)
                            }
                            db.update(MySQLiteOpenHelper.TABLE_PARTICIPANTE, v,
                                "${MySQLiteOpenHelper.COLUMN_ID} = ?", arrayOf(novoPartId.toString()))
                        }
                    }

                    val excJson = pJson.optJSONArray("exclusoes") ?: JSONArray()
                    for (k in 0 until excJson.length()) {
                        val novoExcId = idMap[excJson.getInt(k)] ?: continue
                        val v = ContentValues().apply {
                            put(MySQLiteOpenHelper.COLUMN_PARTICIPANTE_ID, novoPartId)
                            put(MySQLiteOpenHelper.COLUMN_EXCLUIDO_ID, novoExcId)
                        }
                        db.insertWithOnConflict(MySQLiteOpenHelper.TABLE_EXCLUSAO, null, v, SQLiteDatabase.CONFLICT_IGNORE)
                    }
                }

                // Sorteios
                val sorteiosJson = gJson.optJSONArray("sorteios") ?: JSONArray()
                for (j in 0 until sorteiosJson.length()) {
                    val sJson = sorteiosJson.getJSONObject(j)
                    val sorteioValues = ContentValues().apply {
                        put(MySQLiteOpenHelper.COLUMN_SORTEIO_GRUPO_ID, novoGrupoId)
                        put(MySQLiteOpenHelper.COLUMN_SORTEIO_DATA_HORA, sJson.optString("data_hora", ""))
                    }
                    val sorteioId = db.insertOrThrow(MySQLiteOpenHelper.TABLE_SORTEIO, null, sorteioValues)
                    if (sorteioId == -1L) throw IllegalStateException("Falha ao inserir sorteio")

                    val paresJson = sJson.optJSONArray("pares") ?: JSONArray()
                    for (k in 0 until paresJson.length()) {
                        val parJson = paresJson.getJSONObject(k)
                        val novoPartId = idMap[parJson.optInt("participante_id", -1)] ?: continue
                        val novoSortId = idMap[parJson.optInt("sorteado_id", -1)] ?: continue
                        val parValues = ContentValues().apply {
                            put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_SORTEIO_ID, sorteioId)
                            put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_PARTICIPANTE_ID, novoPartId)
                            put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_SORTEADO_ID, novoSortId)
                            put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_NOME_PARTICIPANTE, parJson.optString("nome_participante", ""))
                            put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_NOME_SORTEADO, parJson.optString("nome_sorteado", ""))
                            put(MySQLiteOpenHelper.COLUMN_SORTEIO_PAR_ENVIADO, parJson.optInt("enviado", 0))
                        }
                        db.insertWithOnConflict(MySQLiteOpenHelper.TABLE_SORTEIO_PAR, null, parValues, SQLiteDatabase.CONFLICT_IGNORE)
                    }
                }

                gruposImportados++
            }

            db.setTransactionSuccessful()
            ImportResult.Success(gruposImportados)
        } catch (e: Exception) {
            Timber.e(e, "importarDeJson: falha na importação — rollback executado")
            ImportResult.Failure(e.message ?: "Erro desconhecido")
        } finally {
            db.endTransaction()
            // Não fechar helper: SQLiteOpenHelper usa reference counting interno.
            // Fechar aqui invalidaria o connection pool compartilhado com DAOs legados
            // que possam estar abertos em outras threads (ex: GruposActivity.contarPorGrupo).
        }
    }

    sealed class ImportResult {
        data class Success(val gruposImportados: Int) : ImportResult()
        data class Failure(val reason: String) : ImportResult()
    }
}
