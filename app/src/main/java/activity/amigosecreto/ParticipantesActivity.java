package activity.amigosecreto;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.Participante;
import activity.amigosecreto.db.ParticipanteDAO;
import activity.amigosecreto.db.DesejoDAO;

public class ParticipantesActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int REQUEST_CONTACT_PICKER = 200;

    // ID do participante cujo SMS foi aberto; marcado como enviado no onResume ao retornar.
    private int pendingSmsParticipanteId = -1;
    // True apenas quando startActivity foi chamado nesta sessao (nao quando restaurado de rotacao).
    private boolean smsLaunched = false;
    // Estado da sequencia de SMS; retomado no onResume para evitar dialog durante pausa da activity.
    private List<Participante> pendingSmsList = null;
    private Map<Integer, String> pendingSmsNomesAmigos = null;
    private int pendingSmsNextIndex = -1;

    private ListView lvParticipantes;
    private TextView tvCount;
    private ExtendedFloatingActionButton fabAdd;
    private View btnSortear;
    private View btnLimpar;
    private ParticipanteDAO dao;
    private List<Participante> listaParticipantes = new ArrayList<>();
    private ParticipantesAdapter adapter;
    private Grupo grupoAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar_participantes);

        grupoAtual = (Grupo) getIntent().getSerializableExtra("grupo");
        if (grupoAtual == null) {
            finish();
            return;
        }

        if (savedInstanceState != null) {
            pendingSmsParticipanteId = savedInstanceState.getInt("pendingSmsId", -1);
            pendingSmsNextIndex = savedInstanceState.getInt("pendingSmsNextIndex", -1);
            int[] ids = savedInstanceState.getIntArray("pendingSmsIds");
            String[] telefones = savedInstanceState.getStringArray("pendingSmsTelefones");
            String[] nomes = savedInstanceState.getStringArray("pendingSmsNomes");
            String[] nomesAmigos = savedInstanceState.getStringArray("pendingSmsNomesAmigos");
            if (ids != null && telefones != null && nomes != null && nomesAmigos != null) {
                pendingSmsList = new ArrayList<>();
                pendingSmsNomesAmigos = new HashMap<>();
                for (int i = 0; i < ids.length; i++) {
                    Participante p = new Participante();
                    p.setId(ids[i]);
                    p.setTelefone(telefones[i]);
                    p.setNome(nomes[i]);
                    pendingSmsList.add(p);
                    // Restore null for empty-string sentinel (saved when nomeAmigo was null)
                    pendingSmsNomesAmigos.put(ids[i], nomesAmigos[i].isEmpty() ? null : nomesAmigos[i]);
                }
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(grupoAtual.getNome());
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dao = new ParticipanteDAO(this);
        lvParticipantes = findViewById(R.id.lv_participantes);
        tvCount = findViewById(R.id.tv_count);
        fabAdd = findViewById(R.id.fab_add_participante);
        btnSortear = findViewById(R.id.btn_sortear);
        btnLimpar = findViewById(R.id.btn_limpar);

        adapter = new ParticipantesAdapter(this, listaParticipantes);
        lvParticipantes.setAdapter(adapter);

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exibirDialogAdd();
            }
        });

        btnSortear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                realizarSorteio();
            }
        });

        btnLimpar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmarLimparTudo();
            }
        });

        atualizarLista();
    }

    private void atualizarLista() {
        dao.open();
        listaParticipantes.clear();
        listaParticipantes.addAll(dao.listarPorGrupo(grupoAtual.getId()));
        dao.close();

        // Pré-carregar counts de desejos para evitar criar DAO a cada item do adapter
        Map<Integer, Integer> desejosCountMap = new HashMap<>();
        DesejoDAO desejoDAO = new DesejoDAO(this);
        try {
            desejoDAO.open();
            for (Participante p : listaParticipantes) {
                int count = desejoDAO.contarDesejosPorParticipante(p.getId());
                desejosCountMap.put(p.getId(), count);
            }
        } finally {
            desejoDAO.close();
        }

        adapter.setDesejosCountMap(desejosCountMap);
        adapter.notifyDataSetChanged();

        if (listaParticipantes.isEmpty()) {
            tvCount.setText("Nenhum participante ainda");
        } else {
            tvCount.setText(listaParticipantes.size() + " participantes no grupo " + grupoAtual.getNome());
        }
    }

    private void exibirDialogAdd() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Novo Participante");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_participante, null);
        final EditText etNome = view.findViewById(R.id.et_nome);
        final EditText etTelefone = view.findViewById(R.id.et_telefone);
        final EditText etEmail = view.findViewById(R.id.et_email);
        View btnPickContact = view.findViewById(R.id.btn_pick_contact);

        btnPickContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(ParticipantesActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ParticipantesActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
                } else {
                    abrirSeletorContatos();
                }
            }
        });

        builder.setView(view);
        builder.setPositiveButton("Adicionar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nome = etNome.getText().toString().trim();
                String telefone = etTelefone.getText().toString().trim();
                String email = etEmail.getText().toString().trim();

                if (!nome.isEmpty()) {
                    Participante p = new Participante();
                    p.setNome(nome);
                    p.setTelefone(telefone);
                    p.setEmail(email);
                    dao.open();
                    dao.inserir(p, grupoAtual.getId());
                    dao.close();
                    atualizarLista();
                } else {
                    Toast.makeText(ParticipantesActivity.this, "Nome é obrigatório", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void abrirSeletorContatos() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CONTACT_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTACT_PICKER && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
            try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1);

                    Participante p = new Participante();
                    p.setNome(name);
                    p.setTelefone(number);
                    dao.open();
                    dao.inserir(p, grupoAtual.getId());
                    dao.close();
                    atualizarLista();
                    Toast.makeText(this, name + " adicionado!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Marca como enviado apenas se o SMS foi efetivamente aberto nesta sessao.
        // smsLaunched evita marcacao incorreta apos rotacao (pendingSmsParticipanteId e
        // restaurado do bundle mas o app de SMS nunca foi aberto).
        if (smsLaunched && pendingSmsParticipanteId != -1) {
            dao.open();
            dao.marcarComoEnviado(pendingSmsParticipanteId);
            dao.close();
            pendingSmsParticipanteId = -1;
            smsLaunched = false;
        }
        // Atualizar lista ao voltar para esta activity (ex: depois de adicionar desejos)
        atualizarLista();
        // Retoma sequencia de SMS apos retornar do app de mensagens (evita dialog durante pausa)
        if (pendingSmsList != null && pendingSmsNextIndex >= 0) {
            List<Participante> lista = pendingSmsList;
            Map<Integer, String> nomes = pendingSmsNomesAmigos;
            int nextIndex = pendingSmsNextIndex;
            pendingSmsList = null;
            pendingSmsNomesAmigos = null;
            pendingSmsNextIndex = -1;
            enviarSmsSequencial(lista, nomes, nextIndex);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pendingSmsId", pendingSmsParticipanteId);
        outState.putInt("pendingSmsNextIndex", pendingSmsNextIndex);
        if (pendingSmsList != null && pendingSmsNomesAmigos != null) {
            int[] ids = new int[pendingSmsList.size()];
            String[] telefones = new String[pendingSmsList.size()];
            String[] nomes = new String[pendingSmsList.size()];
            String[] nomesAmigos = new String[pendingSmsList.size()];
            for (int i = 0; i < pendingSmsList.size(); i++) {
                Participante p = pendingSmsList.get(i);
                ids[i] = p.getId();
                telefones[i] = p.getTelefone();
                nomes[i] = p.getNome();
                String nomeAmigo = pendingSmsNomesAmigos.get(p.getId());
                nomesAmigos[i] = nomeAmigo != null ? nomeAmigo : "";
            }
            outState.putIntArray("pendingSmsIds", ids);
            outState.putStringArray("pendingSmsTelefones", telefones);
            outState.putStringArray("pendingSmsNomes", nomes);
            outState.putStringArray("pendingSmsNomesAmigos", nomesAmigos);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirSeletorContatos();
            }
        }
    }

    private void confirmarLimparTudo() {
        new AlertDialog.Builder(this)
                .setTitle("Limpar Tudo")
                .setMessage("Deseja remover todos os participantes deste grupo?")
                .setPositiveButton("Sim, limpar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dao.open();
                        dao.deletarTodosDoGrupo(grupoAtual.getId());
                        dao.close();
                        atualizarLista();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void realizarSorteio() {
        if (listaParticipantes.size() < 3) {
            Toast.makeText(this, "Adicione pelo menos 3 participantes", Toast.LENGTH_LONG).show();
            return;
        }

        List<Participante> sorteados = null;
        int tentativas = 0;
        while (sorteados == null && tentativas < 100) {
            tentativas++;
            sorteados = tentarSorteioComRegras(new ArrayList<>(listaParticipantes));
        }

        if (sorteados != null) {
            dao.open();
            boolean sucesso = dao.salvarSorteio(listaParticipantes, sorteados);
            dao.close();
            
            if (sucesso) {
                atualizarLista();
                new AlertDialog.Builder(this)
                        .setTitle("Sorteio Concluído!")
                        .setMessage("Deseja enviar os resultados agora por SMS?")
                        .setPositiveButton("Sim, enviar SMS", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                enviarSmsViaIntent();
                            }
                        })
                        .setNegativeButton("Não", null)
                        .show();
            }
        } else {
            Toast.makeText(this, "Impossível realizar sorteio com as regras atuais.", Toast.LENGTH_LONG).show();
        }
    }

    private List<Participante> tentarSorteioComRegras(List<Participante> participantes) {
        List<Participante> disponiveis = new ArrayList<>(participantes);
        List<Participante> resultado = new ArrayList<>();
        Random random = new Random();

        for (Participante atual : participantes) {
            List<Participante> possiveis = new ArrayList<>();
            for (Participante p : disponiveis) {
                if (p.getId() != atual.getId() && !atual.getIdsExcluidos().contains(p.getId())) {
                    possiveis.add(p);
                }
            }
            if (possiveis.isEmpty()) return null;
            Participante sorteado = possiveis.get(random.nextInt(possiveis.size()));
            resultado.add(sorteado);
            disponiveis.remove(sorteado);
        }
        return resultado;
    }

    // Envia SMS abrindo o app de mensagens do dispositivo via Intent (sem permissao SEND_SMS).
    // O usuario confirma e envia um por um — compativel com Play Store sem restricoes.
    private void enviarSmsViaIntent() {
        List<Participante> comTelefone = new ArrayList<>();
        Map<Integer, String> nomesAmigos = new HashMap<>();
        try {
            dao.open();
            for (Participante p : listaParticipantes) {
                if (p.getTelefone() != null && !p.getTelefone().trim().isEmpty()) {
                    comTelefone.add(p);
                    nomesAmigos.put(p.getId(), dao.getNomeAmigoSorteado(p.getAmigoSorteadoId()));
                }
            }
        } finally {
            dao.close();
        }

        if (comTelefone.isEmpty()) {
            Toast.makeText(this, "Nenhum participante com telefone cadastrado.", Toast.LENGTH_LONG).show();
            return;
        }

        enviarSmsSequencial(comTelefone, nomesAmigos, 0);
    }

    // Exibe dialog para cada participante antes de abrir o app de SMS, evitando stack de activities.
    private void enviarSmsSequencial(final List<Participante> lista, final Map<Integer, String> nomesAmigos, final int index) {
        if (index >= lista.size()) {
            Toast.makeText(this, "SMS preparados para " + lista.size() + " participante(s).", Toast.LENGTH_LONG).show();
            return;
        }

        final Participante p = lista.get(index);
        final String mensagem = gerarMensagemSecreta(p.getNome(), nomesAmigos.get(p.getId()));

        new AlertDialog.Builder(this)
                .setTitle("Enviar para " + p.getNome() + " (" + (index + 1) + "/" + lista.size() + ")")
                .setMessage("Abrir app de SMS para " + p.getTelefone() + "?")
                .setPositiveButton("Abrir SMS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Nao usar Uri.encode: converte '+' de numeros internacionais em '%2B'
                        Uri smsUri = Uri.parse("smsto:" + p.getTelefone());
                        Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
                        intent.putExtra("sms_body", mensagem);
                        try {
                            // Registra o id antes de sair; onResume marca como enviado ao retornar.
                            // Proximo dialog e agendado para onResume para evitar BadTokenException.
                            pendingSmsParticipanteId = p.getId();
                            pendingSmsList = lista;
                            pendingSmsNomesAmigos = nomesAmigos;
                            pendingSmsNextIndex = index + 1;
                            smsLaunched = true;
                            startActivity(intent);
                        } catch (android.content.ActivityNotFoundException e) {
                            pendingSmsParticipanteId = -1;
                            pendingSmsList = null;
                            pendingSmsNomesAmigos = null;
                            pendingSmsNextIndex = -1;
                            Toast.makeText(ParticipantesActivity.this,
                                    "Nenhum app de SMS encontrado.", Toast.LENGTH_SHORT).show();
                            // Postar no Handler evita abrir novo AlertDialog enquanto o atual ainda
                            // esta sendo descartado, prevenindo WindowManager exception.
                            new android.os.Handler(android.os.Looper.getMainLooper())
                                    .post(new Runnable() {
                                        @Override
                                        public void run() {
                                            enviarSmsSequencial(lista, nomesAmigos, index + 1);
                                        }
                                    });
                        }
                    }
                })
                .setNegativeButton("Pular", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Limpa id possivelmente restaurado do bundle para evitar estado inconsistente.
                        // Handler.post adia o proximo dialog ate o atual ser descartado (evita race condition).
                        pendingSmsParticipanteId = -1;
                        new android.os.Handler(android.os.Looper.getMainLooper())
                                .post(new Runnable() {
                                    @Override
                                    public void run() {
                                        enviarSmsSequencial(lista, nomesAmigos, index + 1);
                                    }
                                });
                    }
                })
                .setNeutralButton("Cancelar tudo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pendingSmsParticipanteId = -1;
                        pendingSmsList = null;
                        pendingSmsNomesAmigos = null;
                        pendingSmsNextIndex = -1;
                        smsLaunched = false;
                    }
                })
                .setCancelable(false)
                .show();
    }

    private String gerarMensagemSecreta(String nomeParticipante, String nomeAmigo) {
        if (nomeAmigo == null) nomeAmigo = "???";
        StringBuilder sb = new StringBuilder();
        sb.append("🎁 *Amigo Secreto* 🎁\n\n");
        sb.append("Olá, *").append(nomeParticipante).append("*!\n");
        sb.append("Seu resultado está pronto.\n");
        sb.append("ROLE PARA BAIXO PARA VER\n");
        for (int i = 0; i < 25; i++) sb.append(".\n");
        sb.append("\n🕵️ *Seu Amigo Secreto é:* \n");
        sb.append("✨ *").append(nomeAmigo).append("* ✨\n\n");
        sb.append("Não conte para ninguém! 🤫");
        return sb.toString();
    }

    private void exibirDialogRegras(final Participante p) {
        final List<Participante> outros = new ArrayList<>();
        for (Participante p2 : listaParticipantes) {
            if (p2.getId() != p.getId()) outros.add(p2);
        }
        if (outros.isEmpty()) return;

        String[] nomes = new String[outros.size()];
        final boolean[] selecionados = new boolean[outros.size()];
        for (int i = 0; i < outros.size(); i++) {
            nomes[i] = outros.get(i).getNome();
            selecionados[i] = p.getIdsExcluidos().contains(outros.get(i).getId());
        }

        new AlertDialog.Builder(this)
                .setTitle("Quem " + p.getNome() + " NÃO pode tirar?")
                .setMultiChoiceItems(nomes, selecionados, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        selecionados[which] = isChecked;
                    }
                })
                .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dao.open();
                        for (int i = 0; i < outros.size(); i++) {
                            if (selecionados[i]) dao.adicionarExclusao(p.getId(), outros.get(i).getId());
                            else dao.removerExclusao(p.getId(), outros.get(i).getId());
                        }
                        dao.close();
                        atualizarLista();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private class ParticipantesAdapter extends BaseAdapter {
        private Context ctx;
        private List<Participante> itens;
        private Map<Integer, Integer> desejosCountMap = new HashMap<>();

        public ParticipantesAdapter(Context ctx, List<Participante> itens) {
            this.ctx = ctx;
            this.itens = itens;
        }

        public void setDesejosCountMap(Map<Integer, Integer> desejosCountMap) {
            this.desejosCountMap = desejosCountMap;
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
                convertView = LayoutInflater.from(ctx).inflate(R.layout.item_participante, parent, false);
            }

            final Participante p = itens.get(position);
            TextView tvNumero = convertView.findViewById(R.id.tv_numero);
            TextView tvAvatar = convertView.findViewById(R.id.tv_avatar);
            TextView tvNome = convertView.findViewById(R.id.tv_nome);
            TextView tvEmail = convertView.findViewById(R.id.tv_email);
            TextView tvDesejosCount = convertView.findViewById(R.id.tv_desejos_count);
            ImageButton btnDesejos = convertView.findViewById(R.id.btn_desejos);
            ImageButton btnRegras = convertView.findViewById(R.id.btn_regras);
            ImageButton btnShare = convertView.findViewById(R.id.btn_share);
            ImageButton btnRemover = convertView.findViewById(R.id.btn_remover);

            tvNumero.setText(String.valueOf(position + 1));
            tvAvatar.setText(p.getNome().substring(0, 1).toUpperCase());
            tvNome.setText(p.getNome());

            // Obter count de desejos do map pré-carregado
            Integer countDesejos = desejosCountMap.get(p.getId());
            if (countDesejos == null) countDesejos = 0;

            if (countDesejos > 0) {
                tvDesejosCount.setText(countDesejos + (countDesejos == 1 ? " desejo" : " desejos"));
                tvDesejosCount.setVisibility(View.VISIBLE);
            } else {
                tvDesejosCount.setVisibility(View.GONE);
            }

            if (p.getAmigoSorteadoId() != null && p.getAmigoSorteadoId() > 0) {
                tvEmail.setText(p.isEnviado() ? "✓ Resultado enviado" : "Pronto p/ compartilhar");
                tvEmail.setTextColor(ContextCompat.getColor(ctx, p.isEnviado() ? R.color.text_secondary : R.color.colorAccent));
                btnShare.setVisibility(View.VISIBLE);
            } else {
                tvEmail.setText(p.getIdsExcluidos().isEmpty() ? "Sem restrições" : p.getIdsExcluidos().size() + " restrições");
                tvEmail.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
                btnShare.setVisibility(View.GONE);
            }

            btnDesejos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ctx, ParticipanteDesejosActivity.class);
                    intent.putExtra("participante", p);
                    ctx.startActivity(intent);
                }
            });

            btnRegras.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exibirDialogRegras(p);
                }
            });

            btnShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { compartilharResultado(p); }
            });

            btnRemover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dao.open();
                    dao.remover(p.getId());
                    dao.close();
                    atualizarLista();
                }
            });

            // Click listener apenas na área de informações do participante
            View.OnClickListener revelarListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (p.getAmigoSorteadoId() != null && p.getAmigoSorteadoId() > 0) {
                        Intent intent = new Intent(ParticipantesActivity.this, RevelarAmigoActivity.class);
                        intent.putExtra("participante", p);
                        startActivity(intent);
                    }
                }
            };

            tvNome.setOnClickListener(revelarListener);
            tvEmail.setOnClickListener(revelarListener);
            tvAvatar.setOnClickListener(revelarListener);

            return convertView;
        }

        private void compartilharResultado(Participante p) {
            dao.open();
            String nomeAmigo = dao.getNomeAmigoSorteado(p.getAmigoSorteadoId());
            dao.marcarComoEnviado(p.getId());
            dao.close();
            atualizarLista();

            String mensagem = gerarMensagemSecreta(p.getNome(), nomeAmigo);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, mensagem);
            ctx.startActivity(Intent.createChooser(intent, "Compartilhar com " + p.getNome()));
        }
    }
}
