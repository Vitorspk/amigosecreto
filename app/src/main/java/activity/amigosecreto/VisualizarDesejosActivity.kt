package activity.amigosecreto

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.DesejoDAO
import activity.amigosecreto.db.Participante
import activity.amigosecreto.util.WindowInsetsUtils
import com.google.android.material.appbar.MaterialToolbar

class VisualizarDesejosActivity : AppCompatActivity() {

    private lateinit var participante: Participante
    private lateinit var desejoDAO: DesejoDAO
    private val listaDesejos = mutableListOf<Desejo>()
    private lateinit var adapter: DesejosAdapter
    private lateinit var lvDesejos: ListView
    private lateinit var layoutEmpty: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_visualizar_desejos)

        @Suppress("DEPRECATION")
        val p = intent.getSerializableExtra("participante") as? Participante
        if (p == null) {
            Toast.makeText(this, R.string.error_participant_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        participante = p

        desejoDAO = DesejoDAO(this)
        desejoDAO.open()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.title_wish_list_of, participante.nome)
        toolbar.setNavigationOnClickListener { finish() }

        lvDesejos = findViewById(R.id.lv_desejos)
        layoutEmpty = findViewById(R.id.layout_empty)

        adapter = DesejosAdapter(this, listaDesejos)
        lvDesejos.adapter = adapter

        carregarDesejos()
    }

    private fun carregarDesejos() {
        listaDesejos.clear()
        listaDesejos.addAll(desejoDAO.listarPorParticipante(participante.id))
        adapter.notifyDataSetChanged()
        val isEmpty = listaDesejos.isEmpty()
        layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        lvDesejos.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        desejoDAO.close()
    }

    private class DesejosAdapter(
        private val context: Context,
        private val desejos: List<Desejo>
    ) : BaseAdapter() {

        override fun getCount() = desejos.size
        override fun getItem(position: Int) = desejos[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_desejo, parent, false)

            val desejo = desejos[position]

            view.findViewById<TextView>(R.id.tv_item_produto).text = desejo.produto

            val tvCategoria = view.findViewById<TextView>(R.id.tv_item_categoria)
            if (!desejo.categoria.isNullOrEmpty()) {
                tvCategoria.text = desejo.categoria
                tvCategoria.visibility = View.VISIBLE
            } else {
                tvCategoria.visibility = View.GONE
            }

            val tvPreco = view.findViewById<TextView>(R.id.tv_item_preco)
            if (desejo.precoMinimo > 0 || desejo.precoMaximo > 0) {
                val nf = WindowInsetsUtils.numberFormatPtBr()
                tvPreco.text = "R$ ${nf.format(desejo.precoMinimo)} - R$ ${nf.format(desejo.precoMaximo)}"
                tvPreco.visibility = View.VISIBLE
            } else {
                tvPreco.visibility = View.GONE
            }

            return view
        }
    }
}
