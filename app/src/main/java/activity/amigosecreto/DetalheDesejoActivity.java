package activity.amigosecreto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import java.text.NumberFormat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import activity.amigosecreto.db.Desejo;

public class DetalheDesejoActivity extends AppCompatActivity {

    private TextView tv_produto;
    private TextView tv_categoria;
    private TextView tv_preco_minimo;
    private TextView tv_preco_maximo;
    private TextView tv_lojas;
    private MaterialButton btn_buscape;

    private Desejo desejo;

    public static final int RESULT_REMOVE = 1000;
    public static final int RESULT_SAVE = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_desejo);

        // Configurar MaterialToolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (getIntent().getExtras() != null) {
            this.desejo = (Desejo) getIntent().getExtras().get("desejo");
        }

        tv_produto = findViewById(R.id.tv_produto);
        tv_categoria = findViewById(R.id.tv_categoria);
        tv_preco_minimo = findViewById(R.id.tv_preco_minimo);
        tv_preco_maximo = findViewById(R.id.tv_preco_maximo);
        tv_lojas = findViewById(R.id.tv_lojas);
        btn_buscape = findViewById(R.id.btn_pesquisar_buscape);

        if (btn_buscape != null && desejo != null) {
            btn_buscape.setOnClickListener(v -> {
                Uri uri = new Uri.Builder()
                    .scheme("https")
                    .authority("www.buscape.com.br")
                    .appendPath("search")
                    .appendQueryParameter("q", desejo.getProduto())
                    .build();
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
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
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == RESULT_OK){
            // Recarregar o desejo atualizado do banco de dados
            recarregarDesejo();
        }
    }

    private void recarregarDesejo() {
        if (desejo != null) {
            try {
                activity.amigosecreto.db.DesejoDAO dao = new activity.amigosecreto.db.DesejoDAO(this);
                dao.open();
                // Buscar o desejo atualizado pelo ID
                Desejo desejoAtualizado = dao.buscarPorId(desejo.getId());
                dao.close();

                if (desejoAtualizado != null) {
                    desejo = desejoAtualizado;
                    carregarCampos(desejo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
