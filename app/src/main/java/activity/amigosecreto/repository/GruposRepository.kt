package activity.amigosecreto.repository

import androidx.annotation.VisibleForTesting
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.room.GrupoRoomDao

/**
 * Repository que encapsula todo o acesso a [GrupoRoomDao].
 *
 * A classe é `open` para que testes Kotlin possam criar subclasses anônimas com override.
 * Todos os métodos são suspend — devem ser chamados a partir de uma coroutine.
 */
open class GruposRepository @VisibleForTesting internal constructor(
    private val dao: GrupoRoomDao,
) {
    open suspend fun listar(): List<Grupo> = dao.listar()
    open suspend fun inserir(grupo: Grupo): Long = dao.inserir(grupo)
    open suspend fun atualizar(grupo: Grupo): Boolean = dao.atualizar(grupo) > 0
    open suspend fun remover(grupo: Grupo) = dao.remover(grupo)
    open suspend fun limparTudo() = dao.deletarTudo()
}
