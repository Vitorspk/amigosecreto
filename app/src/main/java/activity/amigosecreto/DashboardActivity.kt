package activity.amigosecreto

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import activity.amigosecreto.db.Grupo

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    private var grupoId: Int = -1

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
        val g = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("grupo", Grupo::class.java)
        } else {
            intent.getSerializableExtra("grupo") as? Grupo
        }
        if (g == null) { finish(); return }
        grupoId = g.id

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

        viewModel.uiState.observe(this) { state ->
            if (state.error != null) {
                Toast.makeText(this, R.string.error_load_share_data, Toast.LENGTH_SHORT).show()
                return@observe
            }
            state.grupo?.let { grupo ->
                tvDashNomeGrupo.text = grupo.nome
                val dataEvento = grupo.dataEvento
                tvDashDataEvento.text = if (!dataEvento.isNullOrEmpty()) dataEvento
                else getString(R.string.dashboard_nao_definido)
                val localEvento = grupo.localEvento
                tvDashLocalEvento.text = if (!localEvento.isNullOrEmpty()) localEvento
                else getString(R.string.dashboard_nao_definido)
            }
            tvDashTotalParticipantes.text = state.totalParticipantes.toString()
            tvDashSorteioRealizado.text = if (state.sorteioRealizado)
                getString(R.string.dashboard_sim)
            else
                getString(R.string.dashboard_nao)
            tvDashConfirmados.text = state.confirmados.toString()
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarrega do banco a cada retorno para refletir edições feitas em ConfiguracoesGrupoActivity
        viewModel.carregarDados(grupoId)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
