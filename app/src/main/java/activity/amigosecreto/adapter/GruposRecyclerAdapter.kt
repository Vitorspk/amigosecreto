package activity.amigosecreto.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import activity.amigosecreto.GruposViewModel
import activity.amigosecreto.R
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.util.HapticFeedbackUtils

/**
 * RecyclerView adapter for the groups list in GruposActivity.
 * Extracted from inner class (Fase 4C4) to reduce Activity size (~560 lines → ~380 lines).
 *
 * Uses DiffUtil to avoid full redraws (Fase 4C3).
 *
 * All user actions are delegated to [OnGrupoActionListener] — the Activity implements
 * this interface and routes actions to the ViewModel.
 */
class GruposRecyclerAdapter(
    private val ctx: Context,
    private val emojis: Array<String>,
    private val gradientes: IntArray,
    private val listener: OnGrupoActionListener,
) : RecyclerView.Adapter<GruposRecyclerAdapter.ViewHolder>() {

    interface OnGrupoActionListener {
        fun onGrupoClick(grupo: Grupo)
        fun onEditarNome(grupo: Grupo, novoNome: String, dialog: AlertDialog, button: View)
        fun onRemover(grupo: Grupo)
    }

    private val itens = mutableListOf<GruposViewModel.GrupoComContagem>()

    fun setItens(novaLista: List<GruposViewModel.GrupoComContagem>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = itens.size
            override fun getNewListSize() = novaLista.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                itens[oldPos].grupo.id == novaLista[newPos].grupo.id
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                val o = itens[oldPos]; val n = novaLista[newPos]
                return o.grupo.nome == n.grupo.nome &&
                    o.totalParticipantes == n.totalParticipantes &&
                    o.totalEnviados == n.totalEnviados
            }
        })
        itens.clear()
        itens.addAll(novaLista)
        diff.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView = view.findViewById(R.id.tv_grupo_nome)
        val tvParticipantes: TextView = view.findViewById(R.id.tv_grupo_participantes)
        val tvEmoji: TextView = view.findViewById(R.id.tv_grupo_emoji)
        val layoutContent: LinearLayout = view.findViewById(R.id.layout_grupo_content)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                listener.onGrupoClick(itens[pos].grupo)
            }
            itemView.setOnLongClickListener { v ->
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnLongClickListener false
                HapticFeedbackUtils.performMediumFeedback(v)
                exibirMenuContextoGrupo(v, itens[pos].grupo)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(ctx).inflate(R.layout.item_grupo, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = itens.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pos = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
        val item = itens[pos]
        val g = item.grupo

        holder.tvNome.text = g.nome
        holder.tvEmoji.text = emojis[g.id % emojis.size]
        holder.layoutContent.setBackgroundResource(gradientes[g.id % gradientes.size])

        holder.tvParticipantes.text = if (item.totalEnviados > 0) {
            ctx.getString(R.string.label_progresso_sorteio, item.totalEnviados, item.totalParticipantes)
        } else {
            ctx.resources.getQuantityString(R.plurals.label_participants, item.totalParticipantes, item.totalParticipantes)
        }
    }

    private fun exibirMenuContextoGrupo(anchorView: View, g: Grupo) {
        val popup = PopupMenu(ctx, anchorView)
        popup.menu.add(0, MENU_EDITAR, 0, ctx.getString(R.string.grupo_menu_editar_nome))
        popup.menu.add(0, MENU_EXCLUIR, 1, ctx.getString(R.string.grupo_menu_excluir))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_EDITAR -> { exibirDialogEditarNome(g); true }
                MENU_EXCLUIR -> { listener.onRemover(g); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun exibirDialogEditarNome(g: Grupo) {
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_criar_grupo, null)

        val etNome = dialogView.findViewById<TextInputEditText>(R.id.et_nome_grupo)
        val btnCriar = dialogView.findViewById<MaterialButton>(R.id.btn_criar)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btn_cancelar)

        dialogView.findViewById<View>(R.id.chip_group_sugestoes)?.visibility = View.GONE

        etNome.setText(g.nome)
        etNome.setSelection(etNome.text?.length ?: 0)
        btnCriar.setText(R.string.button_save)

        val dialog = AlertDialog.Builder(ctx)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCriar.setOnClickListener { v ->
            val novoNome = etNome.text?.toString()?.trim() ?: ""
            if (novoNome.isEmpty()) {
                android.widget.Toast.makeText(ctx, R.string.grupo_erro_nome_obrigatorio, android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            listener.onEditarNome(g, novoNome, dialog, v)
        }

        btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private companion object {
        const val MENU_EDITAR = 1
        const val MENU_EXCLUIR = 2
    }
}
