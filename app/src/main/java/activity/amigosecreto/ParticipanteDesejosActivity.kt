package activity.amigosecreto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.DesejoDAO
import activity.amigosecreto.db.Participante
import activity.amigosecreto.util.GeminiClient
import activity.amigosecreto.util.WindowInsetsUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParticipanteDesejosActivity : AppCompatActivity() {

    private companion object {
        const val REQUEST_EDIT_DESEJO = 100
    }

    private lateinit var participante: Participante
    private lateinit var desejoDAO: DesejoDAO
    private val listaDesejos = mutableListOf<Desejo>()
    private lateinit var lvDesejos: ListView
    private lateinit var tvPresentesCount: TextView
    private lateinit var layoutEmpty: View
    private lateinit var adapter: DesejosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_participante_desejos)

        desejoDAO = DesejoDAO(this)
        desejoDAO.open()

        @Suppress("DEPRECATION")
        val p = intent.getSerializableExtra("participante") as? Participante
        if (p == null) {
            Toast.makeText(this, R.string.error_participant_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        participante = p

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "${participante.nome} 🎁"
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item -> onMenuItemSelected(item) }

        lvDesejos = findViewById(R.id.lv_desejos)
        tvPresentesCount = findViewById(R.id.tv_presentes_count)
        layoutEmpty = findViewById(R.id.layout_empty)
        val btnAddDesejo = findViewById<MaterialButton>(R.id.btn_add_desejo)

        adapter = DesejosAdapter(this, listaDesejos)
        lvDesejos.adapter = adapter

        btnAddDesejo.setOnClickListener { mostrarDialogAdicionarDesejo() }

        carregarDesejos()
    }

    private fun carregarDesejos() {
        listaDesejos.clear()
        listaDesejos.addAll(desejoDAO.listarPorParticipante(participante.id))
        adapter.notifyDataSetChanged()

        tvPresentesCount.text = getString(R.string.label_wishes_count_format, listaDesejos.size)

        val isEmpty = listaDesejos.isEmpty()
        layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        lvDesejos.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun mostrarDialogAdicionarDesejo() {
        val view = layoutInflater.inflate(R.layout.dialog_add_desejo, null)
        val etProduto = view.findViewById<TextInputEditText>(R.id.et_produto)
        val etCategoria = view.findViewById<TextInputEditText>(R.id.et_categoria)
        val etPrecoMin = view.findViewById<TextInputEditText>(R.id.et_preco_minimo)
        val etPrecoMax = view.findViewById<TextInputEditText>(R.id.et_preco_maximo)
        val etLojas = view.findViewById<TextInputEditText>(R.id.et_lojas)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setTitle(R.string.dialog_add_wish_title)
            .setPositiveButton(R.string.button_save, null)
            .setNegativeButton(R.string.button_cancel, null)
            .create()
        dialog.show()

        // Override the positive button to prevent auto-dismiss on validation failure
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val produto = etProduto.text?.toString()?.trim() ?: ""
            if (produto.isEmpty()) {
                Toast.makeText(this, R.string.error_product_name_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val desejo = Desejo()
                desejo.produto = produto
                desejo.categoria = etCategoria.text?.toString()?.trim() ?: ""

                // Tratar preços - substituir vírgula por ponto para parseDouble
                val precoMinStr = etPrecoMin.text?.toString()?.trim()?.replace(",", ".") ?: ""
                val precoMaxStr = etPrecoMax.text?.toString()?.trim()?.replace(",", ".") ?: ""
                desejo.precoMinimo = if (precoMinStr.isEmpty()) 0.0 else precoMinStr.toDouble()
                desejo.precoMaximo = if (precoMaxStr.isEmpty()) 0.0 else precoMaxStr.toDouble()

                desejo.lojas = etLojas.text?.toString()?.trim() ?: ""
                desejo.participanteId = participante.id

                desejoDAO.inserir(desejo)
                Toast.makeText(this, R.string.toast_wish_added, Toast.LENGTH_SHORT).show()
                carregarDesejos()
                dialog.dismiss()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, R.string.error_invalid_price, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                val msg = e.message ?: getString(R.string.error_unknown)
                Toast.makeText(this, getString(R.string.error_generic_format, msg), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarOpcoesDesejo(desejo: Desejo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_opcoes_desejo, null)
        val tvProdutoNome = dialogView.findViewById<TextView>(R.id.tv_produto_nome)
        val cardEditar = dialogView.findViewById<MaterialCardView>(R.id.card_editar)
        val cardRemover = dialogView.findViewById<MaterialCardView>(R.id.card_remover)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btn_cancelar)

        tvProdutoNome.text = desejo.produto

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Tornar o fundo transparente para mostrar os cantos arredondados
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        cardEditar.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, AlterarDesejoActivity::class.java)
            intent.putExtra("desejo", desejo)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_EDIT_DESEJO)
        }

        cardRemover.setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_remove_wish_title)
                .setMessage(getString(R.string.dialog_remove_wish_message_format, desejo.produto))
                .setPositiveButton(R.string.button_remove_yes) { _, _ ->
                    desejoDAO.remover(desejo)
                    Toast.makeText(this, R.string.toast_wish_removed, Toast.LENGTH_SHORT).show()
                    carregarDesejos()
                }
                .setNegativeButton(R.string.button_cancel, null)
                .show()
        }

        btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_compartilhar_desejos -> { compartilharListaDesejos(); true }
            R.id.action_sugerir_presentes -> { sugerirPresentesIA(); true }
            else -> false
        }
    }

    private fun compartilharListaDesejos() {
        val nomeParticipante = participante.nome ?: ""
        val texto = if (listaDesejos.isEmpty()) {
            getString(R.string.share_desejos_empty, nomeParticipante)
        } else {
            val nf = WindowInsetsUtils.currencyFormatPtBr()
            val sb = StringBuilder()
            sb.appendLine(getString(R.string.share_desejos_titulo, nomeParticipante))
            sb.appendLine()
            for (d in listaDesejos) {
                sb.appendLine(getString(R.string.share_desejos_item, d.produto))
                if (!d.categoria.isNullOrBlank()) {
                    sb.appendLine(getString(R.string.share_desejos_categoria, d.categoria))
                }
                val temMin = d.precoMinimo > 0
                val temMax = d.precoMaximo > 0
                when {
                    temMin && temMax -> sb.appendLine(getString(R.string.share_desejos_preco_completo, nf.format(d.precoMinimo), nf.format(d.precoMaximo)))
                    temMin -> sb.appendLine(getString(R.string.share_desejos_preco_minimo, nf.format(d.precoMinimo)))
                    temMax -> sb.appendLine(getString(R.string.share_desejos_preco_maximo, nf.format(d.precoMaximo)))
                }
                if (!d.lojas.isNullOrBlank()) {
                    sb.appendLine(getString(R.string.share_desejos_lojas, d.lojas))
                }
                sb.appendLine()
            }
            sb.toString().trimEnd()
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_desejos_titulo, nomeParticipante))
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_compartilhar_desejos)))
    }

    private fun sugerirPresentesIA() {
        if (!GeminiClient.isDisponivel) {
            Toast.makeText(this, R.string.error_gemini_nao_configurado, Toast.LENGTH_LONG).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_sugestoes_titulo)
            .setMessage(R.string.dialog_sugestoes_carregando)
            .setCancelable(false)
            .create()
        progressDialog.show()

        val snapshot = listaDesejos.toList()
        val nomeParticipante = participante.nome ?: ""

        CoroutineScope(Dispatchers.Main).launch {
            val sugestoes = withContext(Dispatchers.IO) {
                GeminiClient.sugerirPresentes(nomeParticipante, snapshot)
            }
            progressDialog.dismiss()

            if (sugestoes == null) {
                Toast.makeText(this@ParticipanteDesejosActivity, R.string.error_gemini_falhou, Toast.LENGTH_LONG).show()
                return@launch
            }

            AlertDialog.Builder(this@ParticipanteDesejosActivity)
                .setTitle(R.string.dialog_sugestoes_titulo)
                .setMessage(sugestoes)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.menu_compartilhar) { _, _ ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, sugestoes)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.action_compartilhar_desejos)))
                }
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        desejoDAO.close()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_DESEJO && resultCode == RESULT_OK) {
            carregarDesejos()
        }
    }

    private inner class DesejosAdapter(
        private val context: Context,
        private val desejos: List<Desejo>
    ) : BaseAdapter() {

        override fun getCount() = desejos.size
        override fun getItem(position: Int) = desejos[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_desejo, parent, false)
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

            view.setOnClickListener { mostrarOpcoesDesejo(desejo) }
            return view
        }
    }
}
