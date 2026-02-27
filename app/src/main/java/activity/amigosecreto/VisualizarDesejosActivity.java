package activity.amigosecreto;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.DesejoDAO;
import activity.amigosecreto.db.Participante;

public class VisualizarDesejosActivity extends AppCompatActivity {

    private Participante participante;
    private DesejoDAO desejoDAO;
    private List<Desejo> listaDesejos;

    private ListView lvDesejos;
    private TextView tvNomeParticipante;
    private View layoutEmpty;
    private DesejosAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizar_desejos);

        // Receber participante via Intent
        participante = (Participante) getIntent().getSerializableExtra("participante");
        if (participante == null) {
            Toast.makeText(this, "Erro: participante não encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar DAO
        desejoDAO = new DesejoDAO(this);
        desejoDAO.open();

        // Inicializar views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Lista de Desejos de " + participante.getNome());
        toolbar.setNavigationOnClickListener(v -> finish());

        lvDesejos = findViewById(R.id.lv_desejos);
        tvNomeParticipante = findViewById(R.id.tv_nome_participante);
        layoutEmpty = findViewById(R.id.layout_empty);

        tvNomeParticipante.setText(participante.getNome());

        // Configurar adapter
        listaDesejos = new ArrayList<>();
        adapter = new DesejosAdapter(this, listaDesejos);
        lvDesejos.setAdapter(adapter);

        // Carregar desejos
        carregarDesejos();
    }

    private void carregarDesejos() {
        listaDesejos.clear();
        List<Desejo> desejos = desejoDAO.listarPorParticipante(participante.getId());
        listaDesejos.addAll(desejos);
        adapter.notifyDataSetChanged();

        // Mostrar/ocultar empty state
        if (listaDesejos.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            lvDesejos.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            lvDesejos.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (desejoDAO != null) {
            desejoDAO.close();
        }
    }

    // Adapter para lista de desejos (somente visualização)
    private class DesejosAdapter extends BaseAdapter {
        private Context context;
        private List<Desejo> desejos;

        public DesejosAdapter(Context context, List<Desejo> desejos) {
            this.context = context;
            this.desejos = desejos;
        }

        @Override
        public int getCount() {
            return desejos.size();
        }

        @Override
        public Object getItem(int position) {
            return desejos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_desejo, parent, false);
            }

            Desejo desejo = desejos.get(position);

            TextView tvProduto = convertView.findViewById(R.id.tv_item_produto);
            TextView tvCategoria = convertView.findViewById(R.id.tv_item_categoria);
            TextView tvPreco = convertView.findViewById(R.id.tv_item_preco);

            tvProduto.setText(desejo.getProduto());

            if (desejo.getCategoria() != null && !desejo.getCategoria().isEmpty()) {
                tvCategoria.setText(desejo.getCategoria());
                tvCategoria.setVisibility(View.VISIBLE);
            } else {
                tvCategoria.setVisibility(View.GONE);
            }

            // Formatar preço
            if (desejo.getPrecoMinimo() > 0 || desejo.getPrecoMaximo() > 0) {
                String precoTexto = "R$ " + String.format("%.2f", desejo.getPrecoMinimo())
                        + " - R$ " + String.format("%.2f", desejo.getPrecoMaximo());
                tvPreco.setText(precoTexto);
                tvPreco.setVisibility(View.VISIBLE);
            } else {
                tvPreco.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}
