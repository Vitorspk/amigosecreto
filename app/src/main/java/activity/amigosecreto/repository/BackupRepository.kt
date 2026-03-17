package activity.amigosecreto.repository

import android.content.Context
import activity.amigosecreto.util.BackupManager

/**
 * Repository que encapsula [BackupManager] seguindo o padrão do projeto.
 *
 * Todos os métodos são síncronos e devem ser chamados a partir de uma thread de background.
 */
open class BackupRepository(private val context: Context) {

    open fun exportar(): String = BackupManager.exportarParaJson(context)

    open fun importar(jsonString: String): BackupManager.ImportResult =
        BackupManager.importarDeJson(context, jsonString)
}
