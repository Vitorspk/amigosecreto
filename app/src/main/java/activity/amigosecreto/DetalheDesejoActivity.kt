package activity.amigosecreto

import android.content.Intent
import timber.log.Timber
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.DesejoDAO
import activity.amigosecreto.util.WindowInsetsUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class DetalheDesejoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DetalheDesejoActivity"
        const val RESULT_REMOVE = 1000
        const val RESULT_SAVE = 2000
    }

    private lateinit var tvProduto: TextView
    private lateinit var tvCategoria: TextView
    private lateinit var tvPrecoMinimo: TextView
    private lateinit var tvPrecoMaximo: TextView
    private lateinit var tvLojas: TextView
    private lateinit var btnBuscape: MaterialButton

    private var desejo: Desejo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalhe_desejo)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        desejo = intent.extras?.get("desejo") as? Desejo

        tvProduto = findViewById(R.id.tv_produto)
        tvCategoria = findViewById(R.id.tv_categoria)
        tvPrecoMinimo = findViewById(R.id.tv_preco_minimo)
        tvPrecoMaximo = findViewById(R.id.tv_preco_maximo)
        tvLojas = findViewById(R.id.tv_lojas)
        btnBuscape = findViewById(R.id.btn_pesquisar_buscape)

        desejo?.let { d ->
            btnBuscape.setOnClickListener {
                val uri = Uri.Builder()
                    .scheme("https")
                    .authority("www.buscape.com.br")
                    .appendPath("search")
                    .appendQueryParameter("q", d.produto)
                    .build()
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            carregarCampos(d)
        }
    }

    private fun carregarCampos(d: Desejo) {
        val nf = WindowInsetsUtils.currencyFormatPtBr()
        tvProduto.setText(d.produto)
        tvCategoria.setText(d.categoria)
        tvPrecoMinimo.text = nf.format(d.precoMinimo)
        tvPrecoMaximo.text = nf.format(d.precoMaximo)
        tvLojas.setText(d.lojas)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.detalhe_desejo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean = when (item.itemId) {
        R.id.menu_editar -> {
            val intent = Intent(this, AlterarDesejoActivity::class.java)
            intent.putExtra("desejo", desejo)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 1)
            true
        }
        android.R.id.home -> { finish(); true }
        else -> super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            recarregarDesejo()
        }
    }

    private fun recarregarDesejo() {
        val d = desejo ?: return
        try {
            val dao = DesejoDAO(this)
            dao.open()
            val atualizado = dao.buscarPorId(d.id)
            dao.close()
            if (atualizado != null) {
                desejo = atualizado
                carregarCampos(atualizado)
            }
        } catch (e: Exception) {
            Timber.e(e, "recarregarDesejo: failed for desejo id=${d.id}")
        }
    }
}
