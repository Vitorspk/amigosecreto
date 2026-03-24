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
    @ColumnInfo(name = "descricao")
    var descricao: String? = null,
    @ColumnInfo(name = "data_evento")
    var dataEvento: String? = null,
    @ColumnInfo(name = "local_evento")
    var localEvento: String? = null,
    @ColumnInfo(name = "data_limite_sorteio")
    var dataLimiteSorteio: String? = null,
    @ColumnInfo(name = "valor_minimo", defaultValue = "0.0")
    var valorMinimo: Double = 0.0,
    @ColumnInfo(name = "valor_maximo", defaultValue = "0.0")
    var valorMaximo: Double = 0.0,
    @ColumnInfo(name = "regras")
    var regras: String? = null,
    @ColumnInfo(name = "permitir_ver_desejos", defaultValue = "1")
    var permitirVerDesejos: Boolean = true,
    @ColumnInfo(name = "exigir_confirmacao_compra", defaultValue = "0")
    var exigirConfirmacaoCompra: Boolean = false,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    override fun toString(): String = nome ?: ""
}
