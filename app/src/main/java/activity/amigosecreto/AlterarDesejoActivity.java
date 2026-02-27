package activity.amigosecreto;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.DesejoDAO;

/**
 * Created by HP on 21/06/2015.
 */
public class AlterarDesejoActivity extends AppCompatActivity {

    private Desejo old_desejo;
    private Desejo new_desejo;

    private EditText et_produto;
    private EditText et_categoria;
    private EditText et_preco_minimo;
    private EditText et_preco_maximo;
    private EditText et_lojas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alterar_desejo);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        old_desejo = (Desejo) getIntent().getExtras().get("desejo");
        et_produto = (EditText) findViewById(R.id.et_produto);
        et_categoria = (EditText) findViewById(R.id.et_categoria);
        et_preco_minimo = (EditText) findViewById(R.id.et_preco_minimo);
        et_preco_maximo = (EditText) findViewById(R.id.et_preco_maximo);
        et_lojas = (EditText) findViewById(R.id.et_lojas);
        
        if (old_desejo != null) {
            et_produto.setText(old_desejo.getProduto());
            et_categoria.setText(old_desejo.getCategoria());
            et_preco_minimo.setText(String.valueOf(old_desejo.getPrecoMinimo()));
            et_preco_maximo.setText(String.valueOf(old_desejo.getPrecoMaximo()));
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
            
            String pMin = et_preco_minimo.getText().toString().trim();
            new_desejo.setPrecoMinimo(pMin.isEmpty() ? 0 : Double.parseDouble(pMin));
            
            String pMax = et_preco_maximo.getText().toString().trim();
            new_desejo.setPrecoMaximo(pMax.isEmpty() ? 0 : Double.parseDouble(pMax));
            
            new_desejo.setLojas(et_lojas.getText().toString().trim());
            dao.alterar(old_desejo, new_desejo);
            dao.close();
            Toast.makeText(this, "Desejo atualizado", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
