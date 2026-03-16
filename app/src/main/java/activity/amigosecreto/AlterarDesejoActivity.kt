package activity.amigosecreto

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.DesejoDAO
import activity.amigosecreto.util.WindowInsetsUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AlterarDesejoActivity : AppCompatActivity() {

    private companion object { const val TAG = "AlterarDesejoActivity" }

    private lateinit var oldDesejo: Desejo

    private lateinit var etProduto: TextInputEditText
    private lateinit var etCategoria: TextInputEditText
    private lateinit var etPrecoMinimo: TextInputEditText
    private lateinit var etPrecoMaximo: TextInputEditText
    private lateinit var etLojas: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alterar_desejo)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        WindowInsetsUtils.applyImeBottomPadding(findViewById(R.id.scroll_alterar_desejo))

        val desejo = intent.extras?.get("desejo") as? Desejo
        if (desejo == null) {
            Toast.makeText(this, R.string.error_load_wish, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        oldDesejo = desejo

        etProduto = findViewById(R.id.et_produto)
        etCategoria = findViewById(R.id.et_categoria)
        etPrecoMinimo = findViewById(R.id.et_preco_minimo)
        etPrecoMaximo = findViewById(R.id.et_preco_maximo)
        etLojas = findViewById(R.id.et_lojas)

        findViewById<MaterialButton>(R.id.btn_atualizar).setOnClickListener {
            if (validar()) {
                alterar()
                setResult(RESULT_OK)
                finish()
            }
        }

        etProduto.setText(oldDesejo.produto)
        etCategoria.setText(oldDesejo.categoria)

        // Formatar preços para exibição nos campos de edição.
        // Intencional: usa "1.500,00" (sem prefixo R$) pois o layout já exibe
        // o prefixo "R$ " via app:prefixText. A tela de detalhes usa currencyFormat
        // que inclui o símbolo — essa diferença é esperada e facilita a digitação.
        if (oldDesejo.precoMinimo > 0) {
            etPrecoMinimo.setText(String.format(WindowInsetsUtils.LOCALE_PT_BR, "%.2f", oldDesejo.precoMinimo))
        }
        if (oldDesejo.precoMaximo > 0) {
            etPrecoMaximo.setText(String.format(WindowInsetsUtils.LOCALE_PT_BR, "%.2f", oldDesejo.precoMaximo))
        }

        etLojas.setText(oldDesejo.lojas)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.alterar_desejo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean = when (item.itemId) {
        R.id.menu_salvar -> {
            if (validar()) {
                alterar()
                setResult(DetalheDesejoActivity.RESULT_SAVE)
                finish()
            }
            true
        }
        R.id.menu_excluir -> {
            remover()
            setResult(DetalheDesejoActivity.RESULT_REMOVE)
            finish()
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

    private fun remover() {
        val dao = DesejoDAO(this)
        try {
            dao.open()
            dao.remover(oldDesejo)
            Toast.makeText(this, R.string.toast_wish_deleted, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "remover: failed for desejo id=${oldDesejo.id}", e)
        } finally {
            dao.close()
        }
    }

    private fun alterar() {
        val dao = DesejoDAO(this)
        try {
            dao.open()
            val newDesejo = Desejo()
            newDesejo.id = oldDesejo.id
            newDesejo.produto = etProduto.text.toString().trim()
            newDesejo.categoria = etCategoria.text.toString().trim()

            val pMin = etPrecoMinimo.text.toString().trim().replace(",", ".")
            newDesejo.precoMinimo = if (pMin.isEmpty()) 0.0 else pMin.toDouble()

            val pMax = etPrecoMaximo.text.toString().trim().replace(",", ".")
            newDesejo.precoMaximo = if (pMax.isEmpty()) 0.0 else pMax.toDouble()

            newDesejo.lojas = etLojas.text.toString().trim()

            // Importante: preservar o participanteId do desejo original
            newDesejo.participanteId = oldDesejo.participanteId

            dao.alterar(oldDesejo, newDesejo)
            Toast.makeText(this, R.string.toast_wish_updated, Toast.LENGTH_SHORT).show()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, R.string.error_invalid_price, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            val msg = e.message ?: getString(R.string.error_unknown)
            Toast.makeText(this, getString(R.string.error_update_wish_format, msg), Toast.LENGTH_LONG).show()
        } finally {
            dao.close()
        }
    }
}
