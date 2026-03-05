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

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.Participante;
import activity.amigosecreto.db.ParticipanteDAO;
import activity.amigosecreto.db.DesejoDAO;
import activity.amigosecreto.util.ValidationUtils;

public class ParticipantesActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int REQUEST_CONTACT_PICKER = 200;

    // ID do participante cujo SMS foi aberto; marcado como enviado no onResume ao retornar.
    private int pendingSmsParticipanteId = -1;
    // True apenas quando startActivity foi chamado nesta sessao (nao quando restaurado de rotacao).
    private boolean smsLaunched = false;
    // Estado da sequencia de SMS; retomado no onResume para evitar dialog durante pausa da activity.
    private List<Participante> pendingSmsList = null;
    // Mensagens SMS já formatadas, mapeadas por participante ID.
    private Map<Integer, String> pendingSmsMensagens = null;
    private int pendingSmsNextIndex = -1;

    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private ListView lvParticipantes;
    private TextView tvCount;
    private MaterialButton fabAdd;
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
            String[] mensagens = savedInstanceState.getStringArray("pendingSmsMensagens");
            if (ids != null && telefones != null && nomes != null && mensagens != null) {
                pendingSmsList = new ArrayList<>();
                pendingSmsMensagens = new HashMap<>();
                for (int i = 0; i < ids.length; i++) {
                    Participante p = new Participante();
                    p.setId(ids[i]);
                    p.setTelefone(telefones[i]);
                    p.setNome(nomes[i]);
                    pendingSmsList.add(p);
                    pendingSmsMensagens.put(ids[i], mensagens[i]);
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

    private void exibirDialogEditar(final Participante participante) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Participante");
        if (participante.isEnviado()) {
            builder.setMessage("⚠️ O resultado deste participante já foi compartilhado. Editar os dados não atualizará a mensagem já enviada.");
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_add_participante, null);
        final EditText etNome = view.findViewById(R.id.et_nome);
        final EditText etTelefone = view.findViewById(R.id.et_telefone);
        final EditText etEmail = view.findViewById(R.id.et_email);
        View btnPickContact = view.findViewById(R.id.btn_pick_contact);
        btnPickContact.setVisibility(View.GONE);

        etNome.setText(participante.getNome());
        etTelefone.setText(participante.getTelefone());
        etEmail.setText(participante.getEmail());

        builder.setView(view);
        // Botoes declarados sem listener aqui; listener registrado apos show() para controlar
        // o dismiss manualmente e evitar que o dialog feche ao falhar na validacao.
        builder.setPositiveButton("Salvar", null);
        builder.setNegativeButton("Cancelar", null);
        AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ValidationUtils.validateName(etNome)) return;
                if (!ValidationUtils.validatePhone(etTelefone)) return;
                if (!ValidationUtils.validateEmail(etEmail)) return;

                String nome = etNome.getText().toString().trim();
                String telefone = etTelefone.getText().toString().trim();
                String email = etEmail.getText().toString().trim();

                // Guarda valores originais para restaurar se o banco falhar
                String nomeOriginal = participante.getNome();
                String telefoneOriginal = participante.getTelefone();
                String emailOriginal = participante.getEmail();
                participante.setNome(nome);
                participante.setTelefone(telefone);
                participante.setEmail(email);
                // DAO local evita conflito com o dao compartilhado da Activity que pode
                // estar aberto em outro fluxo (ex: enviarSmsViaIntent em background).
                boolean ok = false;
                ParticipanteDAO daoLocal = new ParticipanteDAO(ParticipantesActivity.this);
                try {
                    daoLocal.open();
                    ok = daoLocal.atualizar(participante);
                } finally {
                    daoLocal.close();
                }
                if (ok) {
                    atualizarLista();
                    dialog.dismiss();
                } else {
                    // Restaura estado original para manter objeto em sincronia com o banco
                    participante.setNome(nomeOriginal);
                    participante.setTelefone(telefoneOriginal);
                    participante.setEmail(emailOriginal);
                    Toast.makeText(ParticipantesActivity.this,
                            "Erro ao salvar. Tente novamente.", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
            Map<Integer, String> mensagens = pendingSmsMensagens;
            int nextIndex = pendingSmsNextIndex;
            pendingSmsList = null;
            pendingSmsMensagens = null;
            pendingSmsNextIndex = -1;
            enviarSmsSequencial(lista, mensagens, nextIndex);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pendingSmsId", pendingSmsParticipanteId);
        outState.putInt("pendingSmsNextIndex", pendingSmsNextIndex);
        if (pendingSmsList != null && pendingSmsMensagens != null) {
            int[] ids = new int[pendingSmsList.size()];
            String[] telefones = new String[pendingSmsList.size()];
            String[] nomes = new String[pendingSmsList.size()];
            String[] mensagens = new String[pendingSmsList.size()];
            for (int i = 0; i < pendingSmsList.size(); i++) {
                Participante p = pendingSmsList.get(i);
                ids[i] = p.getId();
                telefones[i] = p.getTelefone() != null ? p.getTelefone() : "";
                nomes[i] = p.getNome();
                String msg = pendingSmsMensagens.get(p.getId());
                mensagens[i] = msg != null ? msg : "";
            }
            outState.putIntArray("pendingSmsIds", ids);
            outState.putStringArray("pendingSmsTelefones", telefones);
            outState.putStringArray("pendingSmsNomes", nomes);
            outState.putStringArray("pendingSmsMensagens", mensagens);
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
    // O acesso ao banco e feito em thread de fundo para evitar ANR em grupos grandes.
    private void enviarSmsViaIntent() {
        final List<Participante> snapshot = new ArrayList<>(listaParticipantes);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final List<Participante> comTelefone = new ArrayList<>();
                final Map<Integer, String> mensagensParticipantes = new HashMap<>();
                DesejoDAO desejoDAO = new DesejoDAO(ParticipantesActivity.this);
                try {
                    dao.open();
                    desejoDAO.open();
                    for (Participante p : snapshot) {
                        if (p.getTelefone() != null && !p.getTelefone().trim().isEmpty()) {
                            comTelefone.add(p);
                            String nomeAmigo = dao.getNomeAmigoSorteado(p.getAmigoSorteadoId());
                            List<Desejo> desejos = new ArrayList<>();
                            if (p.getAmigoSorteadoId() != null && p.getAmigoSorteadoId() > 0) {
                                desejos = desejoDAO.listarPorParticipante(p.getAmigoSorteadoId());
                            }
                            mensagensParticipantes.put(p.getId(), gerarMensagemSecreta(p.getNome(), nomeAmigo, desejos));
                        }
                    }
                } finally {
                    dao.close();
                    desejoDAO.close();
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) return;
                        if (comTelefone.isEmpty()) {
                            Toast.makeText(ParticipantesActivity.this,
                                    "Nenhum participante com telefone cadastrado.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        enviarSmsSequencial(comTelefone, mensagensParticipantes, 0);
                    }
                });
            }
        });
        executor.shutdown();
    }

    // Exibe dialog para cada participante antes de abrir o app de SMS, evitando stack de activities.
    private void enviarSmsSequencial(final List<Participante> lista, final Map<Integer, String> mensagensMap, final int index) {
        if (index >= lista.size()) {
            Toast.makeText(this, "SMS preparados para " + lista.size() + " participante(s).", Toast.LENGTH_LONG).show();
            return;
        }

        final Participante p = lista.get(index);
        final String mensagem = mensagensMap.get(p.getId());
        if (mensagem == null || mensagem.isEmpty()) {
            // Mensagem ausente ou vazia (estado inconsistente, ex: restaurado do bundle como ""); pular.
            mainHandler.post(new Runnable() {
                @Override public void run() { enviarSmsSequencial(lista, mensagensMap, index + 1); }
            });
            return;
        }

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
                            pendingSmsMensagens = mensagensMap;
                            pendingSmsNextIndex = index + 1;
                            smsLaunched = true;
                            startActivity(intent);
                        } catch (android.content.ActivityNotFoundException e) {
                            pendingSmsParticipanteId = -1;
                            pendingSmsList = null;
                            pendingSmsMensagens = null;
                            pendingSmsNextIndex = -1;
                            Toast.makeText(ParticipantesActivity.this,
                                    "Nenhum app de SMS encontrado.", Toast.LENGTH_SHORT).show();
                            // Postar no Handler evita abrir novo AlertDialog enquanto o atual ainda
                            // esta sendo descartado, prevenindo WindowManager exception.
                            mainHandler
                                    .post(new Runnable() {
                                        @Override
                                        public void run() {
                                            enviarSmsSequencial(lista, mensagensMap, index + 1);
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
                        mainHandler
                                .post(new Runnable() {
                                    @Override
                                    public void run() {
                                        enviarSmsSequencial(lista, mensagensMap, index + 1);
                                    }
                                });
                    }
                })
                .setNeutralButton("Cancelar tudo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pendingSmsParticipanteId = -1;
                        pendingSmsList = null;
                        pendingSmsMensagens = null;
                        pendingSmsNextIndex = -1;
                        smsLaunched = false;
                    }
                })
                .setCancelable(false)
                .show();
    }

    // Visivel ao pacote para permitir testes unitarios sem reflexao.
    static String formatarPreco(double valor) {
        long inteiro = (long) valor;
        if (Math.abs(valor - inteiro) < 0.005) {
            return String.valueOf(inteiro);
        }
        return String.format(java.util.Locale.US, "%.2f", valor).replace('.', ',');
    }

    private String gerarMensagemSecreta(String nomeParticipante, String nomeAmigo, List<Desejo> desejos) {
        if (nomeAmigo == null) nomeAmigo = "???";
        StringBuilder sb = new StringBuilder();
        sb.append("🎁 *Amigo Secreto* 🎁\n\n");
        sb.append("Olá, *").append(nomeParticipante).append("*!\n\n");
        sb.append("O sorteio foi realizado e você foi escolhido(a) para presentear alguém muito especial!\n");
        sb.append("Role para baixo para descobrir quem é o seu Amigo Secreto 👇\n");
        for (int i = 0; i < 25; i++) sb.append(".\n");
        sb.append("\n🎉 *Seu Amigo Secreto é:*\n");
        sb.append("✨ *").append(nomeAmigo).append("* ✨\n\n");
        if (desejos != null && !desejos.isEmpty()) {
            sb.append("🛍️ *Lista de desejos de ").append(nomeAmigo).append(":*\n");
            int num = 1;
            for (Desejo d : desejos) {
                if (d.getProduto() == null || d.getProduto().trim().isEmpty()) continue;
                sb.append(num++).append(". ").append(d.getProduto());
                if (d.getCategoria() != null && !d.getCategoria().trim().isEmpty()) {
                    sb.append(" (").append(d.getCategoria()).append(")");
                }
                if (d.getPrecoMinimo() > 0 && d.getPrecoMaximo() >= d.getPrecoMinimo()) {
                    sb.append(" — R$ ").append(formatarPreco(d.getPrecoMinimo()))
                      .append(" a R$ ").append(formatarPreco(d.getPrecoMaximo()));
                } else if (d.getPrecoMinimo() > 0) {
                    sb.append(" — a partir de R$ ").append(formatarPreco(d.getPrecoMinimo()));
                } else if (d.getPrecoMaximo() > 0) {
                    sb.append(" — até R$ ").append(formatarPreco(d.getPrecoMaximo()));
                }
                if (d.getLojas() != null && !d.getLojas().trim().isEmpty()) {
                    sb.append(" 🏪 ").append(d.getLojas());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        sb.append("Lembre-se: o segredo é seu! Não conte para ninguém. 🤫");
        return sb.toString();
    }

    private void exibirDialogRegras(final Participante p) {
        final List<Participante> outros = new ArrayList<>();
        for (Participante p2 : listaParticipantes) {
            if (p2.getId() != p.getId()) outros.add(p2);
        }
        if (outros.isEmpty()) return;

        // Inflar o layout customizado
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_regras, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        android.widget.LinearLayout layoutLista = dialogView.findViewById(R.id.layout_lista_participantes);

        tvTitle.setText("Restrições para " + p.getNome());
        tvSubtitle.setText("Selecione quem NÃO pode ser sorteado");

        // Criar checkboxes para cada participante
        final boolean[] selecionados = new boolean[outros.size()];
        for (int i = 0; i < outros.size(); i++) {
            final Participante outro = outros.get(i);
            selecionados[i] = p.getIdsExcluidos().contains(outro.getId());

            View itemView = getLayoutInflater().inflate(R.layout.item_regra_checkbox, layoutLista, false);
            TextView tvAvatar = itemView.findViewById(R.id.tv_avatar_regra);
            TextView tvNome = itemView.findViewById(R.id.tv_nome_regra);
            com.google.android.material.checkbox.MaterialCheckBox checkbox = itemView.findViewById(R.id.checkbox_regra);

            tvAvatar.setText(outro.getNome().substring(0, 1).toUpperCase());
            tvNome.setText(outro.getNome());
            checkbox.setChecked(selecionados[i]);

            final int index = i;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkbox.setChecked(!checkbox.isChecked());
                    selecionados[index] = checkbox.isChecked();
                }
            });

            checkbox.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    selecionados[index] = isChecked;
                }
            });

            layoutLista.addView(itemView);
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
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
            ImageButton btnEditar = convertView.findViewById(R.id.btn_editar);
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

            btnEditar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exibirDialogEditar(p);
                }
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

            return convertView;
        }

        private void compartilharResultado(Participante p) {
            String nomeAmigo = null;
            List<Desejo> desejos = new ArrayList<>();
            DesejoDAO desejoDAO = new DesejoDAO(ctx);
            try {
                dao.open();
                desejoDAO.open();
                nomeAmigo = dao.getNomeAmigoSorteado(p.getAmigoSorteadoId());
                if (p.getAmigoSorteadoId() != null && p.getAmigoSorteadoId() > 0) {
                    desejos = desejoDAO.listarPorParticipante(p.getAmigoSorteadoId());
                }
                // Marca como enviado apenas apos obter todos os dados necessarios para a mensagem.
                // TODO: idealmente marcarComoEnviado deveria ser chamado apos confirmacao do usuario
                //       (ex: callback do share sheet), mas a API do ACTION_SEND nao oferece esse callback.
                dao.marcarComoEnviado(p.getId());
            } finally {
                dao.close();
                desejoDAO.close();
            }
            atualizarLista();

            String mensagem = gerarMensagemSecreta(p.getNome(), nomeAmigo, desejos);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, mensagem);
            ctx.startActivity(Intent.createChooser(intent, "Compartilhar com " + p.getNome()));
        }
    }
}
