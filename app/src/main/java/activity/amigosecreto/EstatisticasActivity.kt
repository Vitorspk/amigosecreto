package activity.amigosecreto

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import activity.amigosecreto.util.WindowInsetsUtils

@AndroidEntryPoint
class EstatisticasActivity : AppCompatActivity() {

    private val viewModel: EstatisticasViewModel by viewModels()

    private lateinit var tvEstatTotalGrupos: TextView
    private lateinit var tvEstatTotalParticipantes: TextView
    private lateinit var tvEstatTotalSorteios: TextView
    private lateinit var tvEstatTotalDesejos: TextView
    private lateinit var tvEstatMediaValor: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_estatisticas)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_estatisticas)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.estatisticas_titulo)
            setDisplayHomeAsUpEnabled(true)
        }

        tvEstatTotalGrupos = findViewById(R.id.tv_estat_total_grupos)
        tvEstatTotalParticipantes = findViewById(R.id.tv_estat_total_participantes)
        tvEstatTotalSorteios = findViewById(R.id.tv_estat_total_sorteios)
        tvEstatTotalDesejos = findViewById(R.id.tv_estat_total_desejos)
        tvEstatMediaValor = findViewById(R.id.tv_estat_media_valor)

        viewModel.uiState.observe(this) { state ->
            if (state.error != null) {
                Toast.makeText(this, R.string.error_load_share_data, Toast.LENGTH_SHORT).show()
                return@observe
            }
            tvEstatTotalGrupos.text = state.totalGrupos.toString()
            tvEstatTotalParticipantes.text = state.totalParticipantes.toString()
            tvEstatTotalSorteios.text = state.totalSorteios.toString()
            tvEstatTotalDesejos.text = state.totalDesejos.toString()
            val mediaValor = state.mediaValor
            tvEstatMediaValor.text = if (mediaValor != null && mediaValor > 0) {
                getString(
                    R.string.estatisticas_valor_format,
                    WindowInsetsUtils.numberFormatPtBr().format(mediaValor)
                )
            } else {
                getString(R.string.estatisticas_sem_dados)
            }
        }

        viewModel.carregarEstatisticas()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
