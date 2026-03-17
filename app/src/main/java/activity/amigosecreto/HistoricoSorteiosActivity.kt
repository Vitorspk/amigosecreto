package activity.amigosecreto

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import activity.amigosecreto.adapter.SorteiosAdapter
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Sorteio
import activity.amigosecreto.util.AsyncDatabaseHelper
import activity.amigosecreto.util.StateViewHelper

class HistoricoSorteiosActivity : AppCompatActivity() {

    private lateinit var grupoAtual: Grupo
    private lateinit var rvHistorico: RecyclerView
    private lateinit var stateHelper: StateViewHelper
    private val listaSorteios = mutableListOf<Sorteio>()
    private lateinit var adapter: SorteiosAdapter
    private lateinit var sorteioRepository: activity.amigosecreto.repository.SorteioRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico_sorteios)

        grupoAtual = intent.getSerializableExtra("grupo") as Grupo
        sorteioRepository = activity.amigosecreto.repository.SorteioRepository(this)

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

        carregarHistorico()
    }

    private fun carregarHistorico() {
        stateHelper.showLoading()
        AsyncDatabaseHelper.execute(
            object : AsyncDatabaseHelper.BackgroundTask<List<Sorteio>> {
                override fun doInBackground(): List<Sorteio> =
                    sorteioRepository.listarPorGrupo(grupoAtual.id)
            },
            object : AsyncDatabaseHelper.ResultCallback<List<Sorteio>> {
                override fun onSuccess(result: List<Sorteio>) {
                    listaSorteios.clear()
                    listaSorteios.addAll(result)
                    if (listaSorteios.isEmpty()) stateHelper.showEmpty()
                    else stateHelper.showContent()
                    adapter.notifyDataSetChanged()
                }

                override fun onError(e: Exception) {
                    android.util.Log.e("HistoricoSorteios", "Erro ao carregar histórico", e)
                    stateHelper.showEmpty()
                }
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
