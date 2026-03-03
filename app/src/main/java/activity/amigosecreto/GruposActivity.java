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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.GrupoDAO;
import activity.amigosecreto.db.ParticipanteDAO;

public class GruposActivity extends AppCompatActivity {

    private ListView lvGrupos;
    private MaterialButton btnCriarGrupo;
    private GrupoDAO dao;
    private ParticipanteDAO participanteDao;
    private List<Grupo> listaGrupos = new ArrayList<>();
    private GruposAdapter adapter;

    // Arrays de emojis e gradientes para variar os cards
    private String[] emojis = {"🎅", "🏝️", "🎄", "🎉", "🎊", "🎁", "🎈", "🌟", "💝", "🎂"};
    private int[] gradientes = {
        R.drawable.card_gradient_orange,
        R.drawable.card_gradient_blue,
        R.drawable.card_gradient_green
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar_grupos);

        // Configurar toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar_grupos);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        dao = new GrupoDAO(this);
        participanteDao = new ParticipanteDAO(this);
        lvGrupos = findViewById(R.id.lv_grupos);
        btnCriarGrupo = findViewById(R.id.btn_criar_grupo);

        adapter = new GruposAdapter(this, listaGrupos);
        lvGrupos.setAdapter(adapter);

        // Listener do botão criar grupo
        btnCriarGrupo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exibirDialogAdd();
            }
        });

        atualizarLista();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Atualizar a lista sempre que voltar para esta tela
        atualizarLista();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_grupos, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_more) {
            exibirMenuMais();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exibirMenuMais() {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, findViewById(R.id.action_more));
        popup.getMenuInflater().inflate(R.menu.menu_mais_opcoes, popup.getMenu());

        popup.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.action_sobre) {
                    exibirSobre();
                    return true;
                } else if (id == R.id.action_compartilhar) {
                    compartilharApp();
                    return true;
                } else if (id == R.id.action_avaliar) {
                    abrirPlayStore();
                    return true;
                } else if (id == R.id.action_limpar_dados) {
                    confirmarLimparTodosDados();
                    return true;
                }

                return false;
            }
        });

        popup.show();
    }

    private void exibirSobre() {
        new AlertDialog.Builder(this)
                .setTitle("Sobre o App")
                .setMessage(
                    "🎅 Amigo Secreto\n\n" +
                    "Organize seus amigos secretos de forma fácil e divertida!\n\n" +
                    "Recursos:\n" +
                    "• Múltiplos grupos\n" +
                    "• Restrições personalizadas\n" +
                    "• Lista de desejos\n" +
                    "• Compartilhamento via SMS/WhatsApp\n" +
                    "• Interface moderna\n\n" +
                    "Desenvolvido com ❤️ para tornar suas confraternizações mais especiais!"
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void compartilharApp() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Amigo Secreto - App");
        intent.putExtra(Intent.EXTRA_TEXT,
            "🎁 Organize seu Amigo Secreto de forma fácil!\n\n" +
            "Baixe o app Amigo Secreto e torne suas confraternizações mais divertidas!\n\n" +
            "https://play.google.com/store/apps/details?id=" + getPackageName()
        );
        startActivity(Intent.createChooser(intent, "Compartilhar app"));
    }

    private void abrirPlayStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("market://details?id=" + getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void confirmarLimparTodosDados() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Limpar Todos os Dados")
                .setMessage("Isso irá remover TODOS os grupos, participantes e sorteios.\n\nEsta ação NÃO pode ser desfeita!\n\nDeseja continuar?")
                .setPositiveButton("Sim, limpar tudo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dao.open();
                        dao.limparTudo();
                        dao.close();
                        atualizarLista();
                        Toast.makeText(GruposActivity.this, "Todos os dados foram removidos", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void atualizarLista() {
        dao.open();
        listaGrupos.clear();
        listaGrupos.addAll(dao.listar());
        dao.close();
        adapter.notifyDataSetChanged();
    }

    private void exibirDialogAdd() {
        // Inflar o layout customizado
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_criar_grupo, null);

        final com.google.android.material.textfield.TextInputEditText etNome = dialogView.findViewById(R.id.et_nome_grupo);
        com.google.android.material.button.MaterialButton btnCriar = dialogView.findViewById(R.id.btn_criar);
        com.google.android.material.button.MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_cancelar);

        // Chips de sugestões
        com.google.android.material.chip.Chip chipFamilia = dialogView.findViewById(R.id.chip_familia);
        com.google.android.material.chip.Chip chipTrabalho = dialogView.findViewById(R.id.chip_trabalho);
        com.google.android.material.chip.Chip chipAmigos = dialogView.findViewById(R.id.chip_amigos);

        // Criar dialog
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Tornar o fundo transparente para mostrar os cantos arredondados
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Listeners dos chips
        chipFamilia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etNome.setText("Família");
            }
        });

        chipTrabalho.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etNome.setText("Trabalho");
            }
        });

        chipAmigos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etNome.setText("Amigos");
            }
        });

        // Botão criar
        btnCriar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nome = etNome.getText().toString().trim();
                if (!nome.isEmpty()) {
                    Grupo g = new Grupo();
                    g.setNome(nome);
                    g.setData(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
                    dao.open();
                    dao.inserir(g);
                    dao.close();
                    atualizarLista();
                    dialog.dismiss();
                } else {
                    Toast.makeText(GruposActivity.this, "O nome do grupo é obrigatório", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Botão cancelar
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
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
                convertView = LayoutInflater.from(ctx).inflate(R.layout.item_grupo_modern, parent, false);
            }

            final Grupo g = itens.get(position);
            TextView tvNome = convertView.findViewById(R.id.tv_grupo_nome);
            TextView tvParticipantes = convertView.findViewById(R.id.tv_grupo_participantes);
            TextView tvEmoji = convertView.findViewById(R.id.tv_grupo_emoji);
            LinearLayout layoutContent = convertView.findViewById(R.id.layout_grupo_content);

            // Set nome
            tvNome.setText(g.getNome());

            // Set participantes count - buscar do banco
            participanteDao.open();
            int numParticipantes = participanteDao.listarPorGrupo(g.getId()).size();
            participanteDao.close();
            tvParticipantes.setText(numParticipantes + (numParticipantes == 1 ? " Participante" : " Participantes"));

            // Set emoji baseado na posição
            String emoji = emojis[position % emojis.length];
            tvEmoji.setText(emoji);

            // Set gradiente baseado na posição
            int gradiente = gradientes[position % gradientes.length];
            layoutContent.setBackgroundResource(gradiente);

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
