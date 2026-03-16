package activity.amigosecreto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.ParticipanteDAO
import activity.amigosecreto.db.DesejoDAO
import com.google.android.material.button.MaterialButton

class RevelarAmigoActivity : AppCompatActivity() {

    private var amigoSorteado: Participante? = null
    private var revelado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_revelar_amigo)

        // Aplica padding de sistema no root para que o conteúdo não fique
        // atrás da status bar ou navigation bar (edge-to-edge no Android 15+).
        // Os valores originais do XML são capturados fora do lambda porque o listener
        // pode ser invocado múltiplas vezes (ex: rotação) e setPadding sobrescreveria
        // os insets acumulados em chamadas anteriores.
        val rootView = findViewById<View>(R.id.root_revelar)
        if (rootView != null) {
            val padLeft = rootView.paddingLeft
            val padRight = rootView.paddingRight
            val padTop = rootView.paddingTop
            val padBottom = rootView.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(padLeft, padTop + systemBars.top, padRight, padBottom + systemBars.bottom)
                insets
            }
        }

        @Suppress("DEPRECATION")
        val participante = intent.getSerializableExtra("participante") as? Participante
        val dao = ParticipanteDAO(this)
        val desejoDAO = DesejoDAO(this)

        val tvNomeUsuario = findViewById<TextView>(R.id.tv_nome_usuario)
        val tvAmigoSorteado = findViewById<TextView>(R.id.tv_amigo_sorteado)
        val layoutRevelado = findViewById<View>(R.id.layout_revelado)
        val layoutEscondido = findViewById<View>(R.id.layout_escondido)
        val cardRevelacao = findViewById<View>(R.id.card_revelacao)
        val btnVoltar = findViewById<View>(R.id.btn_voltar)
        val btnVerDesejos = findViewById<MaterialButton>(R.id.btn_ver_desejos)

        if (participante != null) {
            tvNomeUsuario.text = participante.nome
            val amigoId = participante.amigoSorteadoId?.takeIf { it > 0 }
            if (amigoId != null) {
                dao.open()
                val nomeAmigo = dao.getNomeAmigoSorteado(amigoId)
                amigoSorteado = dao.buscarPorId(amigoId)
                dao.close()
                tvAmigoSorteado.text = nomeAmigo
            } else {
                tvAmigoSorteado.setText(R.string.label_no_draw)
            }
        }

        btnVerDesejos.setOnClickListener {
            val amigo = amigoSorteado
            if (amigo != null) {
                desejoDAO.open()
                val count = desejoDAO.contarDesejosPorParticipante(amigo.id)
                desejoDAO.close()
                if (count > 0) {
                    val intent = Intent(this, VisualizarDesejosActivity::class.java)
                    intent.putExtra("participante", amigo)
                    intent.putExtra("somente_visualizar", true)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, getString(R.string.toast_no_wishes_format, amigo.nome), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, R.string.label_no_draw, Toast.LENGTH_SHORT).show()
            }
        }

        cardRevelacao.setOnClickListener {
            if (!revelado) {
                val amigoId = participante?.amigoSorteadoId?.takeIf { it > 0 }
                if (amigoId != null) {
                    layoutEscondido.visibility = View.GONE
                    layoutRevelado.visibility = View.VISIBLE
                    btnVerDesejos.visibility = View.VISIBLE
                    revelado = true
                } else {
                    Toast.makeText(this, R.string.error_no_draw, Toast.LENGTH_SHORT).show()
                }
            } else {
                layoutEscondido.visibility = View.VISIBLE
                layoutRevelado.visibility = View.GONE
                btnVerDesejos.visibility = View.GONE
                revelado = false
            }
        }

        btnVoltar.setOnClickListener { finish() }
    }
}
