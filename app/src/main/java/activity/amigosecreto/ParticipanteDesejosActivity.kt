package activity.amigosecreto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import activity.amigosecreto.util.WindowInsetsUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

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
        toolbar.title = "${participante.nome} 🎁"
        toolbar.setNavigationOnClickListener { finish() }

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
                desejo.id = desejoDAO.proximoId()
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
            view.findViewById<TextView>(R.id.tv_item_categoria).text = desejo.categoria

            val tvPreco = view.findViewById<TextView>(R.id.tv_item_preco)
            if (desejo.precoMinimo > 0 || desejo.precoMaximo > 0) {
                val nf = WindowInsetsUtils.numberFormatPtBr()
                tvPreco.text = "R$ ${nf.format(desejo.precoMinimo)} - R$ ${nf.format(desejo.precoMaximo)}"
            } else {
                tvPreco.text = ""
            }

            view.setOnClickListener { mostrarOpcoesDesejo(desejo) }
            return view
        }
    }
}
