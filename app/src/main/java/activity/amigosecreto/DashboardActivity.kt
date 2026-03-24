package activity.amigosecreto

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    @Inject
    lateinit var db: AppDatabase

    private lateinit var grupoAtual: Grupo

    private lateinit var tvDashNomeGrupo: TextView
    private lateinit var tvDashTotalParticipantes: TextView
    private lateinit var tvDashSorteioRealizado: TextView
    private lateinit var tvDashConfirmados: TextView
    private lateinit var tvDashDataEvento: TextView
    private lateinit var tvDashLocalEvento: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        @Suppress("DEPRECATION")
        grupoAtual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("grupo", Grupo::class.java)!!
        } else {
            intent.getSerializableExtra("grupo") as Grupo
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_dashboard)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.dashboard_titulo)
            setDisplayHomeAsUpEnabled(true)
        }

        tvDashNomeGrupo = findViewById(R.id.tv_dash_nome_grupo)
        tvDashTotalParticipantes = findViewById(R.id.tv_dash_total_participantes)
        tvDashSorteioRealizado = findViewById(R.id.tv_dash_sorteio_realizado)
        tvDashConfirmados = findViewById(R.id.tv_dash_confirmados)
        tvDashDataEvento = findViewById(R.id.tv_dash_data_evento)
        tvDashLocalEvento = findViewById(R.id.tv_dash_local_evento)

        tvDashNomeGrupo.text = grupoAtual.nome

        carregarDados()
    }

    private fun carregarDados() {
        lifecycleScope.launch {
            try {
                val totalParticipantes: Int
                val sorteioRealizado: Boolean
                val confirmados: Int

                withContext(Dispatchers.IO) {
                    totalParticipantes = db.participanteDao().contarPorGrupo(grupoAtual.id)
                    val participantes = db.participanteDao().listarPorGrupoSemExclusoes(grupoAtual.id)
                    sorteioRealizado = participantes.any { it.amigoSorteadoId != null && it.amigoSorteadoId!! > 0 }
                    confirmados = db.participanteDao().contarConfirmados(grupoAtual.id)
                }

                tvDashTotalParticipantes.text = totalParticipantes.toString()
                tvDashSorteioRealizado.text = if (sorteioRealizado)
                    getString(R.string.dashboard_sim)
                else
                    getString(R.string.dashboard_nao)
                tvDashConfirmados.text = confirmados.toString()

                val dataEvento = grupoAtual.dataEvento
                tvDashDataEvento.text = if (!dataEvento.isNullOrEmpty()) dataEvento
                else getString(R.string.dashboard_nao_definido)

                val localEvento = grupoAtual.localEvento
                tvDashLocalEvento.text = if (!localEvento.isNullOrEmpty()) localEvento
                else getString(R.string.dashboard_nao_definido)
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar dados do dashboard")
                Toast.makeText(this@DashboardActivity, R.string.error_load_share_data, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
