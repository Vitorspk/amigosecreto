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
import androidx.activity.EdgeToEdge;

import com.google.android.material.button.MaterialButton;

import android.util.Log;
import android.view.MenuItem;
import android.widget.PopupMenu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.GrupoDAO;
import activity.amigosecreto.db.ParticipanteDAO;
import activity.amigosecreto.util.AsyncDatabaseHelper;
import activity.amigosecreto.util.HapticFeedbackUtils;

public class GruposActivity extends AppCompatActivity {

    private static final String TAG = "GruposActivity";

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
        EdgeToEdge.enable(this);
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
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.action_more));
        popup.getMenuInflater().inflate(R.menu.menu_mais_opcoes, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
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
                .setTitle(R.string.dialog_about_title)
                .setMessage(R.string.dialog_about_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void compartilharApp() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app_subject));
        intent.putExtra(Intent.EXTRA_TEXT,
            getString(R.string.share_app_body) + "\n\n" +
            "https://play.google.com/store/apps/details?id=" + getPackageName()
        );
        startActivity(Intent.createChooser(intent, getString(R.string.action_share_app)));
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
                .setTitle(R.string.dialog_clear_all_title)
                .setMessage(R.string.dialog_clear_all_message)
                .setPositiveButton(R.string.button_clear_all_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dao.open();
                        dao.limparTudo();
                        dao.close();
                        atualizarLista();
                        Toast.makeText(GruposActivity.this, R.string.toast_all_data_cleared, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    private void atualizarLista() {
        dao.open();
        listaGrupos.clear();
        listaGrupos.addAll(dao.listar());
        dao.close();
        // Exibe lista imediatamente; contagens chegam via callback e disparam novo notify
        adapter.notifyDataSetChanged();
        adapter.recarregarContagensAsync();
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
                etNome.setText(getString(R.string.chip_sugestao_familia));
            }
        });

        chipTrabalho.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etNome.setText(getString(R.string.chip_sugestao_trabalho));
            }
        });

        chipAmigos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etNome.setText(getString(R.string.chip_sugestao_amigos));
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
                    Toast.makeText(GruposActivity.this, R.string.grupo_erro_nome_obrigatorio, Toast.LENGTH_SHORT).show();
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
        private Map<Integer, Integer> contagemParticipantes = new HashMap<>();

        public GruposAdapter(Context ctx, List<Grupo> itens) {
            this.ctx = ctx;
            this.itens = itens;
        }

        void recarregarContagensAsync() {
            AsyncDatabaseHelper.execute(
                () -> {
                    participanteDao.open();
                    Map<Integer, Integer> mapa = participanteDao.contarPorGrupo();
                    participanteDao.close();
                    return mapa;
                },
                new AsyncDatabaseHelper.ResultCallback<Map<Integer, Integer>>() {
                    @Override
                    public void onSuccess(Map<Integer, Integer> mapa) {
                        contagemParticipantes.clear();
                        contagemParticipantes.putAll(mapa);
                        notifyDataSetChanged();
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Erro ao carregar contagem de participantes", e);
                    }
                }
            );
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

            // Set participantes count - usar contagem pré-carregada
            int numParticipantes = contagemParticipantes.containsKey(g.getId())
                ? contagemParticipantes.get(g.getId()) : 0;
            tvParticipantes.setText(ctx.getResources().getQuantityString(R.plurals.label_participants, numParticipantes, numParticipantes));

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

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    HapticFeedbackUtils.performMediumFeedback(v);
                    exibirMenuContextoGrupo(v, g);
                    return true;
                }
            });

            return convertView;
        }

        private static final int MENU_EDITAR = 1;
        private static final int MENU_EXCLUIR = 2;

        private void exibirMenuContextoGrupo(View anchorView, final Grupo g) {
            PopupMenu popup = new PopupMenu(ctx, anchorView);
            popup.getMenu().add(0, MENU_EDITAR, 0, ctx.getString(R.string.grupo_menu_editar_nome));
            popup.getMenu().add(0, MENU_EXCLUIR, 1, ctx.getString(R.string.grupo_menu_excluir));

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int id = item.getItemId();
                    if (id == MENU_EDITAR) {
                        exibirDialogEditarNome(g);
                        return true;
                    } else if (id == MENU_EXCLUIR) {
                        confirmarRemoverGrupo(g);
                        return true;
                    }
                    return false;
                }
            });

            popup.show();
        }

        private void exibirDialogEditarNome(final Grupo g) {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_criar_grupo, null);

            final com.google.android.material.textfield.TextInputEditText etNome =
                    dialogView.findViewById(R.id.et_nome_grupo);
            com.google.android.material.button.MaterialButton btnCriar =
                    dialogView.findViewById(R.id.btn_criar);
            com.google.android.material.button.MaterialButton btnCancelar =
                    dialogView.findViewById(R.id.btn_cancelar);

            // Ocultar chips de sugestões no modo edição
            View chipGroup = dialogView.findViewById(R.id.chip_group_sugestoes);
            if (chipGroup != null) chipGroup.setVisibility(View.GONE);

            etNome.setText(g.getNome());
            etNome.setSelection(etNome.getText() != null ? etNome.getText().length() : 0);
            btnCriar.setText(getString(R.string.grupo_btn_salvar));

            final AlertDialog dialog = new AlertDialog.Builder(ctx)
                    .setView(dialogView)
                    .create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            btnCriar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String novoNome = etNome.getText().toString().trim();
                    if (novoNome.isEmpty()) {
                        Toast.makeText(GruposActivity.this, R.string.grupo_erro_nome_obrigatorio, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final String nomeOriginal = g.getNome();
                    g.setNome(novoNome);
                    btnCriar.setEnabled(false);
                    AsyncDatabaseHelper.execute(
                        () -> {
                            dao.open();
                            int rows = dao.atualizarNome(g);
                            dao.close();
                            return rows;
                        },
                        new AsyncDatabaseHelper.ResultCallback<Integer>() {
                            @Override
                            public void onSuccess(Integer rows) {
                                if (rows > 0) {
                                    atualizarLista();
                                    dialog.dismiss();
                                } else {
                                    g.setNome(nomeOriginal);
                                    btnCriar.setEnabled(true);
                                    Toast.makeText(GruposActivity.this, R.string.grupo_erro_salvar, Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onError(Exception e) {
                                g.setNome(nomeOriginal);
                                notifyDataSetChanged();
                                btnCriar.setEnabled(true);
                                Log.e(TAG, "Erro ao atualizar nome do grupo", e);
                                Toast.makeText(GruposActivity.this, R.string.grupo_erro_salvar, Toast.LENGTH_SHORT).show();
                            }
                        }
                    );
                }
            });

            btnCancelar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        }

        private void confirmarRemoverGrupo(final Grupo g) {
            new AlertDialog.Builder(ctx)
                    .setTitle(R.string.grupo_dialog_excluir_titulo)
                    .setMessage(ctx.getString(R.string.grupo_dialog_excluir_mensagem, g.getNome()))
                    .setPositiveButton(R.string.grupo_btn_excluir, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AsyncDatabaseHelper.executeSimple(
                                () -> {
                                    dao.open();
                                    dao.remover(g.getId());
                                    dao.close();
                                },
                                () -> atualizarLista()
                            );
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show();
        }
    }
}
