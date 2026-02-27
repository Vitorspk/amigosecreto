package activity.amigosecreto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.NumberFormat;
import activity.amigosecreto.db.Desejo;

public class DetalheDesejoActivity extends AppCompatActivity {

    private TextView tv_produto;
    private TextView tv_categoria;
    private TextView tv_preco_minimo;
    private TextView tv_preco_maximo;
    private TextView tv_lojas;
    private Button btn_buscape;

    private Desejo desejo;

    public static final int RESULT_REMOVE = 1000;
    public static final int RESULT_SAVE = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_desejo);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        if (getIntent().getExtras() != null) {
            this.desejo = (Desejo) getIntent().getExtras().get("desejo");
        }
        
        tv_produto = (TextView) findViewById(R.id.tv_produto);
        tv_categoria = (TextView) findViewById(R.id.tv_categoria);
        tv_preco_minimo = (TextView) findViewById(R.id.tv_preco_minimo);
        tv_preco_maximo = (TextView) findViewById(R.id.tv_preco_maximo);
        tv_lojas = (TextView) findViewById(R.id.tv_lojas);
        btn_buscape = (Button) findViewById(R.id.btn_pesquisar_buscape);
        
        if (btn_buscape != null && desejo != null) {
            btn_buscape.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri uri = Uri.parse("http://compare.buscape.com.br/" + desejo.getProduto());
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }
            });
        }
        
        if (desejo != null) {
            carregarCampos(desejo);
        }
    }

    private void carregarCampos(Desejo desejo) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        if (tv_produto != null) tv_produto.setText(desejo.getProduto());
        if (tv_categoria != null) tv_categoria.setText(desejo.getCategoria());
        if (tv_preco_minimo != null) tv_preco_minimo.setText(nf.format(desejo.getPrecoMinimo()));
        if (tv_preco_maximo != null) tv_preco_maximo.setText(nf.format(desejo.getPrecoMaximo()));
        if (tv_lojas != null) tv_lojas.setText(desejo.getLojas());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detalhe_desejo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_editar) {
            Intent intent = new Intent(this, AlterarDesejoActivity.class);
            intent.putExtra("desejo", desejo);
            startActivityForResult(intent, 1);
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1){
            // Se voltou da edição, o ideal seria recarregar ou fechar para a lista atualizar
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
