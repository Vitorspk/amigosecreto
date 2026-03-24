package activity.amigosecreto

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ConfiguracoesGrupoActivity : AppCompatActivity() {

    @Inject
    lateinit var db: AppDatabase

    private lateinit var grupoAtual: Grupo

    private lateinit var etNome: TextInputEditText
    private lateinit var etDescricao: TextInputEditText
    private lateinit var etLocalEvento: TextInputEditText
    private lateinit var etDataLimiteSorteio: TextInputEditText
    private lateinit var etRegras: TextInputEditText
    private lateinit var etValorMinimo: TextInputEditText
    private lateinit var etValorMaximo: TextInputEditText
    private lateinit var switchPermitirVerDesejos: SwitchMaterial
    private lateinit var switchExigirConfirmacao: SwitchMaterial
    private lateinit var btnSalvar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_configuracoes_grupo)

        @Suppress("DEPRECATION")
        grupoAtual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("grupo", Grupo::class.java)!!
        } else {
            intent.getSerializableExtra("grupo") as Grupo
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_configuracoes_grupo)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.configuracoes_grupo_titulo)
            setDisplayHomeAsUpEnabled(true)
        }

        etNome = findViewById(R.id.et_config_nome)
        etDescricao = findViewById(R.id.et_config_descricao)
        etLocalEvento = findViewById(R.id.et_config_local_evento)
        etDataLimiteSorteio = findViewById(R.id.et_config_data_limite)
        etRegras = findViewById(R.id.et_config_regras)
        etValorMinimo = findViewById(R.id.et_config_valor_minimo)
        etValorMaximo = findViewById(R.id.et_config_valor_maximo)
        switchPermitirVerDesejos = findViewById(R.id.switch_permitir_ver_desejos)
        switchExigirConfirmacao = findViewById(R.id.switch_exigir_confirmacao)
        btnSalvar = findViewById(R.id.btn_salvar_configuracoes)

        preencherFormulario()

        btnSalvar.setOnClickListener { salvar() }
    }

    private fun preencherFormulario() {
        etNome.setText(grupoAtual.nome)
        etDescricao.setText(grupoAtual.descricao)
        etLocalEvento.setText(grupoAtual.localEvento)
        etDataLimiteSorteio.setText(grupoAtual.dataLimiteSorteio)
        etRegras.setText(grupoAtual.regras)
        if (grupoAtual.valorMinimo > 0) etValorMinimo.setText(grupoAtual.valorMinimo.toString())
        if (grupoAtual.valorMaximo > 0) etValorMaximo.setText(grupoAtual.valorMaximo.toString())
        switchPermitirVerDesejos.isChecked = grupoAtual.permitirVerDesejos
        switchExigirConfirmacao.isChecked = grupoAtual.exigirConfirmacaoCompra
    }

    private fun salvar() {
        val nome = etNome.text?.toString()?.trim() ?: ""
        if (nome.isEmpty()) {
            Toast.makeText(this, R.string.grupo_erro_nome_obrigatorio, Toast.LENGTH_SHORT).show()
            return
        }

        val valorMinimo = etValorMinimo.text?.toString()?.toDoubleOrNull() ?: 0.0
        val valorMaximo = etValorMaximo.text?.toString()?.toDoubleOrNull() ?: 0.0
        if (valorMinimo > 0 && valorMaximo > 0 && valorMinimo > valorMaximo) {
            Toast.makeText(this, R.string.configuracoes_erro_faixa_valor, Toast.LENGTH_SHORT).show()
            return
        }

        grupoAtual.nome = nome
        grupoAtual.descricao = etDescricao.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        grupoAtual.localEvento = etLocalEvento.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        grupoAtual.dataLimiteSorteio = etDataLimiteSorteio.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        grupoAtual.regras = etRegras.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        grupoAtual.valorMinimo = valorMinimo
        grupoAtual.valorMaximo = valorMaximo
        grupoAtual.permitirVerDesejos = switchPermitirVerDesejos.isChecked
        grupoAtual.exigirConfirmacaoCompra = switchExigirConfirmacao.isChecked

        btnSalvar.isEnabled = false
        lifecycleScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) {
                    db.grupoDao().atualizar(grupoAtual)
                }
                if (rows > 0) {
                    Toast.makeText(this@ConfiguracoesGrupoActivity, R.string.configuracoes_salvas, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK, Intent().putExtra("grupo", grupoAtual))
                    finish()
                } else {
                    Toast.makeText(this@ConfiguracoesGrupoActivity, R.string.grupo_erro_salvar, Toast.LENGTH_SHORT).show()
                    btnSalvar.isEnabled = true
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao salvar configurações do grupo")
                Toast.makeText(this@ConfiguracoesGrupoActivity, R.string.grupo_erro_salvar, Toast.LENGTH_SHORT).show()
                btnSalvar.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
