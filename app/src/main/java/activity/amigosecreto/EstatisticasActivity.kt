package activity.amigosecreto

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import activity.amigosecreto.db.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class EstatisticasActivity : AppCompatActivity() {

    @Inject
    lateinit var db: AppDatabase

    private lateinit var tvEstatTotalGrupos: TextView
    private lateinit var tvEstatTotalParticipantes: TextView
    private lateinit var tvEstatTotalSorteios: TextView
    private lateinit var tvEstatTotalDesejos: TextView
    private lateinit var tvEstatMediaValor: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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

        carregarEstatisticas()
    }

    private fun carregarEstatisticas() {
        lifecycleScope.launch {
            try {
                val totalGrupos: Int
                val totalParticipantes: Int
                val totalSorteios: Int
                val totalDesejos: Int
                val mediaValor: Double?

                withContext(Dispatchers.IO) {
                    totalGrupos = db.grupoDao().contarGrupos()
                    totalParticipantes = db.grupoDao().contarParticipantes()
                    totalSorteios = db.grupoDao().contarSorteios()
                    totalDesejos = db.grupoDao().contarDesejos()
                    mediaValor = db.grupoDao().mediaValorDesejos()
                }

                tvEstatTotalGrupos.text = totalGrupos.toString()
                tvEstatTotalParticipantes.text = totalParticipantes.toString()
                tvEstatTotalSorteios.text = totalSorteios.toString()
                tvEstatTotalDesejos.text = totalDesejos.toString()
                tvEstatMediaValor.text = if (mediaValor != null && mediaValor > 0) {
                    getString(R.string.estatisticas_valor_format, String.format(Locale("pt", "BR"), "%.2f", mediaValor))
                } else {
                    getString(R.string.dashboard_nao_definido)
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar estatísticas")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
