package activity.amigosecreto

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import activity.amigosecreto.adapter.SorteiosAdapter
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.util.StateViewHelper

@AndroidEntryPoint
class HistoricoSorteiosActivity : AppCompatActivity() {

    private val viewModel: HistoricoSorteiosViewModel by viewModels()
    private lateinit var grupoAtual: Grupo
    private lateinit var rvHistorico: RecyclerView
    private lateinit var stateHelper: StateViewHelper
    private val listaSorteios = mutableListOf<Sorteio>()
    private lateinit var adapter: SorteiosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historico_sorteios)

        @Suppress("DEPRECATION")
        val g = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(Grupo.EXTRA_GRUPO, Grupo::class.java)
        } else {
            intent.getSerializableExtra(Grupo.EXTRA_GRUPO) as? Grupo
        }
        if (g == null) { finish(); return }
        grupoAtual = g

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar_historico)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.historico_titulo)
            setDisplayHomeAsUpEnabled(true)
        }

        rvHistorico = findViewById(R.id.rv_historico)
        rvHistorico.layoutManager = LinearLayoutManager(this)
        adapter = SorteiosAdapter(this, listaSorteios)
        rvHistorico.adapter = adapter

        stateHelper = StateViewHelper(
            stubLoading = findViewById(R.id.stub_loading),
            stubEmpty = findViewById(R.id.stub_empty),
            contentView = rvHistorico
        )

        observeViewModel()
        viewModel.carregarHistorico(grupoAtual.id)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            if (loading) stateHelper.showLoading()
        }
        viewModel.sorteios.observe(this) { sorteios ->
            listaSorteios.clear()
            listaSorteios.addAll(sorteios)
            if (listaSorteios.isEmpty()) stateHelper.showEmpty()
            else stateHelper.showContent()
            adapter.notifyDataSetChanged()
        }
        viewModel.hasError.observe(this) { error ->
            if (error == true) stateHelper.showEmpty()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
