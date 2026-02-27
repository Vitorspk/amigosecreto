package activity.amigosecreto;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.DesejoDAO;

public class InserirDesejoActivity extends AppCompatActivity {
    private Desejo desejo;
    private EditText et_produto;
    private EditText et_categoria;
    private EditText et_preco_minimo;
    private EditText et_preco_maximo;
    private EditText et_lojas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inserir_desejo);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        et_produto = (EditText) findViewById(R.id.et_produto_ins);
        et_categoria = (EditText) findViewById(R.id.et_categoria_ins);
        et_preco_minimo = (EditText) findViewById(R.id.et_preco_minimo_ins);
        et_preco_maximo = (EditText) findViewById(R.id.et_preco_maximo_ins);
        et_lojas = (EditText) findViewById(R.id.et_lojas_ins);
        
        View btnSalvar = findViewById(R.id.btn_salvar_ins);
        if (btnSalvar != null) {
            btnSalvar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (validar()) {
                        inserir();
                        finish();
                    }
                }
            });
        }
    }
    
    private boolean validar() {
        if (et_produto.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Informe o nome do produto", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.inserir_desejo, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_salvar) {
            if (validar()) {
                inserir();
                finish();
            }
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void inserir() {
        try {
            DesejoDAO dao = new DesejoDAO(this);
            dao.open();
            desejo = new Desejo();
            desejo.setId(dao.proximoId());
            desejo.setProduto(et_produto.getText().toString().trim());
            desejo.setCategoria(et_categoria.getText().toString().trim());
            
            String pMin = et_preco_minimo.getText().toString().trim();
            desejo.setPrecoMinimo(pMin.isEmpty() ? 0 : Double.parseDouble(pMin));
            
            String pMax = et_preco_maximo.getText().toString().trim();
            desejo.setPrecoMaximo(pMax.isEmpty() ? 0 : Double.parseDouble(pMax));
            
            desejo.setLojas(et_lojas.getText().toString().trim());
            dao.inserir(desejo);
            dao.close();
            Toast.makeText(this, "Desejo salvo com sucesso!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao salvar desejo", Toast.LENGTH_SHORT).show();
        }
    }
}
