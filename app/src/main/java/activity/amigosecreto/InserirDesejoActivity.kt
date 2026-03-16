package activity.amigosecreto

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.DesejoDAO
import activity.amigosecreto.util.WindowInsetsUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class InserirDesejoActivity : AppCompatActivity() {

    private lateinit var etProduto: TextInputEditText
    private lateinit var etCategoria: TextInputEditText
    private lateinit var etPrecoMinimo: TextInputEditText
    private lateinit var etPrecoMaximo: TextInputEditText
    private lateinit var etLojas: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inserir_desejo)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        WindowInsetsUtils.applyImeBottomPadding(findViewById(R.id.scroll_inserir_desejo))

        etProduto = findViewById(R.id.et_produto_ins)
        etCategoria = findViewById(R.id.et_categoria_ins)
        etPrecoMinimo = findViewById(R.id.et_preco_minimo_ins)
        etPrecoMaximo = findViewById(R.id.et_preco_maximo_ins)
        etLojas = findViewById(R.id.et_lojas_ins)

        findViewById<MaterialButton>(R.id.btn_salvar_ins)?.setOnClickListener {
            if (validar()) {
                inserir()
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.inserir_desejo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean = when (item.itemId) {
        R.id.menu_salvar -> {
            if (validar()) {
                inserir()
                finish()
            }
            true
        }
        android.R.id.home -> { finish(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun validar(): Boolean {
        if (etProduto.text.toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.error_product_name_required, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun inserir() {
        try {
            val dao = DesejoDAO(this)
            dao.open()
            val desejo = Desejo()
            desejo.id = dao.proximoId()
            desejo.produto = etProduto.text.toString().trim()
            desejo.categoria = etCategoria.text.toString().trim()

            val pMin = etPrecoMinimo.text.toString().trim().replace(",", ".")
            desejo.precoMinimo = if (pMin.isEmpty()) 0.0 else pMin.toDouble()

            val pMax = etPrecoMaximo.text.toString().trim().replace(",", ".")
            desejo.precoMaximo = if (pMax.isEmpty()) 0.0 else pMax.toDouble()

            desejo.lojas = etLojas.text.toString().trim()
            dao.inserir(desejo)
            dao.close()
            Toast.makeText(this, R.string.toast_wish_saved, Toast.LENGTH_SHORT).show()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, R.string.error_invalid_price, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            val msg = e.message ?: getString(R.string.error_unknown)
            Toast.makeText(this, getString(R.string.error_save_wish_format, msg), Toast.LENGTH_LONG).show()
        }
    }
}
