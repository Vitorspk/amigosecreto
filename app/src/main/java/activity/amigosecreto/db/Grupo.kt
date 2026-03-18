package activity.amigosecreto.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

// Plain class (not data class): reference equality — safe in all collection types.
// toString() returns "" for null nome; Java returned null. Production uses getNome() directly.
@Entity(tableName = "grupo")
class Grupo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,
    @ColumnInfo(name = "nome")
    var nome: String? = null,
    @ColumnInfo(name = "data")
    var data: String? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    override fun toString(): String = nome ?: ""
}
