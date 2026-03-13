package activity.amigosecreto;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import activity.amigosecreto.util.WindowInsetsUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.DesejoDAO;

/**
 * Created by HP on 21/06/2015.
 */
public class AlterarDesejoActivity extends AppCompatActivity {

    private Desejo old_desejo;
    private Desejo new_desejo;

    private TextInputEditText et_produto;
    private TextInputEditText et_categoria;
    private TextInputEditText et_preco_minimo;
    private TextInputEditText et_preco_maximo;
    private TextInputEditText et_lojas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alterar_desejo);

        // Configurar MaterialToolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        WindowInsetsUtils.applyImeBottomPadding(findViewById(R.id.scroll_alterar_desejo));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            old_desejo = (Desejo) extras.get("desejo");
        }
        if (old_desejo == null) {
            Toast.makeText(this, "Erro ao carregar desejo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        et_produto = findViewById(R.id.et_produto);
        et_categoria = findViewById(R.id.et_categoria);
        et_preco_minimo = findViewById(R.id.et_preco_minimo);
        et_preco_maximo = findViewById(R.id.et_preco_maximo);
        et_lojas = findViewById(R.id.et_lojas);

        MaterialButton btnAtualizar = findViewById(R.id.btn_atualizar);
        btnAtualizar.setOnClickListener(v -> {
            if (validar()) {
                alterar();
                setResult(RESULT_OK);
                finish();
            }
        });

        if (old_desejo != null) {
            et_produto.setText(old_desejo.getProduto());
            et_categoria.setText(old_desejo.getCategoria());

            // Formatar preços para exibição nos campos de edição.
            // Intencional: usa "1.500,00" (sem prefixo R$) pois o layout já exibe
            // o prefixo "R$ " via app:prefixText. A tela de detalhes usa currencyFormat
            // que inclui o símbolo — essa diferença é esperada e facilita a digitação.
            if (old_desejo.getPrecoMinimo() > 0) {
                et_preco_minimo.setText(String.format(WindowInsetsUtils.LOCALE_PT_BR, "%.2f", old_desejo.getPrecoMinimo()));
            }
            if (old_desejo.getPrecoMaximo() > 0) {
                et_preco_maximo.setText(String.format(WindowInsetsUtils.LOCALE_PT_BR, "%.2f", old_desejo.getPrecoMaximo()));
            }

            et_lojas.setText(old_desejo.getLojas());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.alterar_desejo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_salvar) {
            if (validar()) {
                alterar();
                setResult(DetalheDesejoActivity.RESULT_SAVE);
                finish();
            }
            return true;
        } else if (id == R.id.menu_excluir) {
            remover();
            setResult(DetalheDesejoActivity.RESULT_REMOVE);
            finish();
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean validar() {
        if (et_produto.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Informe o nome do produto", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void remover() {
        try {
            DesejoDAO dao = new DesejoDAO(this);
            dao.open();
            dao.remover(old_desejo);
            dao.close();
            Toast.makeText(this, "Desejo excluído", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void alterar() {
        try {
            DesejoDAO dao = new DesejoDAO(this);
            dao.open();
            new_desejo = new Desejo();
            new_desejo.setId(old_desejo.getId());
            new_desejo.setProduto(et_produto.getText().toString().trim());
            new_desejo.setCategoria(et_categoria.getText().toString().trim());

            // Tratar preços - substituir vírgula por ponto para parseDouble
            String pMin = et_preco_minimo.getText().toString().trim().replace(",", ".");
            new_desejo.setPrecoMinimo(pMin.isEmpty() ? 0 : Double.parseDouble(pMin));

            String pMax = et_preco_maximo.getText().toString().trim().replace(",", ".");
            new_desejo.setPrecoMaximo(pMax.isEmpty() ? 0 : Double.parseDouble(pMax));

            new_desejo.setLojas(et_lojas.getText().toString().trim());

            // Importante: preservar o participanteId do desejo original
            new_desejo.setParticipanteId(old_desejo.getParticipanteId());

            dao.alterar(old_desejo, new_desejo);
            dao.close();
            Toast.makeText(this, "Desejo atualizado", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Erro: preço inválido", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao atualizar desejo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

}
