package activity.amigosecreto.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import activity.amigosecreto.R
import activity.amigosecreto.db.Participante

/**
 * Modern RecyclerView Adapter for Participantes with animations
 */
class ParticipantesRecyclerAdapter(
    private val context: Context,
    private var participantes: List<Participante>
) : RecyclerView.Adapter<ParticipantesRecyclerAdapter.ViewHolder>() {

    private var listener: OnItemClickListener? = null
    private var lastPosition = -1

    interface OnItemClickListener {
        fun onItemClick(participante: Participante)
        fun onRemoveClick(participante: Participante)
        fun onShareClick(participante: Participante)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_participante, parent, false)
        return ViewHolder(view)
    }

    @Suppress("RecyclerView")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val participante = participantes[position]
        holder.bind(participante)

        // Apply animation
        if (position > lastPosition) {
            holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.card_appear))
            lastPosition = position
        }
    }

    override fun getItemCount() = participantes.size

    @Suppress("NotifyDataSetChanged")
    fun updateList(newList: List<Participante>) {
        participantes = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNome: TextView = itemView.findViewById(R.id.tv_nome)
        val tvEmail: TextView = itemView.findViewById(R.id.tv_email)
        val tvNumero: TextView = itemView.findViewById(R.id.tv_numero)
        val tvAvatar: TextView = itemView.findViewById(R.id.tv_avatar)
        val btnRemover: ImageButton = itemView.findViewById(R.id.btn_remover)
        val btnShare: ImageButton = itemView.findViewById(R.id.btn_share)

        fun bind(participante: Participante) {
            tvNome.text = participante.nome
            tvNumero.text = (bindingAdapterPosition + 1).toString()

            // Avatar with first letter
            // TODO: extrair para fun String?.toAvatarText() — mesma expressão duplicada em
            //       ExclusionViewHolder e no ViewHolder inline do bottom sheet em ParticipantesActivity.
            tvAvatar.text = participante.nome?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

            // Status
            tvEmail.text = if (participante.isEnviado) context.getString(R.string.status_item_sent) else context.getString(R.string.status_item_pending)

            // Click listeners
            itemView.setOnClickListener {
                listener?.onItemClick(participante)
            }

            btnRemover.setOnClickListener {
                listener?.onRemoveClick(participante)
            }

            btnShare.setOnClickListener {
                listener?.onShareClick(participante)
            }
        }
    }
}
