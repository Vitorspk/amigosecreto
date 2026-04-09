package activity.amigosecreto

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.repository.DesejoRepository
import activity.amigosecreto.util.WindowInsetsUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

@AndroidEntryPoint
class InserirDesejoActivity : AppCompatActivity() {

    companion object {
        /** Optional Intent extra: ID of the participant this wish belongs to.
         *  When absent (e.g. launched from ListarDesejos), participanteId stays 0 (no FK). */
        const val EXTRA_PARTICIPANTE_ID = "extra_participante_id"
    }

    private lateinit var etProduto: TextInputEditText
    private lateinit var etCategoria: TextInputEditText
    private lateinit var etPrecoMinimo: TextInputEditText
    private lateinit var etPrecoMaximo: TextInputEditText
    private lateinit var etLojas: TextInputEditText
    private lateinit var btnSalvar: MaterialButton

    @Inject lateinit var repo: DesejoRepository

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
        btnSalvar = findViewById(R.id.btn_salvar_ins)

        btnSalvar.setOnClickListener {
            if (validar()) {
                btnSalvar.isEnabled = false
                inserir()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.inserir_desejo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_salvar -> {
            if (validar()) {
                item.isEnabled = false
                inserir()
            }
            true
        }
        android.R.id.home -> {
            finish()
            true
        }
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
        val desejo = try {
            Desejo().apply {
                produto = etProduto.text.toString().trim()
                categoria = etCategoria.text.toString().trim()
                val pMin = etPrecoMinimo.text.toString().trim().replace(",", ".")
                precoMinimo = if (pMin.isEmpty()) 0.0 else pMin.toDouble()
                val pMax = etPrecoMaximo.text.toString().trim().replace(",", ".")
                precoMaximo = if (pMax.isEmpty()) 0.0 else pMax.toDouble()
                lojas = etLojas.text.toString().trim()
                // participanteId is optional: 0 when launched from ListarDesejos (global wish list);
                // set when launched from ParticipanteDesejosActivity via EXTRA_PARTICIPANTE_ID.
                participanteId = intent.getIntExtra(EXTRA_PARTICIPANTE_ID, 0)
            }
        } catch (e: NumberFormatException) {
            btnSalvar.isEnabled = true
            Toast.makeText(this, R.string.error_invalid_price, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { repo.inserir(desejo) }
                if (!isDestroyed && !isFinishing) {
                    Toast.makeText(this@InserirDesejoActivity, R.string.toast_wish_saved, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                if (!isDestroyed && !isFinishing) {
                    btnSalvar.isEnabled = true
                    invalidateOptionsMenu()
                    val msg = e.message ?: getString(R.string.error_unknown)
                    Toast.makeText(this@InserirDesejoActivity, getString(R.string.error_save_wish_format, msg), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
