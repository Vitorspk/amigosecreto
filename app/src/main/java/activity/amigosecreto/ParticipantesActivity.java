package activity.amigosecreto;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import activity.amigosecreto.db.Grupo;
import activity.amigosecreto.db.Participante;
import activity.amigosecreto.repository.ParticipanteRepository;
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
    // Índice de retomada após rotação quando ViewModel reconstrói mensagens em background.
    private int pendingSmsResumeIndex = -1;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ParticipantesViewModel viewModel;

    private ListView lvParticipantes;
    private TextView tvCount;
    private MaterialButton fabAdd;
    private View btnSortear;
    private View btnLimpar;
    private ParticipanteRepository participanteRepository;
    private List<Participante> listaParticipantes = new ArrayList<>();
    private ParticipantesAdapter adapter;
    private Grupo grupoAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_listar_participantes);

        // Ajusta padding do container de botoes inferiores para nao ficar atras
        // da navigation bar em modo edge-to-edge (Android 15+).
        View bottomButtons = findViewById(R.id.layout_bottom_buttons);
        if (bottomButtons != null) {
            final int padBottom = bottomButtons.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(bottomButtons, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        padBottom + systemBars.bottom);
                return insets;
            });
        }

        grupoAtual = (Grupo) getIntent().getSerializableExtra("grupo");
        if (grupoAtual == null) {
            finish();
            return;
        }

        if (savedInstanceState != null) {
            pendingSmsParticipanteId = savedInstanceState.getInt("pendingSmsId", -1);
            pendingSmsNextIndex = savedInstanceState.getInt("pendingSmsNextIndex", -1);
            pendingSmsResumeIndex = savedInstanceState.getInt("pendingSmsResumeIndex", -1);
            int[] ids = savedInstanceState.getIntArray("pendingSmsIds");
            String[] telefones = savedInstanceState.getStringArray("pendingSmsTelefones");
            String[] nomes = savedInstanceState.getStringArray("pendingSmsNomes");
            // pendingSmsMensagens nao e salvo no bundle (risco de TransactionTooLarge);
            // sera reconstruido via banco em onResume.
            if (ids != null && telefones != null && nomes != null) {
                pendingSmsList = new ArrayList<>();
                for (int i = 0; i < ids.length; i++) {
                    Participante p = new Participante();
                    p.setId(ids[i]);
                    p.setTelefone(telefones[i]);
                    p.setNome(nomes[i]);
                    pendingSmsList.add(p);
                }
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(grupoAtual.getNome());
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        participanteRepository = new ParticipanteRepository(this);
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

        // Inicializar ViewModel e observar LiveData.
        // init() dispara o primeiro carregamento; rotação reutiliza o ViewModel existente.
        viewModel = new ViewModelProvider(this).get(ParticipantesViewModel.class);
        viewModel.init(grupoAtual.getId());

        viewModel.getParticipants().observe(this, participantes -> {
            listaParticipantes.clear();
            listaParticipantes.addAll(participantes);
            adapter.notifyDataSetChanged();
            if (listaParticipantes.isEmpty()) {
                tvCount.setText(R.string.label_no_participants);
            } else {
                tvCount.setText(getResources().getQuantityString(
                        R.plurals.label_participants_in_group,
                        listaParticipantes.size(),
                        listaParticipantes.size(),
                        grupoAtual.getNome()));
            }
        });

        viewModel.getWishCounts().observe(this, counts -> {
            adapter.setDesejosCountMap(counts);
            adapter.notifyDataSetChanged();
        });

        viewModel.getSorteioResult().observe(this, resultado -> {
            if (resultado == null) return;
            viewModel.clearSorteioResult();
            switch (resultado.status) {
                case FAILURE_NOT_ENOUGH:
                    Toast.makeText(this, getString(R.string.participante_sorteio_minimo), Toast.LENGTH_LONG).show();
                    break;
                case FAILURE_IMPOSSIBLE:
                    Toast.makeText(this, getString(R.string.participante_sorteio_impossivel), Toast.LENGTH_LONG).show();
                    break;
                case SUCCESS:
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.participante_sorteio_titulo))
                            .setMessage(getString(R.string.participante_sorteio_msg_sms))
                            .setPositiveButton(getString(R.string.participante_sorteio_btn_sms), (dialog, which) -> enviarSmsViaIntent())
                            .setNegativeButton("Não", null)
                            .show();
                    break;
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg == null) return;
            viewModel.clearErrorMessage();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });

        // Mensagens SMS prontas — iniciar (ou retomar) sequência de envio.
        // pendingSmsResumeIndex é >= 0 quando estamos retomando após rotação de tela.
        viewModel.getMensagensSmsResult().observe(this, resultado -> {
            if (resultado == null) return;
            viewModel.clearMensagensSmsResult();
            if (resultado.participantesComTelefone.isEmpty()) {
                Toast.makeText(this, R.string.error_no_phone_participants, Toast.LENGTH_LONG).show();
                return;
            }
            int startIndex = pendingSmsResumeIndex >= 0 ? pendingSmsResumeIndex : 0;
            pendingSmsResumeIndex = -1;
            enviarSmsSequencial(resultado.participantesComTelefone, resultado.mensagens, startIndex);
        });

        // Mensagem de compartilhamento pronta — abrir share sheet.
        viewModel.getMensagemCompartilhamentoResult().observe(this, resultado -> {
            if (resultado == null) return;
            viewModel.clearMensagemCompartilhamentoResult();
            atualizarLista();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, resultado.mensagem);
            startActivity(Intent.createChooser(intent,
                    getString(R.string.share_with_person, resultado.participante.getNome())));
        });
    }

    private void atualizarLista() {
        viewModel.carregarParticipantes();
    }

    private void exibirDialogAdd() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_new_participant_title);

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
        // Botoes declarados sem listener aqui; listener registrado apos show() para controlar
        // o dismiss manualmente e evitar que o dialog feche ao falhar na validacao.
        builder.setPositiveButton(getString(R.string.button_add), null);
        builder.setNegativeButton(getString(R.string.button_cancel), null);
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

                Participante p = new Participante();
                p.setNome(nome);
                p.setTelefone(telefone);
                p.setEmail(email);
                viewModel.inserirParticipante(p, grupoAtual.getId());
                dialog.dismiss();
            }
        });
    }

    private void exibirDialogEditar(final Participante participante) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_edit_participant_title);
        if (participante.isEnviado()) {
            builder.setMessage(R.string.dialog_edit_sent_warning);
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
        builder.setPositiveButton(getString(R.string.button_save), null);
        builder.setNegativeButton(getString(R.string.button_cancel), null);
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

                // Captura valores editados e originais antes de entrar na thread
                final String nomeFinal = nome;
                final String telefoneFinal = telefone;
                final String emailFinal = email;
                final String nomeOriginal = participante.getNome();
                final String telefoneOriginal = participante.getTelefone();
                final String emailOriginal = participante.getEmail();

                // Desabilita o botao para evitar duplo toque enquanto o banco salva
                v.setEnabled(false);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            boolean ok = false;
                            // Aplica valores no objeto antes de tentar salvar no banco
                            participante.setNome(nomeFinal);
                            participante.setTelefone(telefoneFinal);
                            participante.setEmail(emailFinal);
                            // DAO local evita conflito com o dao compartilhado da Activity.
                            try {
                                ok = participanteRepository.atualizar(participante);
                            } catch (Exception e) {
                                ok = false;
                            }
                            final boolean sucesso = ok;
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    v.setEnabled(true);
                                    if (isFinishing() || isDestroyed()) return;
                                    if (sucesso) {
                                        atualizarLista();
                                        dialog.dismiss();
                                    } else {
                                        // Restaura estado original para manter objeto em sincronia com o banco
                                        participante.setNome(nomeOriginal);
                                        participante.setTelefone(telefoneOriginal);
                                        participante.setEmail(emailOriginal);
                                        Toast.makeText(ParticipantesActivity.this,
                                                R.string.error_save_failed, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
                } finally {
                    executor.shutdown();
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
                    viewModel.inserirParticipante(p, grupoAtual.getId());
                    Toast.makeText(this, getString(R.string.toast_contact_added_format, name), Toast.LENGTH_SHORT).show();
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
            viewModel.marcarComoEnviado(pendingSmsParticipanteId);
            pendingSmsParticipanteId = -1;
            smsLaunched = false;
        }
        // Atualizar lista ao voltar para esta activity (ex: depois de adicionar desejos)
        atualizarLista();
        // Retoma sequencia de SMS apos retornar do app de mensagens (evita dialog durante pausa).
        // Apos rotacao, pendingSmsMensagens e null (nao foi salvo no bundle); reconstroi via banco.
        if (pendingSmsList != null && pendingSmsNextIndex >= 0) {
            final List<Participante> lista = pendingSmsList;
            final int nextIndex = pendingSmsNextIndex;
            pendingSmsList = null;
            pendingSmsNextIndex = -1;

            if (pendingSmsMensagens != null) {
                // Mensagens ja disponiveis (fluxo normal sem rotacao)
                Map<Integer, String> mensagens = pendingSmsMensagens;
                pendingSmsMensagens = null;
                enviarSmsSequencial(lista, mensagens, nextIndex);
            } else {
                // Mensagens perdidas por rotacao: ViewModel reconstroi a partir do banco.
                // O observer de mensagensSmsResult retoma a partir de pendingSmsResumeIndex.
                pendingSmsResumeIndex = nextIndex;
                viewModel.prepararMensagensSms(lista);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pendingSmsId", pendingSmsParticipanteId);
        outState.putInt("pendingSmsNextIndex", pendingSmsNextIndex);
        outState.putInt("pendingSmsResumeIndex", pendingSmsResumeIndex);
        // Salva apenas IDs, telefones e nomes — omite as mensagens formatadas para evitar
        // TransactionTooLargeException (~1 MB Binder limit) em grupos com listas de desejos longas.
        // As mensagens sao reconstruidas a partir do banco no onResume apos rotacao.
        if (pendingSmsList != null) {
            int[] ids = new int[pendingSmsList.size()];
            String[] telefones = new String[pendingSmsList.size()];
            String[] nomes = new String[pendingSmsList.size()];
            for (int i = 0; i < pendingSmsList.size(); i++) {
                Participante p = pendingSmsList.get(i);
                ids[i] = p.getId();
                telefones[i] = p.getTelefone() != null ? p.getTelefone() : "";
                nomes[i] = p.getNome() != null ? p.getNome() : "";
            }
            outState.putIntArray("pendingSmsIds", ids);
            outState.putStringArray("pendingSmsTelefones", telefones);
            outState.putStringArray("pendingSmsNomes", nomes);
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
                .setTitle(getString(R.string.participante_limpar_titulo))
                .setMessage(getString(R.string.participante_limpar_msg))
                .setPositiveButton(getString(R.string.participante_limpar_btn_sim), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewModel.deletarTodosDoGrupo(grupoAtual.getId());
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void realizarSorteio() {
        viewModel.realizarSorteio();
    }

    // Envia SMS abrindo o app de mensagens do dispositivo via Intent (sem permissao SEND_SMS).
    // O usuario confirma e envia um por um — compativel com Play Store sem restricoes.
    // O acesso ao banco e feito pelo ViewModel em background; o resultado chega via LiveData.
    private void enviarSmsViaIntent() {
        viewModel.prepararMensagensSms(new ArrayList<>(listaParticipantes));
    }

    // Exibe dialog para cada participante antes de abrir o app de SMS, evitando stack de activities.
    private void enviarSmsSequencial(final List<Participante> lista, final Map<Integer, String> mensagensMap, final int index) {
        if (index >= lista.size()) {
            // Conta apenas participantes com mensagem valida (exclui pulados por mensagem ausente).
            int enviados = 0;
            for (Participante p : lista) {
                String m = mensagensMap.get(p.getId());
                if (m != null && !m.isEmpty()) enviados++;
            }
            Toast.makeText(this, getString(R.string.toast_sms_prepared_format, enviados), Toast.LENGTH_LONG).show();
            return;
        }

        final Participante p = lista.get(index);
        final String mensagem = mensagensMap.get(p.getId());
        if (mensagem == null || mensagem.isEmpty()) {
            // Mensagem ausente ou vazia (estado inconsistente, ex: restaurado do bundle como ""); pular.
            // Handler.post e usado por stack-safety: evita stack overflow em listas longas onde
            // multiplos itens consecutivos sao pulados, convertendo recursao em iteracao no loop de mensagens.
            mainHandler.post(new Runnable() {
                @Override public void run() { enviarSmsSequencial(lista, mensagensMap, index + 1); }
            });
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_send_sms_title_format, p.getNome(), index + 1, lista.size()))
                .setMessage(getString(R.string.dialog_send_sms_message_format, p.getTelefone()))
                .setPositiveButton(R.string.button_open_sms, new DialogInterface.OnClickListener() {
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
                                    R.string.error_no_sms_app, Toast.LENGTH_SHORT).show();
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
                .setNegativeButton(R.string.button_skip, new DialogInterface.OnClickListener() {
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
                .setNeutralButton(R.string.button_cancel_all, new DialogInterface.OnClickListener() {
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

        tvTitle.setText(getString(R.string.dialog_restrictions_title_format, p.getNome()));
        tvSubtitle.setText(R.string.dialog_restrictions_subtitle);

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
                .setPositiveButton(R.string.button_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<Integer> adicionar = new ArrayList<>();
                        List<Integer> remover = new ArrayList<>();
                        for (int i = 0; i < outros.size(); i++) {
                            if (selecionados[i]) adicionar.add(outros.get(i).getId());
                            else remover.add(outros.get(i).getId());
                        }
                        viewModel.salvarExclusoes(p.getId(), adicionar, remover);
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
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
                tvDesejosCount.setText(ctx.getResources().getQuantityString(R.plurals.label_wishes_count, countDesejos, countDesejos));
                tvDesejosCount.setVisibility(View.VISIBLE);
            } else {
                tvDesejosCount.setVisibility(View.GONE);
            }

            if (p.getAmigoSorteadoId() != null && p.getAmigoSorteadoId() > 0) {
                tvEmail.setText(p.isEnviado() ? getString(R.string.status_result_sent) : getString(R.string.status_ready_share));
                tvEmail.setTextColor(ContextCompat.getColor(ctx, p.isEnviado() ? R.color.text_secondary : R.color.colorAccent));
                btnShare.setVisibility(View.VISIBLE);
            } else {
                tvEmail.setText(p.getIdsExcluidos().isEmpty()
                        ? getString(R.string.status_no_restrictions)
                        : ctx.getResources().getQuantityString(R.plurals.label_restrictions_count, p.getIdsExcluidos().size(), p.getIdsExcluidos().size()));
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
                public void onClick(View v) {
                    // Desabilitar imediatamente para evitar multiplos taps que disparariam
                    // requisicoes duplicadas. Reabilitado quando notifyDataSetChanged() recria
                    // as views apos atualizarLista() no observer de mensagemCompartilhamento.
                    v.setEnabled(false);
                    compartilharResultado(p);
                }
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
                    viewModel.removerParticipante(p.getId());
                }
            });

            return convertView;
        }

        private void compartilharResultado(final Participante p) {
            // ViewModel prepara mensagem em background; resultado chega via observer de
            // mensagemCompartilhamentoResult em onCreate(). atualizarLista() no observer
            // dispara notifyDataSetChanged(), que recria as views e reabilita o botão.
            viewModel.prepararMensagemCompartilhamento(p);
        }
    }
}
