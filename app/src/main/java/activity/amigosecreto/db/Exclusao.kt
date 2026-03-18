package activity.amigosecreto.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Tabela de exclusões: define pares (participante_id, excluido_id) onde o participante
 * NÃO pode sortear o excluído. Usada pelo SorteioEngine para validar restrições.
 * ON DELETE CASCADE: ao remover um participante, suas exclusões são removidas automaticamente.
 */
@Entity(
    tableName = "exclusao",
    primaryKeys = ["participante_id", "excluido_id"],
    foreignKeys = [
        ForeignKey(
            entity = Participante::class,
            parentColumns = ["id"],
            childColumns = ["participante_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Participante::class,
            parentColumns = ["id"],
            childColumns = ["excluido_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class Exclusao(
    @ColumnInfo(name = "participante_id")
    var participanteId: Int = 0,
    @ColumnInfo(name = "excluido_id", index = true)
    var excluidoId: Int = 0,
)
