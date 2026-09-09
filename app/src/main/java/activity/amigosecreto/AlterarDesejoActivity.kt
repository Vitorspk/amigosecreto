package activity.amigosecreto

import android.os.Bundle
import timber.log.Timber
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.repository.DesejoRepository
import activity.amigosecreto.util.WindowInsetsUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlterarDesejoActivity : AppCompatActivity() {

    // Room via Hilt. Não voltar ao DesejoDAO legado: abrir o MySQLiteOpenHelper (congelado
    // em DATABASE_VERSION = 10) sobre o banco que o Room migrou para 13 rebaixa o
    // user_version e faz o Room re-executar a MIGRATION_10_11 no start seguinte, perdendo
    // as colunas v12 de participante. Ver "DAOs legados" no CLAUDE.md.
    @Inject lateinit var desejoRepository: DesejoRepository

    /**
     * Impede double-tap em Salvar/Excluir. Enquanto a escrita era síncrona na main thread,
     * a própria thread bloqueada servia de trava; com a coroutine a UI fica livre e dois
     * toques rápidos disparariam duas operações antes do finish().
     */
    private var operacaoEmAndamento = false

    private lateinit var oldDesejo: Desejo

    private lateinit var etProduto: TextInputEditText
    private lateinit var etCategoria: TextInputEditText
    private lateinit var etPrecoMinimo: TextInputEditText
    private lateinit var etPrecoMaximo: TextInputEditText
    private lateinit var etLojas: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alterar_desejo)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        WindowInsetsUtils.applyImeBottomPadding(findViewById(R.id.scroll_alterar_desejo))

        val desejo = intent.extras?.get("desejo") as? Desejo
        if (desejo == null) {
            Toast.makeText(this, R.string.error_load_wish, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        oldDesejo = desejo

        etProduto = findViewById(R.id.et_produto)
        etCategoria = findViewById(R.id.et_categoria)
        etPrecoMinimo = findViewById(R.id.et_preco_minimo)
        etPrecoMaximo = findViewById(R.id.et_preco_maximo)
        etLojas = findViewById(R.id.et_lojas)

        findViewById<MaterialButton>(R.id.btn_atualizar).setOnClickListener {
            if (validar() && !operacaoEmAndamento) {
                operacaoEmAndamento = true
                // finish() dentro da coroutine: o lifecycleScope é cancelado no onDestroy,
                // então encerrar a Activity antes da escrita terminar a perderia.
                lifecycleScope.launch {
                    if (alterar()) {
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        // Falha ao salvar: mantém a tela aberta para o usuário corrigir,
                        // em vez de fechar aparentando sucesso.
                        operacaoEmAndamento = false
                    }
                }
            }
        }

        etProduto.setText(oldDesejo.produto)
        etCategoria.setText(oldDesejo.categoria)

        // Formatar preços para exibição nos campos de edição.
        // Intencional: usa "1.500,00" (sem prefixo R$) pois o layout já exibe
        // o prefixo "R$ " via app:prefixText. A tela de detalhes usa currencyFormat
        // que inclui o símbolo — essa diferença é esperada e facilita a digitação.
        if (oldDesejo.precoMinimo > 0) {
            etPrecoMinimo.setText(String.format(WindowInsetsUtils.LOCALE_PT_BR, "%.2f", oldDesejo.precoMinimo))
        }
        if (oldDesejo.precoMaximo > 0) {
            etPrecoMaximo.setText(String.format(WindowInsetsUtils.LOCALE_PT_BR, "%.2f", oldDesejo.precoMaximo))
        }

        etLojas.setText(oldDesejo.lojas)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.alterar_desejo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean = when (item.itemId) {
        R.id.menu_salvar -> {
            if (validar() && !operacaoEmAndamento) {
                operacaoEmAndamento = true
                lifecycleScope.launch {
                    if (alterar()) {
                        setResult(DetalheDesejoActivity.RESULT_SAVE)
                        finish()
                    } else {
                        operacaoEmAndamento = false
                    }
                }
            }
            true
        }
        R.id.menu_excluir -> {
            if (!operacaoEmAndamento) {
                operacaoEmAndamento = true
                lifecycleScope.launch {
                    if (remover()) {
                        setResult(DetalheDesejoActivity.RESULT_REMOVE)
                        finish()
                    } else {
                        // Mesma simetria de alterar(): não fechar reportando uma remoção
                        // que não aconteceu.
                        operacaoEmAndamento = false
                    }
                }
            }
            true
        }
        android.R.id.home -> { finish(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun validar(): Boolean {
        if (etProduto.text.toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.error_product_name_required, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    companion object {
        /**
         * Monta o [Desejo] atualizado a partir dos valores brutos dos campos de texto.
         *
         * Função pura — sem dependência de Android — para que o parse de preço seja testável
         * sem Robolectric nem infraestrutura de Hilt. Preços aceitam vírgula decimal (pt-BR)
         * e campo vazio vira `0.0`; qualquer outro formato lança [NumberFormatException],
         * tratada pelo chamador.
         *
         * `id` e `participanteId` vêm de [base] — perder o `participanteId` desvincularia o
         * desejo do seu participante.
         */
        @VisibleForTesting
        internal fun montarDesejoAtualizado(
            base: Desejo,
            produto: String,
            categoria: String,
            precoMinimo: String,
            precoMaximo: String,
            lojas: String,
        ): Desejo = Desejo().apply {
            id = base.id
            participanteId = base.participanteId
            this.produto = produto.trim()
            this.categoria = categoria.trim()
            this.lojas = lojas.trim()
            this.precoMinimo = parsePreco(precoMinimo)
            this.precoMaximo = parsePreco(precoMaximo)
        }

        private fun parsePreco(bruto: String): Double {
            val normalizado = bruto.trim().replace(",", ".")
            return if (normalizado.isEmpty()) 0.0 else normalizado.toDouble()
        }
    }

    /** @return `true` se a remoção foi persistida; `false` mantém a tela aberta. */
    private suspend fun remover(): Boolean {
        try {
            desejoRepository.remover(oldDesejo)
            Toast.makeText(this, R.string.toast_wish_deleted, Toast.LENGTH_SHORT).show()
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "remover: failed for desejo id=${oldDesejo.id}")
            val msg = e.message ?: getString(R.string.error_unknown)
            Toast.makeText(this, getString(R.string.error_generic_format, msg), Toast.LENGTH_LONG).show()
        }
        return false
    }

    /** @return `true` se a alteração foi persistida; `false` mantém a tela aberta. */
    private suspend fun alterar(): Boolean {
        try {
            val newDesejo = montarDesejoAtualizado(
                base = oldDesejo,
                produto = etProduto.text.toString(),
                categoria = etCategoria.text.toString(),
                precoMinimo = etPrecoMinimo.text.toString(),
                precoMaximo = etPrecoMaximo.text.toString(),
                lojas = etLojas.text.toString(),
            )
            desejoRepository.alterar(oldDesejo, newDesejo)
            Toast.makeText(this, R.string.toast_wish_updated, Toast.LENGTH_SHORT).show()
            return true
        } catch (e: NumberFormatException) {
            Timber.e(e, "alterar: preço malformado para desejo id=${oldDesejo.id}")
            Toast.makeText(this, R.string.error_invalid_price, Toast.LENGTH_SHORT).show()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: getString(R.string.error_unknown)
            Toast.makeText(this, getString(R.string.error_update_wish_format, msg), Toast.LENGTH_LONG).show()
        }
        return false
    }
}
