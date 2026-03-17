package activity.amigosecreto

import android.content.Context
import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.graphics.Insets
import androidx.core.view.MenuItemCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.DesejoDAO
import activity.amigosecreto.util.AsyncDatabaseHelper
import activity.amigosecreto.util.StateViewHelper
import activity.amigosecreto.util.WindowInsetsUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ListarDesejos : AppCompatActivity(), AdapterView.OnItemClickListener {

    private companion object { const val TAG = "ListarDesejos" }

    private lateinit var lvDesejos: ListView
    private lateinit var adapter: ListarDesejosAdapter
    private lateinit var stateHelper: StateViewHelper
    private val listaDesejos = mutableListOf<Desejo>()
    private var lista: List<Desejo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listar_desejos)

        adapter = ListarDesejosAdapter(this, listaDesejos)
        lvDesejos = findViewById(R.id.lv_desejos)
        val fabNovo = findViewById<FloatingActionButton>(R.id.fab_novo)

        stateHelper = StateViewHelper(
            stubLoading = findViewById(R.id.stub_loading),
            stubEmpty = findViewById(R.id.stub_empty),
            contentView = lvDesejos
        )

        lvDesejos.onItemClickListener = this
        lvDesejos.adapter = adapter

        if (fabNovo != null) {
            // Ajusta margem inferior do FAB para não ficar atrás da navigation bar.
            // requestLayout() é suficiente após modificar lp em-place (evita re-inflate).
            val fabMarginBase = (24 * resources.displayMetrics.density).toInt()
            ViewCompat.setOnApplyWindowInsetsListener(fabNovo) { v, insets ->
                val systemBars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val lp = v.layoutParams as ViewGroup.MarginLayoutParams
                lp.bottomMargin = systemBars.bottom + fabMarginBase
                v.requestLayout()
                insets
            }
            fabNovo.setOnClickListener {
                startActivity(Intent(this, InserirDesejoActivity::class.java))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        carregarLista()
    }

    private fun carregarLista() {
        stateHelper.showLoading()
        AsyncDatabaseHelper.execute(
            object : AsyncDatabaseHelper.BackgroundTask<List<Desejo>> {
                override fun doInBackground(): List<Desejo> {
                    val dao = DesejoDAO(this@ListarDesejos)
                    dao.open()
                    return try { dao.listar() } finally { dao.close() }
                }
            },
            object : AsyncDatabaseHelper.ResultCallback<List<Desejo>> {
                override fun onSuccess(result: List<Desejo>) {
                    lista = result
                    listaDesejos.clear()
                    listaDesejos.addAll(result)
                    adapter.notifyDataSetChanged()
                    if (listaDesejos.isEmpty()) stateHelper.showEmpty() else stateHelper.showContent()
                }
                override fun onError(e: Exception) {
                    Log.e(TAG, "carregarLista: failed", e)
                    Toast.makeText(this@ListarDesejos, R.string.error_load_wishes, Toast.LENGTH_LONG).show()
                    stateHelper.showEmpty()
                }
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.listar_desejos, menu)
        compartilharLista(menu)
        return true
    }

    private fun compartilharLista(menu: Menu) {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
        val sb = StringBuilder()
        val nf = WindowInsetsUtils.currencyFormatPtBr()
        val ls = System.getProperty("line.separator")
        if (lista.isNotEmpty()) {
            for (d in lista) {
                sb.append(getString(R.string.share_wish_prefix_desejo)).append(d.produto).append(ls)
                sb.append(getString(R.string.share_wish_price_range, nf.format(d.precoMinimo), nf.format(d.precoMaximo))).append(ls)
                val lojas = d.lojas?.replace(ls ?: "\n", ", ") ?: ""
                sb.append(getString(R.string.share_wish_prefix_lojas)).append(lojas).append(ls).append(ls)
            }
        } else {
            sb.append(getString(R.string.share_wishes_empty))
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_wishes_subject))
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString())
        val item = menu.findItem(R.id.menu_compartilhar)
        if (item != null) {
            @Suppress("DEPRECATION")
            val provider = MenuItemCompat.getActionProvider(item) as? ShareActionProvider
            provider?.setShareIntent(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_novo -> {
            startActivity(Intent(this, InserirDesejoActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val desejo = listaDesejos[position]
        val intent = Intent(this, DetalheDesejoActivity::class.java)
        intent.putExtra("desejo", desejo)
        startActivity(intent)
    }

    private class ListarDesejosAdapter(
        private val ctx: Context,
        private val produtos: List<Desejo>
    ) : BaseAdapter() {

        private val nf = WindowInsetsUtils.currencyFormatPtBr()

        override fun getCount() = produtos.size
        override fun getItem(position: Int) = produtos[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val holder: ViewHolder
            val view: View
            if (convertView == null) {
                view = LayoutInflater.from(ctx).inflate(R.layout.item_desejo, parent, false)
                holder = ViewHolder(
                    tvProduto = view.findViewById(R.id.tv_item_produto),
                    tvCategoria = view.findViewById(R.id.tv_item_categoria),
                    tvPreco = view.findViewById(R.id.tv_item_preco)
                )
                view.tag = holder
            } else {
                view = convertView
                holder = convertView.tag as ViewHolder
            }

            val desejo = produtos[position]
            holder.tvProduto?.text = desejo.produto

            if (!desejo.categoria.isNullOrEmpty()) {
                holder.tvCategoria?.text = desejo.categoria
                holder.tvCategoria?.visibility = View.VISIBLE
            } else {
                holder.tvCategoria?.visibility = View.GONE
            }

            if (desejo.precoMinimo > 0 || desejo.precoMaximo > 0) {
                holder.tvPreco?.text = "${nf.format(desejo.precoMinimo)} - ${nf.format(desejo.precoMaximo)}"
                holder.tvPreco?.visibility = View.VISIBLE
            } else {
                holder.tvPreco?.visibility = View.GONE
            }

            return view
        }

        private data class ViewHolder(
            val tvProduto: TextView?,
            val tvCategoria: TextView?,
            val tvPreco: TextView?
        )
    }
}
