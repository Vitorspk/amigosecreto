package activity.amigosecreto;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.NumberFormat;

import activity.amigosecreto.db.Desejo;

/**
 * Created by HP on 21/06/2015.
 */
public class DetalheDesejoActivity extends Activity {

    private TextView tv_produto;
    private TextView tv_categoria;
    private TextView tv_preco_minimo;
    private TextView tv_preco_maximo;
    private TextView tv_lojas;
    private ImageButton ib_buscape;

    private Desejo desejo;

    public static final int RESULT_REMOVE = 1000;
    public static final int RESULT_SAVE = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_desejo);
        this.desejo = (Desejo) getIntent().getExtras().get("desejo");
        tv_produto = (TextView) findViewById(R.id.tv_produto);
        tv_categoria = (TextView) findViewById(R.id.tv_categoria);
        tv_preco_minimo = (TextView) findViewById(R.id.tv_preco_minimo);
        tv_preco_maximo = (TextView) findViewById(R.id.tv_preco_maximo);
        tv_lojas = (TextView) findViewById(R.id.tv_lojas);
        ib_buscape = (ImageButton) findViewById(R.id.ib_buscape);
        ib_buscape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://compare.buscape.com.br/"+desejo.getProduto());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        carregarCampos(desejo);
    }

    private void carregarCampos(Desejo desejo) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        tv_produto.setText(desejo.getProduto());
        tv_categoria.setText(desejo.getCategoria());
        tv_preco_minimo.setText(nf.format(desejo.getPrecoMinimo()));
        tv_preco_maximo.setText(nf.format(desejo.getPrecoMaximo()));
        tv_lojas.setText(desejo.getLojas());
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
            startActivityForResult(intent,1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1){
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}

