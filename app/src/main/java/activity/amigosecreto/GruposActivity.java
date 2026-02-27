package activity.amigosecreto;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.GrupoDAO;

public class GruposActivity extends AppCompatActivity {

    private ListView lvGrupos;
    private TextView tvCount;
    private ExtendedFloatingActionButton fabAdd;
    private GrupoDAO dao;
    private List<Grupo> listaGrupos = new ArrayList<>();
    private GruposAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar_grupos);

        dao = new GrupoDAO(this);
        lvGrupos = findViewById(R.id.lv_grupos);
        tvCount = findViewById(R.id.tv_grupos_count);
        fabAdd = findViewById(R.id.fab_add_grupo);

        adapter = new GruposAdapter(this, listaGrupos);
        lvGrupos.setAdapter(adapter);

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exibirDialogAdd();
            }
        });

        atualizarLista();
    }

    private void atualizarLista() {
        dao.open();
        listaGrupos.clear();
        listaGrupos.addAll(dao.listar());
        dao.close();
        adapter.notifyDataSetChanged();
        
        if (listaGrupos.isEmpty()) {
            tvCount.setText("Crie seu primeiro grupo de sorteio");
        } else {
            tvCount.setText(listaGrupos.size() + " grupos ativos");
        }
    }

    private void exibirDialogAdd() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Novo Grupo");

        final EditText etNome = new EditText(this);
        etNome.setHint("Ex: Família, Trabalho, Futebol...");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        builder.setView(etNome);
        
        // Ajustando margens do EditText no Dialog
        AlertDialog dialog = builder.create();
        dialog.setView(etNome, padding, padding, padding, 0);

        builder.setPositiveButton("Criar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nome = etNome.getText().toString().trim();
                if (!nome.isEmpty()) {
                    Grupo g = new Grupo();
                    g.setNome(nome);
                    g.setData(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
                    dao.open();
                    dao.inserir(g);
                    dao.close();
                    atualizarLista();
                } else {
                    Toast.makeText(GruposActivity.this, "O nome do grupo é obrigatório", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private class GruposAdapter extends BaseAdapter {
        private Context ctx;
        private List<Grupo> itens;

        public GruposAdapter(Context ctx, List<Grupo> itens) {
            this.ctx = ctx;
            this.itens = itens;
        }

        @Override
        public int getCount() { return itens.size(); }
        @Override
        public Object getItem(int position) { return itens.get(position); }
        @Override
        public long getItemId(int position) { return itens.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ctx).inflate(R.layout.item_grupo, parent, false);
            }

            final Grupo g = itens.get(position);
            TextView tvNome = convertView.findViewById(R.id.tv_grupo_nome);
            TextView tvData = convertView.findViewById(R.id.tv_grupo_data);
            ImageButton btnRemover = convertView.findViewById(R.id.btn_remover_grupo);

            tvNome.setText(g.getNome());
            tvData.setText("Criado em: " + g.getData());

            btnRemover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmarRemoverGrupo(g);
                }
            });

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(GruposActivity.this, ParticipantesActivity.class);
                    intent.putExtra("grupo", g);
                    startActivity(intent);
                }
            });

            return convertView;
        }

        private void confirmarRemoverGrupo(final Grupo g) {
            new AlertDialog.Builder(ctx)
                    .setTitle("Excluir Grupo")
                    .setMessage("Deseja excluir o grupo '" + g.getNome() + "' e todos os seus participantes?")
                    .setPositiveButton("Excluir", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dao.open();
                            dao.remover(g.getId());
                            dao.close();
                            atualizarLista();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }
}
