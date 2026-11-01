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
import activity.amigosecreto.repository.ParticipanteRepository
import activity.amigosecreto.repository.DesejoRepository
import activity.amigosecreto.util.AsyncDatabaseHelper
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton

@AndroidEntryPoint
class RevelarAmigoActivity : AppCompatActivity() {

    private var amigoSorteado: Participante? = null
    private var revelado = false
    @Inject lateinit var participanteRepository: ParticipanteRepository
    @Inject lateinit var desejoRepository: DesejoRepository

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
        // Padrão false: quem abrir a revelação sem informar a configuração do grupo não expõe desejos.
        val permitirVerDesejos = intent.getBooleanExtra(EXTRA_PERMITIR_VER_DESEJOS, false)

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
                cardRevelacao.isEnabled = false
                lifecycleScope.launch {
                    AsyncDatabaseHelper.idlingResource.increment()
                    try {
                        amigoSorteado = participanteRepository.buscarPorId(amigoId)
                        tvAmigoSorteado.text = amigoSorteado?.nome ?: getString(R.string.label_no_draw)
                        cardRevelacao.isEnabled = amigoSorteado != null
                    } finally {
                        AsyncDatabaseHelper.idlingResource.decrement()
                    }
                }
            } else {
                tvAmigoSorteado.setText(R.string.label_no_draw)
            }
        }

        btnVerDesejos.setOnClickListener {
            val amigo = amigoSorteado
            if (amigo != null) {
                lifecycleScope.launch {
                    AsyncDatabaseHelper.idlingResource.increment()
                    try {
                        val count = desejoRepository.contarDesejosPorParticipante(amigo.id)
                        if (count > 0) {
                            val intent = Intent(this@RevelarAmigoActivity, VisualizarDesejosActivity::class.java)
                            intent.putExtra("participante", amigo)
                            intent.putExtra("somente_visualizar", true)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@RevelarAmigoActivity, getString(R.string.toast_no_wishes_format, amigo.nome), Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        AsyncDatabaseHelper.idlingResource.decrement()
                    }
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
                    btnVerDesejos.visibility = if (permitirVerDesejos) View.VISIBLE else View.GONE
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

    companion object {
        const val EXTRA_PERMITIR_VER_DESEJOS = "permitir_ver_desejos"
    }
}
