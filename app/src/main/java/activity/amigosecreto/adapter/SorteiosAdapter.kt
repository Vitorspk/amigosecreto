package activity.amigosecreto.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView
import activity.amigosecreto.R
import activity.amigosecreto.db.Sorteio
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class SorteiosAdapter(
    private val ctx: Context,
    private val itens: List<Sorteio>
) : RecyclerView.Adapter<SorteiosAdapter.ViewHolder>() {

    private val expandidos = mutableSetOf<Int>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDataHora: TextView = view.findViewById(R.id.tv_data_hora)
        val tvNumPares: TextView = view.findViewById(R.id.tv_num_pares)
        val ivExpand: ImageView = view.findViewById(R.id.iv_expand)
        val layoutPares: LinearLayout = view.findViewById(R.id.layout_pares)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(ctx).inflate(R.layout.item_sorteio, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = itens.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sorteio = itens[position]

        holder.tvDataHora.text = formatarDataHora(sorteio.dataHora)
        holder.tvNumPares.text = ctx.getString(R.string.historico_pares, sorteio.pares.size)

        val expandido = expandidos.contains(sorteio.id)
        holder.layoutPares.visibility = if (expandido) View.VISIBLE else View.GONE
        holder.ivExpand.rotation = if (expandido) 270f else 90f

        if (expandido) {
            holder.layoutPares.removeAllViews()
            val inflater = LayoutInflater.from(ctx)
            for (par in sorteio.pares) {
                val parView = inflater.inflate(R.layout.item_sorteio_par, holder.layoutPares, false)
                parView.findViewById<TextView>(R.id.tv_par_item).text =
                    ctx.getString(R.string.historico_par_formato, par.nomeParticipante, par.nomeSorteado)
                holder.layoutPares.addView(parView)
            }
        }

        holder.itemView.setOnClickListener {
            if (expandidos.contains(sorteio.id)) {
                expandidos.remove(sorteio.id)
            } else {
                expandidos.add(sorteio.id)
            }
            notifyItemChanged(position)
        }
    }

    @VisibleForTesting
    internal fun formatarDataHora(dataHora: String): String {
        if (dataHora == ctx.getString(R.string.historico_sorteio_anterior)) return dataHora
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val sdfOut = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
            val date = sdfIn.parse(dataHora)
            if (date != null) sdfOut.format(date) else dataHora
        } catch (e: ParseException) {
            dataHora
        }
    }
}
