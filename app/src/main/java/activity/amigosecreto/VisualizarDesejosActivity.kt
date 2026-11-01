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
import activity.amigosecreto.repository.DesejoRepository
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import activity.amigosecreto.db.Participante
import activity.amigosecreto.util.WindowInsetsUtils
import activity.amigosecreto.util.AsyncDatabaseHelper
import com.google.android.material.appbar.MaterialToolbar

@AndroidEntryPoint
class VisualizarDesejosActivity : AppCompatActivity() {

    private lateinit var participante: Participante
    @Inject lateinit var desejoRepository: DesejoRepository
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

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.v2_wishlist_title)
        toolbar.setNavigationOnClickListener { finish() }
        findViewById<TextView>(R.id.tv_nome_participante).text = participante.nome

        lvDesejos = findViewById(R.id.lv_desejos)
        layoutEmpty = findViewById(R.id.layout_empty)
        WindowInsetsUtils.applyImeBottomPadding(lvDesejos)

        adapter = DesejosAdapter(this, listaDesejos)
        lvDesejos.adapter = adapter

        carregarDesejos()
    }

    private fun carregarDesejos() = lifecycleScope.launch {
        AsyncDatabaseHelper.idlingResource.increment()
        try {
            val desejos = desejoRepository.listarPorParticipante(participante.id)
            listaDesejos.clear()
            listaDesejos.addAll(desejos)
            adapter.notifyDataSetChanged()
            val isEmpty = listaDesejos.isEmpty()
            layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            lvDesejos.visibility = if (isEmpty) View.GONE else View.VISIBLE
        } finally {
            AsyncDatabaseHelper.idlingResource.decrement()
        }
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
