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
 *   "version": 1,
 *   "schema_version": 10,
 *   "exported_at": "2026-03-17T14:30:00",
 *   "grupos": [...]
 * }
 * ```
 *
 * Todos os métodos são síncronos e devem ser chamados a partir de uma thread de background.
 */
object BackupManager {

    private const val TAG = "BackupManager"
    private const val BACKUP_VERSION = 1

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
            root.put("schema_version", MySQLiteOpenHelper.DATABASE_VERSION_PUBLIC)
            root.put("exported_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()))

            val gruposJson = JSONArray()
            for (grupo in grupoDao.listar()) {
                val gJson = JSONObject()
                gJson.put("id", grupo.id)
                gJson.put("nome", grupo.nome ?: "")
                gJson.put("data", grupo.data ?: "")

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

        val schemaVersion = root.optInt("schema_version", -1)
        if (schemaVersion > MySQLiteOpenHelper.DATABASE_VERSION_PUBLIC) {
            return ImportResult.Failure(
                "schema_version $schemaVersion é maior que a versão atual ${MySQLiteOpenHelper.DATABASE_VERSION_PUBLIC}"
            )
        }

        val gruposJson = root.optJSONArray("grupos")
            ?: return ImportResult.Failure("Campo 'grupos' ausente no JSON")

        // Fase 2: inserção atômica via conexão do Room.
        // Usar AppDatabase.openHelper garante que Room e BackupManager compartilham a mesma
        // conexão SQLite — evita bloqueios WAL e garante que o Room invalide seu cache após
        // a transação, tornando os dados importados imediatamente visíveis para queries Room.
        val roomDb = AppDatabase.getInstance(context)
        val db = roomDb.openHelper.writableDatabase
        db.execSQL("PRAGMA foreign_keys = ON")
        db.beginTransaction()
        return try {
            // Limpar tudo dentro da transação — deletar em ordem inversa das FKs
            db.execSQL("DELETE FROM ${MySQLiteOpenHelper.TABLE_SORTEIO_PAR}")
            db.execSQL("DELETE FROM ${MySQLiteOpenHelper.TABLE_SORTEIO}")
            db.execSQL("DELETE FROM ${MySQLiteOpenHelper.TABLE_EXCLUSAO}")
            db.execSQL("DELETE FROM ${MySQLiteOpenHelper.TABLE_DESEJO}")
            db.execSQL("DELETE FROM ${MySQLiteOpenHelper.TABLE_PARTICIPANTE}")
            db.execSQL("DELETE FROM ${MySQLiteOpenHelper.TABLE_GRUPO}")

            var gruposImportados = 0
            for (i in 0 until gruposJson.length()) {
                val gJson = gruposJson.getJSONObject(i)

                val grupoValues = ContentValues().apply {
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_NOME, gJson.optString("nome", ""))
                    put(MySQLiteOpenHelper.COLUMN_GRUPO_DATA, gJson.optString("data", ""))
                }
                val novoGrupoId = db.insert(MySQLiteOpenHelper.TABLE_GRUPO, SQLiteDatabase.CONFLICT_NONE, grupoValues)
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
                    }
                    val novoPartId = db.insert(MySQLiteOpenHelper.TABLE_PARTICIPANTE, SQLiteDatabase.CONFLICT_NONE, partValues)
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
                        db.insert(MySQLiteOpenHelper.TABLE_DESEJO, SQLiteDatabase.CONFLICT_NONE, desejoValues)
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
                            db.update(MySQLiteOpenHelper.TABLE_PARTICIPANTE, SQLiteDatabase.CONFLICT_NONE, v,
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
                        db.insert(MySQLiteOpenHelper.TABLE_EXCLUSAO, SQLiteDatabase.CONFLICT_IGNORE, v)
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
                    val sorteioId = db.insert(MySQLiteOpenHelper.TABLE_SORTEIO, SQLiteDatabase.CONFLICT_NONE, sorteioValues)
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
                        db.insert(MySQLiteOpenHelper.TABLE_SORTEIO_PAR, SQLiteDatabase.CONFLICT_IGNORE, parValues)
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
            // Não fechar db — é gerenciado pelo Room (AppDatabase singleton)
        }
    }

    sealed class ImportResult {
        data class Success(val gruposImportados: Int) : ImportResult()
        data class Failure(val reason: String) : ImportResult()
    }
}
