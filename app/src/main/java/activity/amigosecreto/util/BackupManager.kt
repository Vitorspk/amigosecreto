package activity.amigosecreto.util

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import org.json.JSONArray
import org.json.JSONObject
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.Exclusao
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.db.SorteioPar
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
 * ## Acesso ao banco
 *
 * Ambas as operações usam exclusivamente os DAOs do Room. O `MySQLiteOpenHelper` legado
 * **não** pode ser usado aqui: ele está congelado em `DATABASE_VERSION = 10` e, ao abrir um
 * arquivo que o Room migrou para 13, o `SQLiteOpenHelper` chama `onDowngrade()` e em seguida
 * grava `setVersion(10)`. Isso rebaixava o `user_version` do arquivo e fazia o Room
 * re-executar a `MIGRATION_10_11` no start seguinte — que recria `participante` sem as
 * colunas da v12 e perde `confirmou_presente`, `foi_notificado` e `observacoes`.
 *
 * Todos os métodos são `suspend` e devem ser chamados de uma coroutine.
 */
object BackupManager {

    private const val BACKUP_VERSION = 2

    /** Grava [valor] em [chave] apenas se não for nulo — preserva null vs "" no round-trip. */
    private fun JSONObject.putSeNaoNulo(chave: String, valor: String?) {
        if (valor != null) put(chave, valor)
    }

    /** Lê uma string opcional: chave ausente significa `null` (e não `""`). */
    private fun JSONObject.optStringOuNulo(chave: String): String? =
        if (has(chave) && !isNull(chave)) getString(chave) else null

    suspend fun exportarParaJson(context: Context): String {
        val db = AppDatabase.getInstance(context)
        val grupoDao = db.grupoDao()
        val participanteDao = db.participanteDao()
        val desejoDao = db.desejoDao()
        val sorteioDao = db.sorteioDao()

        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        // Os dados vêm do schema gerenciado pelo Room — ver AppDatabase.SCHEMA_VERSION.
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

            // Participantes (listarPorGrupo já popula idsExcluidos)
            val partJson = JSONArray()
            for (p in participanteDao.listarPorGrupo(grupo.id)) {
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

            // Sorteios (listarPorGrupo já popula os pares)
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
        return root.toString(2)
    }

    /**
     * Importa dados de uma string JSON, substituindo todos os dados existentes.
     *
     * A operação é atômica: toda a limpeza e reinserção ocorre dentro de uma única
     * transação do Room. Se qualquer erro acontecer, a transação faz rollback automático
     * e os dados originais são preservados.
     *
     * @return [ImportResult.Success] com o número de grupos importados, ou [ImportResult.Failure]
     */
    suspend fun importarDeJson(context: Context, jsonString: String): ImportResult {
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

        // Compara com a versão do Room: desde a v11 é ele que gerencia o schema, e um
        // backup da v12 em diante carrega colunas que versões anteriores não conhecem.
        val schemaVersion = root.optInt("schema_version", -1)
        if (schemaVersion > AppDatabase.SCHEMA_VERSION) {
            return ImportResult.Failure(
                "schema_version $schemaVersion é maior que a versão atual ${AppDatabase.SCHEMA_VERSION}"
            )
        }

        val gruposJson = root.optJSONArray("grupos")
            ?: return ImportResult.Failure("Campo 'grupos' ausente no JSON")

        // Fase 2: inserção atômica via Room.
        val db = AppDatabase.getInstance(context)
        return try {
            var gruposImportados = 0
            db.withTransaction {
                val grupoDao = db.grupoDao()
                val participanteDao = db.participanteDao()
                val desejoDao = db.desejoDao()
                val sorteioDao = db.sorteioDao()

                // Limpa tudo respeitando a ordem das FKs.
                grupoDao.deletarTudo()

                for (i in 0 until gruposJson.length()) {
                    val gJson = gruposJson.getJSONObject(i)

                    // id = 0 deixa o Room gerar um novo — as referências são remapeadas abaixo.
                    val novoGrupoId = grupoDao.inserir(
                        Grupo(
                            nome = gJson.optString("nome", ""),
                            data = gJson.optString("data", ""),
                            // Backups no formato 1 não têm estes campos: as strings ficam null
                            // e os demais assumem o default do schema.
                            descricao = gJson.optStringOuNulo("descricao"),
                            dataEvento = gJson.optStringOuNulo("data_evento"),
                            localEvento = gJson.optStringOuNulo("local_evento"),
                            dataLimiteSorteio = gJson.optStringOuNulo("data_limite_sorteio"),
                            valorMinimo = gJson.optDouble("valor_minimo", 0.0),
                            valorMaximo = gJson.optDouble("valor_maximo", 0.0),
                            regras = gJson.optStringOuNulo("regras"),
                            permitirVerDesejos = gJson.optInt("permitir_ver_desejos", 1) == 1,
                            exigirConfirmacaoCompra = gJson.optInt("exigir_confirmacao_compra", 0) == 1,
                        )
                    ).toInt()

                    // Mapa id_antigo -> id_novo para remapear referências FK
                    val idMap = mutableMapOf<Int, Int>()
                    val partJson = gJson.optJSONArray("participantes") ?: JSONArray()

                    // Primeira passagem: inserir participantes e desejos
                    for (j in 0 until partJson.length()) {
                        val pJson = partJson.getJSONObject(j)
                        val novoPartId = participanteDao.inserir(
                            Participante(
                                nome = pJson.optString("nome", ""),
                                email = pJson.optString("email", ""),
                                telefone = pJson.optString("telefone", ""),
                                isEnviado = pJson.optInt("enviado", 0) == 1,
                                grupoId = novoGrupoId,
                                confirmouPresente = pJson.optInt("confirmou_presente", 0) == 1,
                                foiNotificado = pJson.optInt("foi_notificado", 0) == 1,
                                observacoes = pJson.optStringOuNulo("observacoes"),
                            )
                        ).toInt()
                        idMap[pJson.optInt("id", -1)] = novoPartId

                        val desejosJson = pJson.optJSONArray("desejos") ?: JSONArray()
                        for (k in 0 until desejosJson.length()) {
                            val dJson = desejosJson.getJSONObject(k)
                            desejoDao.inserir(
                                Desejo(
                                    produto = dJson.optString("produto", ""),
                                    categoria = dJson.optString("categoria", ""),
                                    lojas = dJson.optString("lojas", ""),
                                    precoMinimo = dJson.optDouble("preco_minimo", 0.0),
                                    precoMaximo = dJson.optDouble("preco_maximo", 0.0),
                                    participanteId = novoPartId,
                                )
                            )
                        }
                    }

                    // Segunda passagem: remapear amigo_sorteado_id e exclusões
                    for (j in 0 until partJson.length()) {
                        val pJson = partJson.getJSONObject(j)
                        val novoPartId = idMap[pJson.optInt("id", -1)] ?: continue

                        val oldAmigoId = pJson.optInt("amigo_sorteado_id", 0)
                        if (oldAmigoId > 0) {
                            idMap[oldAmigoId]?.let { novoAmigoId ->
                                participanteDao.atualizarAmigoSorteado(novoPartId, novoAmigoId)
                            }
                        }

                        val excJson = pJson.optJSONArray("exclusoes") ?: JSONArray()
                        for (k in 0 until excJson.length()) {
                            val novoExcId = idMap[excJson.getInt(k)] ?: continue
                            participanteDao.inserirExclusao(Exclusao(novoPartId, novoExcId))
                        }
                    }

                    // Sorteios
                    val sorteiosJson = gJson.optJSONArray("sorteios") ?: JSONArray()
                    for (j in 0 until sorteiosJson.length()) {
                        val sJson = sorteiosJson.getJSONObject(j)
                        val sorteioId = sorteioDao.inserirSorteio(
                            Sorteio(
                                grupoId = novoGrupoId,
                                dataHora = sJson.optString("data_hora", ""),
                            )
                        ).toInt()

                        val paresJson = sJson.optJSONArray("pares") ?: JSONArray()
                        for (k in 0 until paresJson.length()) {
                            val parJson = paresJson.getJSONObject(k)
                            val novoPartId = idMap[parJson.optInt("participante_id", -1)] ?: continue
                            val novoSortId = idMap[parJson.optInt("sorteado_id", -1)] ?: continue
                            sorteioDao.inserirPar(
                                SorteioPar(
                                    sorteioId = sorteioId,
                                    participanteId = novoPartId,
                                    sorteadoId = novoSortId,
                                    nomeParticipante = parJson.optString("nome_participante", ""),
                                    nomeSorteado = parJson.optString("nome_sorteado", ""),
                                    enviado = parJson.optInt("enviado", 0) == 1,
                                )
                            )
                        }
                    }

                    gruposImportados++
                }
            }
            ImportResult.Success(gruposImportados)
        } catch (e: CancellationException) {
            // Cancelamento não é falha de importação: precisa propagar para cooperar com
            // structured concurrency (ex.: viewModelScope sendo cancelado). A transação do
            // Room já fez rollback. Sem este catch, CancellationException — que é uma
            // Exception em Kotlin — viraria um ImportResult.Failure e engoliria o cancelamento.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "importarDeJson: falha na importação — rollback executado")
            ImportResult.Failure(e.message ?: "Erro desconhecido")
        }
    }

    sealed class ImportResult {
        data class Success(val gruposImportados: Int) : ImportResult()
        data class Failure(val reason: String) : ImportResult()
    }
}
