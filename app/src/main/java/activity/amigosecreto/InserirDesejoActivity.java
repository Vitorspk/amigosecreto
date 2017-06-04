package activity.amigosecreto;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.DesejoDAO;

/**
 * Created by HP on 21/06/2015.
 */
public class InserirDesejoActivity extends Activity {
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
        et_produto = (EditText) findViewById(R.id.et_produto_ins);
        et_categoria = (EditText) findViewById(R.id.et_categoria_ins);
        et_preco_minimo = (EditText) findViewById(R.id.et_preco_minimo_ins);
        et_preco_maximo = (EditText) findViewById(R.id.et_preco_maximo_ins);
        et_lojas = (EditText) findViewById(R.id.et_lojas_ins);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.inserir_desejo, menu);

        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_salvar:
                inserir();
                finish();
                return true;
            case android.R.id.home:
                //Metodo finish() vai encerrar essa activity
                NavUtils.navigateUpFromSameTask(this);
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
            desejo.setProduto(et_produto.getEditableText().toString().trim());
            desejo.setCategoria(et_categoria.getEditableText().toString().trim());
            desejo.setPrecoMinimo(Double.parseDouble(et_preco_minimo.getEditableText().toString().trim()));
            desejo.setPrecoMaximo(Double.parseDouble(et_preco_maximo.getEditableText().toString().trim()));
            desejo.setLojas(et_lojas.getEditableText().toString().trim());
            dao.inserir(desejo);
            dao.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

