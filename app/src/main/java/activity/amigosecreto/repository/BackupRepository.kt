package activity.amigosecreto.repository

import android.content.Context
import activity.amigosecreto.util.BackupManager

/**
 * Repository que encapsula [BackupManager] seguindo o padrão do projeto.
 *
 * Todos os métodos são suspend — devem ser chamados a partir de uma coroutine.
 */
open class BackupRepository(private val context: Context) {

    open suspend fun exportar(): String = BackupManager.exportarParaJson(context)

    open suspend fun importar(jsonString: String): BackupManager.ImportResult =
        BackupManager.importarDeJson(context, jsonString)
}
