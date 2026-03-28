package activity.amigosecreto

import android.Manifest
import timber.log.Timber
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import dagger.hilt.android.AndroidEntryPoint
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.util.CompartilharHelper
import activity.amigosecreto.util.StateViewHelper
import activity.amigosecreto.util.ValidationUtils

@AndroidEntryPoint
class ParticipantesActivity : AppCompatActivity() {

    private companion object {
        private const val TAG = "ParticipantesActivity"
        const val PERMISSIONS_REQUEST_READ_CONTACTS = 100
        const val REQUEST_CONTACT_PICKER = 200
    }

    /**
     * Estado de um fluxo de envio sequencial (SMS ou WhatsApp).
     *
     * [launched] NÃO é salvo no bundle — deve ser false após rotação, pois o app
     * externo nunca chegou a ser aberto na nova instância da Activity.
     * [pendingMensagens] NÃO é salvo no bundle para evitar TransactionTooLargeException
     * (~1 MB Binder limit) em grupos com listas de desejos longas; reconstruído via banco.
     */
    private class SequentialSendState {
        var pendingParticipanteId: Int = -1
        var launched: Boolean = false
        var pendingList: List<Participante>? = null
        var pendingMensagens: Map<Int, String>? = null
        var pendingNextIndex: Int = -1
        var pendingResumeIndex: Int = -1
    }

    private var smsState = SequentialSendState()
    private var whatsAppState = SequentialSendState()

    // Estado do dialog de edição em andamento; usado pelo observer de atualizarSucesso.
    private var pendingEditDialog: AlertDialog? = null
    private var pendingEditButton: View? = null
    private var pendingEditParticipante: Participante? = null
    private var pendingEditNomeOriginal: String? = null
    private var pendingEditTelefoneOriginal: String? = null
    private var pendingEditEmailOriginal: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var viewModel: ParticipantesViewModel

    private lateinit var lvParticipantes: android.widget.ListView
    private lateinit var tvCount: TextView
    private lateinit var fabAdd: MaterialButton
    private lateinit var btnSortear: View
    private lateinit var btnLimpar: View
    private val listaParticipantes = mutableListOf<Participante>()
    private lateinit var adapter: ParticipantesAdapter
    private lateinit var grupoAtual: Grupo
    private lateinit var stateHelper: StateViewHelper

    private val configuracoesGrupoLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Atualiza grupoAtual com os dados editados retornados pela ConfiguracoesGrupoActivity
            @Suppress("DEPRECATION")
            val grupoAtualizado = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra(Grupo.EXTRA_GRUPO, Grupo::class.java)
            } else {
                result.data?.getSerializableExtra(Grupo.EXTRA_GRUPO) as? Grupo
            }
            if (grupoAtualizado != null) {
                grupoAtual = grupoAtualizado
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listar_participantes)

        // Ajusta padding do container de botoes inferiores para nao ficar atras
        // da navigation bar em modo edge-to-edge (Android 15+).
        // Tambem propaga a altura total do painel como paddingBottom do conteudo do scroll,
        // garantindo que o ultimo item da lista seja sempre rolavel para alem dos botoes.
        val bottomButtons = findViewById<View>(R.id.layout_bottom_buttons)
        val scrollContent = findViewById<View>(R.id.layout_scroll_content)
        if (bottomButtons != null) {
            val padBottom = bottomButtons.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(bottomButtons) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, padBottom + systemBars.bottom)
                insets
            }
            // Atualiza paddingBottom do conteudo sempre que o painel de botoes mudar de altura.
            // O padding e aplicado no LinearLayout filho do NestedScrollView (nao no proprio scroll)
            // pois e o conteudo que precisa ter espaco extra — o NestedScrollView ja tem clipToPadding=false.
            bottomButtons.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
                val panelHeight = bottom - top
                Timber.d("SCROLL_FIX: bottomButtons top=$top bottom=$bottom panelHeight=$panelHeight scrollContent.paddingBottom=${scrollContent?.paddingBottom}")
                scrollContent?.setPadding(
                    scrollContent.paddingLeft,
                    scrollContent.paddingTop,
                    scrollContent.paddingRight,
                    panelHeight
                )
                Timber.d("SCROLL_FIX: after setPadding scrollContent.paddingBottom=${scrollContent?.paddingBottom}")
            }
        }

        @Suppress("DEPRECATION")
        val g = intent.getSerializableExtra(Grupo.EXTRA_GRUPO) as? Grupo
        if (g == null) {
            finish()
            return
        }
        grupoAtual = g

        if (savedInstanceState != null) {
            smsState.pendingParticipanteId = savedInstanceState.getInt("pendingSmsId", -1)
            smsState.pendingNextIndex = savedInstanceState.getInt("pendingSmsNextIndex", -1)
            smsState.pendingResumeIndex = savedInstanceState.getInt("pendingSmsResumeIndex", -1)
            // pendingMensagens nao e salvo no bundle (risco de TransactionTooLarge);
            // sera reconstruido via banco em onResume.
            smsState.pendingList = restoreParticipantListFromBundle(savedInstanceState, "pendingSms")

            // Restaura estado da sequência WhatsApp (mesma estratégia que SMS).
            whatsAppState.pendingParticipanteId = savedInstanceState.getInt("pendingWhatsAppId", -1)
            whatsAppState.pendingNextIndex = savedInstanceState.getInt("pendingWhatsAppNextIndex", -1)
            whatsAppState.pendingResumeIndex = savedInstanceState.getInt("pendingWhatsAppResumeIndex", -1)
            whatsAppState.pendingList = restoreParticipantListFromBundle(savedInstanceState, "pendingWhatsApp")
        }

        supportActionBar?.apply {
            title = grupoAtual.nome
            setDisplayHomeAsUpEnabled(true)
        }

        lvParticipantes = findViewById(R.id.lv_participantes)
        tvCount = findViewById(R.id.tv_count)
        fabAdd = findViewById(R.id.fab_add_participante)
        btnSortear = findViewById(R.id.btn_sortear)
        btnLimpar = findViewById(R.id.btn_limpar)

        stateHelper = StateViewHelper(
            stubLoading = findViewById(R.id.stub_loading),
            stubEmpty = findViewById(R.id.stub_empty),
            contentView = lvParticipantes
        )

        adapter = ParticipantesAdapter(this, listaParticipantes)
        lvParticipantes.adapter = adapter

        fabAdd.setOnClickListener { exibirDialogAdd() }
        btnSortear.setOnClickListener { realizarSorteio() }
        btnLimpar.setOnClickListener { confirmarLimparTudo() }

        // Inicializar ViewModel e observar LiveData.
        // init() dispara o primeiro carregamento; rotação reutiliza o ViewModel existente.
        viewModel = ViewModelProvider(this)[ParticipantesViewModel::class.java]
        viewModel.init(grupoAtual.id)

        // isLoading drives only the loading state. The empty/content transition is driven
        // solely by the participants observer below, which fires before isLoading=false
        // (ViewModel posts _participants then _isLoading). On error paths, handleDbError
        // posts errorMessage (shown as Toast) but does not update _participants — the
        // participants observer will re-fire when carregarParticipantes() is next called.
        viewModel.isLoading.observe(this) { loading ->
            if (loading) stateHelper.showLoading()
        }

        viewModel.participants.observe(this) { participantes ->
            listaParticipantes.clear()
            listaParticipantes.addAll(participantes)
            adapter.notifyDataSetChanged()
            if (listaParticipantes.isEmpty()) {
                stateHelper.showEmpty()
                tvCount.setText(R.string.label_no_participants)
            } else {
                stateHelper.showContent()
                tvCount.text = resources.getQuantityString(
                    R.plurals.label_participants_in_group,
                    listaParticipantes.size,
                    listaParticipantes.size,
                    grupoAtual.nome
                )
            }
        }

        viewModel.wishCounts.observe(this) { counts ->
            adapter.setDesejosCountMap(counts)
            adapter.notifyDataSetChanged()
        }

        viewModel.sorteioResult.observe(this) { resultado ->
            if (resultado == null) return@observe
            viewModel.clearSorteioResult()
            when (resultado.status) {
                ParticipantesViewModel.SorteioResultado.Status.FAILURE_NOT_ENOUGH ->
                    Toast.makeText(this, getString(R.string.participante_sorteio_minimo), Toast.LENGTH_LONG).show()
                ParticipantesViewModel.SorteioResultado.Status.FAILURE_IMPOSSIBLE ->
                    Toast.makeText(this, getString(R.string.participante_sorteio_impossivel), Toast.LENGTH_LONG).show()
                ParticipantesViewModel.SorteioResultado.Status.SUCCESS -> {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.participante_sorteio_titulo))
                        .setMessage(getString(R.string.participante_sorteio_msg_canal))
                        .setPositiveButton(getString(R.string.participante_sorteio_btn_whatsapp)) { _, _ -> enviarWhatsAppViaIntent() }
                        .setNegativeButton(getString(R.string.participante_sorteio_btn_sms_curto)) { _, _ -> enviarSmsViaIntent() }
                        .setNeutralButton(getString(R.string.participante_sorteio_btn_agora_nao), null)
                        .show()
                }
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (msg == null) return@observe
            viewModel.clearErrorMessage()
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }

        // Mensagens SMS prontas — iniciar (ou retomar) sequência de envio.
        // pendingSmsResumeIndex é >= 0 quando estamos retomando após rotação de tela.
        viewModel.mensagensSmsResult.observe(this) { resultado ->
            if (resultado == null) return@observe
            viewModel.clearMensagensSmsResult()
            if (resultado.participantesComTelefone.isEmpty()) {
                Toast.makeText(this, R.string.error_no_phone_participants, Toast.LENGTH_LONG).show()
                return@observe
            }
            val startIndex = if (smsState.pendingResumeIndex >= 0) smsState.pendingResumeIndex else 0
            smsState.pendingResumeIndex = -1
            enviarSmsSequencial(resultado.participantesComTelefone, resultado.mensagens, startIndex)
        }

        // Mensagens WhatsApp prontas — iniciar (ou retomar) sequência de envio via wa.me.
        viewModel.mensagensWhatsAppResult.observe(this) { resultado ->
            if (resultado == null) return@observe
            viewModel.clearMensagensWhatsAppResult()
            if (resultado.participantesComTelefone.isEmpty()) {
                Toast.makeText(this, R.string.error_no_phone_participants, Toast.LENGTH_LONG).show()
                return@observe
            }
            val startIndex = if (whatsAppState.pendingResumeIndex >= 0) whatsAppState.pendingResumeIndex else 0
            whatsAppState.pendingResumeIndex = -1
            enviarWhatsAppSequencial(resultado.participantesComTelefone, resultado.mensagens, startIndex)
        }

        // Resultado de atualizar participante — fechar dialog se sucesso, restaurar estado se falha.
        viewModel.atualizarSucesso.observe(this) { sucesso ->
            if (sucesso == null) return@observe
            viewModel.clearAtualizarSucesso()
            pendingEditButton?.isEnabled = true
            if (sucesso) {
                pendingEditDialog?.dismiss()
            } else {
                pendingEditParticipante?.let { p ->
                    p.nome = pendingEditNomeOriginal ?: p.nome
                    p.telefone = pendingEditTelefoneOriginal ?: p.telefone
                    p.email = pendingEditEmailOriginal ?: p.email
                }
                Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show()
            }
            pendingEditDialog = null
            pendingEditButton = null
            pendingEditParticipante = null
            pendingEditNomeOriginal = null
            pendingEditTelefoneOriginal = null
            pendingEditEmailOriginal = null
        }

        // Mensagem de compartilhamento pronta — exibir bottom sheet de opções de envio.
        viewModel.mensagemCompartilhamentoResult.observe(this) { resultado ->
            if (resultado == null) return@observe
            viewModel.clearMensagemCompartilhamentoResult()
            atualizarLista()
            exibirBottomSheetCompartilhar(
                resultado.mensagem,
                resultado.participante.nome ?: "",
                resultado.participante.telefone
            )
        }

        // Nome do amigo obtido — abrir QrCodeActivity.
        viewModel.qrCodeResult.observe(this) { resultado ->
            if (resultado == null) return@observe
            viewModel.clearQrCodeResult()
            val intent = Intent(this, QrCodeActivity::class.java).apply {
                putExtra(QrCodeActivity.EXTRA_NOME_PARTICIPANTE, resultado.nomeParticipante)
                putExtra(QrCodeActivity.EXTRA_CONTEUDO_QR, resultado.nomeAmigo)
            }
            startActivity(intent)
        }
    }

    private fun atualizarLista() {
        viewModel.carregarParticipantes()
    }

    /**
     * Exibe o bottom sheet de compartilhamento para um participante específico.
     *
     * @param telefone quando não-nulo/vazio, o botão WhatsApp usa deep link wa.me com o
     *   número pré-preenchido — o participante certo já aparece selecionado no WhatsApp.
     *   Sem telefone, abre o WhatsApp sem destinatário (comportamento anterior).
     */
    private fun exibirBottomSheetCompartilhar(mensagem: String, nomeParticipante: String, telefone: String?) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_compartilhar, null)
        sheet.setContentView(view)

        view.findViewById<android.widget.TextView>(R.id.tv_share_titulo).text =
            getString(R.string.share_sheet_titulo, nomeParticipante)

        val btnWhatsApp = view.findViewById<View>(R.id.btn_share_whatsapp)
        val btnTelegram = view.findViewById<View>(R.id.btn_share_telegram)

        // Oculta botões de apps não instalados — evita confusão ao cair no share sheet genérico
        btnWhatsApp.visibility = if (CompartilharHelper.isWhatsAppInstalado(this)) View.VISIBLE else View.GONE
        btnTelegram.visibility = if (CompartilharHelper.isTelegramInstalado(this)) View.VISIBLE else View.GONE

        btnWhatsApp.setOnClickListener {
            sheet.dismiss()
            val titulo = getString(R.string.share_with_person, nomeParticipante)
            if (!telefone.isNullOrBlank()) {
                // Usa deep link wa.me para pré-preencher o número do participante
                CompartilharHelper.compartilharWhatsAppComTelefone(this, telefone, mensagem, titulo)
            } else {
                CompartilharHelper.compartilharWhatsApp(this, mensagem, titulo)
            }
        }
        btnTelegram.setOnClickListener {
            sheet.dismiss()
            CompartilharHelper.compartilharTelegram(this, mensagem,
                getString(R.string.share_with_person, nomeParticipante))
        }
        view.findViewById<View>(R.id.btn_share_email).setOnClickListener {
            sheet.dismiss()
            CompartilharHelper.compartilharEmail(this, mensagem,
                getString(R.string.share_email_assunto))
        }
        view.findViewById<View>(R.id.btn_share_outros).setOnClickListener {
            sheet.dismiss()
            CompartilharHelper.compartilharGenerico(this, mensagem,
                getString(R.string.share_with_person, nomeParticipante))
        }

        sheet.show()
    }

    private fun exibirDialogAdd() {
        val view = layoutInflater.inflate(R.layout.dialog_add_participante, null)
        val etNome = view.findViewById<EditText>(R.id.et_nome)
        val etTelefone = view.findViewById<EditText>(R.id.et_telefone)
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val btnPickContact = view.findViewById<View>(R.id.btn_pick_contact)

        btnPickContact.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), PERMISSIONS_REQUEST_READ_CONTACTS)
            } else {
                abrirSeletorContatos()
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.dialog_new_participant_title)
        builder.setView(view)
        // Botoes declarados sem listener aqui; listener registrado apos show() para controlar
        // o dismiss manualmente e evitar que o dialog feche ao falhar na validacao.
        builder.setPositiveButton(getString(R.string.button_add), null)
        builder.setNegativeButton(getString(R.string.button_cancel), null)
        val dialog = builder.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!ValidationUtils.validateName(etNome)) return@setOnClickListener
            if (!ValidationUtils.validatePhone(etTelefone)) return@setOnClickListener
            if (!ValidationUtils.validateEmail(etEmail)) return@setOnClickListener

            val nome = etNome.text.toString().trim()
            val telefone = etTelefone.text.toString().trim()
            val email = etEmail.text.toString().trim()

            val p = Participante()
            p.nome = nome
            p.telefone = telefone
            p.email = email
            viewModel.inserirParticipante(p, grupoAtual.id)
            dialog.dismiss()
        }
    }

    private fun exibirDialogEditar(participante: Participante) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.dialog_edit_participant_title)
        if (participante.isEnviado) {
            builder.setMessage(R.string.dialog_edit_sent_warning)
        }

        val view = layoutInflater.inflate(R.layout.dialog_add_participante, null)
        val etNome = view.findViewById<EditText>(R.id.et_nome)
        val etTelefone = view.findViewById<EditText>(R.id.et_telefone)
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val btnPickContact = view.findViewById<View>(R.id.btn_pick_contact)
        btnPickContact.visibility = View.GONE

        etNome.setText(participante.nome)
        etTelefone.setText(participante.telefone)
        etEmail.setText(participante.email)

        builder.setView(view)
        // Botoes declarados sem listener aqui; listener registrado apos show() para controlar
        // o dismiss manualmente e evitar que o dialog feche ao falhar na validacao.
        builder.setPositiveButton(getString(R.string.button_save), null)
        builder.setNegativeButton(getString(R.string.button_cancel), null)
        val dialog = builder.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { v ->
            if (!ValidationUtils.validateName(etNome)) return@setOnClickListener
            if (!ValidationUtils.validatePhone(etTelefone)) return@setOnClickListener
            if (!ValidationUtils.validateEmail(etEmail)) return@setOnClickListener

            val nome = etNome.text.toString().trim()
            val telefone = etTelefone.text.toString().trim()
            val email = etEmail.text.toString().trim()

            // Salva estado original e referências para o observer de atualizarSucesso.
            pendingEditNomeOriginal = participante.nome
            pendingEditTelefoneOriginal = participante.telefone
            pendingEditEmailOriginal = participante.email
            pendingEditParticipante = participante
            pendingEditDialog = dialog
            pendingEditButton = v

            // Aplica valores e delega ao ViewModel (background, evita ANR).
            participante.nome = nome
            participante.telefone = telefone
            participante.email = email
            v.isEnabled = false
            viewModel.atualizarParticipante(participante)
        }
    }

    private fun abrirSeletorContatos() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CONTACT_PICKER)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONTACT_PICKER && resultCode == RESULT_OK && data != null) {
            val contactUri = data.data ?: return
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            try {
                val cursor: Cursor? = contentResolver.query(contactUri, projection, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val name = c.getString(0)
                        val number = c.getString(1)

                        val p = Participante()
                        p.nome = name
                        p.telefone = number
                        viewModel.inserirParticipante(p, grupoAtual.id)
                        Toast.makeText(this, getString(R.string.toast_contact_added_format, name), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "onActivityResult: failed to read contact")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Marca como enviado apenas se o SMS foi efetivamente aberto nesta sessao.
        // launched evita marcacao incorreta apos rotacao (pendingParticipanteId e
        // restaurado do bundle mas o app de SMS nunca foi aberto).
        if (smsState.launched && smsState.pendingParticipanteId != -1) {
            viewModel.marcarComoEnviado(smsState.pendingParticipanteId)
            smsState.pendingParticipanteId = -1
            smsState.launched = false
        }
        // Mesma guarda para WhatsApp — launched nao e salvo no bundle.
        if (whatsAppState.launched && whatsAppState.pendingParticipanteId != -1) {
            viewModel.marcarComoEnviado(whatsAppState.pendingParticipanteId)
            whatsAppState.pendingParticipanteId = -1
            whatsAppState.launched = false
        }
        // Atualizar lista ao voltar para esta activity (ex: depois de adicionar desejos)
        atualizarLista()
        // Retoma sequencia de SMS apos retornar do app de mensagens (evita dialog durante pausa).
        // Apos rotacao, pendingMensagens e null (nao foi salvo no bundle); reconstroi via banco.
        val lista = smsState.pendingList
        if (lista != null && smsState.pendingNextIndex >= 0) {
            val nextIndex = smsState.pendingNextIndex
            smsState.pendingList = null
            smsState.pendingNextIndex = -1

            val mensagens = smsState.pendingMensagens
            if (mensagens != null) {
                // Mensagens ja disponiveis (fluxo normal sem rotacao)
                smsState.pendingMensagens = null
                enviarSmsSequencial(lista, mensagens, nextIndex)
            } else {
                // Mensagens perdidas por rotacao: ViewModel reconstroi a partir do banco.
                // O observer de mensagensSmsResult retoma a partir de pendingResumeIndex.
                smsState.pendingResumeIndex = nextIndex
                viewModel.prepararMensagensSms(lista)
            }
        }
        // Retoma sequencia de WhatsApp — mesma estrategia de reconstrucao que SMS.
        val waLista = whatsAppState.pendingList
        if (waLista != null && whatsAppState.pendingNextIndex >= 0) {
            val nextIndex = whatsAppState.pendingNextIndex
            whatsAppState.pendingList = null
            whatsAppState.pendingNextIndex = -1

            val mensagens = whatsAppState.pendingMensagens
            if (mensagens != null) {
                whatsAppState.pendingMensagens = null
                enviarWhatsAppSequencial(waLista, mensagens, nextIndex)
            } else {
                whatsAppState.pendingResumeIndex = nextIndex
                viewModel.prepararMensagensWhatsApp(waLista)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("pendingSmsId", smsState.pendingParticipanteId)
        outState.putInt("pendingSmsNextIndex", smsState.pendingNextIndex)
        outState.putInt("pendingSmsResumeIndex", smsState.pendingResumeIndex)
        // Salva IDs, índices e lista de participantes (id + telefone + nome).
        // Omite apenas pendingMensagens para evitar TransactionTooLargeException
        // (~1 MB Binder limit) em grupos com listas de desejos longas; as mensagens
        // são reconstruídas a partir do banco no onResume após rotação.
        saveParticipantListToBundle(outState, "pendingSms", smsState.pendingList)
        // Estado do WhatsApp sequencial — mesma estratégia: persiste tudo exceto as mensagens formatadas.
        outState.putInt("pendingWhatsAppId", whatsAppState.pendingParticipanteId)
        outState.putInt("pendingWhatsAppNextIndex", whatsAppState.pendingNextIndex)
        outState.putInt("pendingWhatsAppResumeIndex", whatsAppState.pendingResumeIndex)
        saveParticipantListToBundle(outState, "pendingWhatsApp", whatsAppState.pendingList)
    }

    /**
     * Serializa uma lista de participantes no bundle usando apenas id, telefone e nome —
     * omitindo as mensagens formatadas para evitar TransactionTooLargeException.
     */
    private fun saveParticipantListToBundle(bundle: Bundle, keyPrefix: String, lista: List<Participante>?) {
        if (lista == null) return
        val ids = IntArray(lista.size)
        val telefones = arrayOfNulls<String>(lista.size)
        val nomes = arrayOfNulls<String>(lista.size)
        for (i in lista.indices) {
            ids[i] = lista[i].id
            telefones[i] = lista[i].telefone ?: ""
            nomes[i] = lista[i].nome ?: ""
        }
        bundle.putIntArray("${keyPrefix}Ids", ids)
        bundle.putStringArray("${keyPrefix}Telefones", telefones)
        bundle.putStringArray("${keyPrefix}Nomes", nomes)
    }

    /**
     * Restaura a lista de participantes do bundle (apenas id, telefone e nome).
     * Retorna null se os arrays não estiverem presentes.
     */
    private fun restoreParticipantListFromBundle(bundle: Bundle, keyPrefix: String): List<Participante>? {
        val ids = bundle.getIntArray("${keyPrefix}Ids") ?: return null
        val telefones = bundle.getStringArray("${keyPrefix}Telefones") ?: return null
        val nomes = bundle.getStringArray("${keyPrefix}Nomes") ?: return null
        if (telefones.size != ids.size || nomes.size != ids.size) {
            Timber.e("restoreParticipantListFromBundle: array size mismatch (ids=${ids.size}, telefones=${telefones.size}, nomes=${nomes.size})")
            return null
        }
        return ids.indices.map { i ->
            Participante().also { p ->
                p.id = ids[i]
                p.telefone = telefones[i]
                p.nome = nomes[i]
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirSeletorContatos()
            }
        }
    }

    private fun confirmarLimparTudo() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.participante_limpar_titulo))
            .setMessage(getString(R.string.participante_limpar_msg))
            .setPositiveButton(getString(R.string.participante_limpar_btn_sim)) { _, _ ->
                viewModel.deletarTodosDoGrupo(grupoAtual.id)
            }
            .setNegativeButton(getString(R.string.button_no), null)
            .show()
    }

    private fun realizarSorteio() {
        viewModel.realizarSorteio()
    }

    // Envia SMS abrindo o app de mensagens do dispositivo via Intent (sem permissao SEND_SMS).
    // O usuario confirma e envia um por um — compativel com Play Store sem restricoes.
    // O acesso ao banco e feito pelo ViewModel em background; o resultado chega via LiveData.
    private fun enviarSmsViaIntent() {
        viewModel.prepararMensagensSms(ArrayList(listaParticipantes))
    }

    // Exibe dialog para cada participante antes de abrir o app de SMS, evitando stack de activities.
    private fun enviarSmsSequencial(lista: List<Participante>, mensagensMap: Map<Int, String>, index: Int) {
        if (index >= lista.size) {
            // Conta participantes com mensagem preparada — inclui os que o usuário pulou,
            // pois a API ACTION_SENDTO não oferece callback de confirmação de envio.
            val comMensagem = lista.count { p ->
                val m = mensagensMap[p.id]
                m != null && m.isNotEmpty()
            }
            Toast.makeText(this, getString(R.string.toast_sms_prepared_format, comMensagem), Toast.LENGTH_LONG).show()
            return
        }

        val p = lista[index]
        val mensagem = mensagensMap[p.id]
        if (mensagem == null || mensagem.isEmpty()) {
            // Mensagem ausente ou vazia (estado inconsistente, ex: restaurado do bundle como ""); pular.
            // Handler.post e usado por stack-safety: evita stack overflow em listas longas onde
            // multiplos itens consecutivos sao pulados, convertendo recursao em iteracao no loop de mensagens.
            mainHandler.post { enviarSmsSequencial(lista, mensagensMap, index + 1) }
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_send_sms_title_format, p.nome, index + 1, lista.size))
            .setMessage(getString(R.string.dialog_send_sms_message_format, p.telefone))
            .setPositiveButton(R.string.button_open_sms) { _, _ ->
                // Nao usar Uri.encode: converte '+' de numeros internacionais em '%2B'
                val smsUri = Uri.parse("smsto:${p.telefone}")
                val intent = Intent(Intent.ACTION_SENDTO, smsUri)
                intent.putExtra("sms_body", mensagem)
                try {
                    // Registra o id antes de sair; onResume marca como enviado ao retornar.
                    // Proximo dialog e agendado para onResume para evitar BadTokenException.
                    smsState.pendingParticipanteId = p.id
                    smsState.pendingList = lista
                    smsState.pendingMensagens = mensagensMap
                    smsState.pendingNextIndex = index + 1
                    smsState.launched = true
                    startActivity(intent)
                } catch (e: android.content.ActivityNotFoundException) {
                    smsState.pendingParticipanteId = -1
                    smsState.pendingList = null
                    smsState.pendingMensagens = null
                    smsState.pendingNextIndex = -1
                    Toast.makeText(this, R.string.error_no_sms_app, Toast.LENGTH_SHORT).show()
                    // Postar no Handler evita abrir novo AlertDialog enquanto o atual ainda
                    // esta sendo descartado, prevenindo WindowManager exception.
                    mainHandler.post { enviarSmsSequencial(lista, mensagensMap, index + 1) }
                }
            }
            .setNegativeButton(R.string.button_skip) { _, _ ->
                // Limpa id possivelmente restaurado do bundle para evitar estado inconsistente.
                // Handler.post adia o proximo dialog ate o atual ser descartado (evita race condition).
                smsState.pendingParticipanteId = -1
                mainHandler.post { enviarSmsSequencial(lista, mensagensMap, index + 1) }
            }
            .setNeutralButton(R.string.button_cancel_all) { _, _ ->
                smsState.pendingParticipanteId = -1
                smsState.pendingList = null
                smsState.pendingMensagens = null
                smsState.pendingNextIndex = -1
                smsState.launched = false
            }
            .setCancelable(false)
            .show()
    }

    private fun enviarWhatsAppViaIntent() {
        viewModel.prepararMensagensWhatsApp(ArrayList(listaParticipantes))
    }

    // Exibe dialog para cada participante antes de abrir o WhatsApp via deep link wa.me,
    // evitando stack de activities. Espelha enviarSmsSequencial com URI wa.me no lugar de smsto:.
    private fun enviarWhatsAppSequencial(lista: List<Participante>, mensagensMap: Map<Int, String>, index: Int) {
        if (index >= lista.size) {
            // Conta participantes com mensagem preparada — inclui os que o usuário pulou,
            // pois a API ACTION_VIEW (wa.me) não oferece callback de confirmação de envio.
            val comMensagem = lista.count { p ->
                val m = mensagensMap[p.id]
                m != null && m.isNotEmpty()
            }
            Toast.makeText(this, getString(R.string.toast_whatsapp_prepared_format, comMensagem), Toast.LENGTH_LONG).show()
            return
        }

        val p = lista[index]
        val mensagem = mensagensMap[p.id]
        if (mensagem == null || mensagem.isEmpty()) {
            mainHandler.post { enviarWhatsAppSequencial(lista, mensagensMap, index + 1) }
            return
        }

        // Verifica se o número normalizado pode estar sem código de país (< 12 dígitos).
        // wa.me exige código de país — números locais sem +55 vão falhar ou abrir contato errado.
        val telefoneDisplay = p.telefone ?: ""
        val normalizado = CompartilharHelper.normalizePhoneNumber(telefoneDisplay)
        val aviso = if (normalizado.isNotEmpty() && normalizado.length < 12) {
            getString(R.string.warning_phone_no_country_code)
        } else ""
        val msgDialog = getString(R.string.dialog_send_whatsapp_message_format, telefoneDisplay) + aviso

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_send_whatsapp_title_format, p.nome, index + 1, lista.size))
            .setMessage(msgDialog)
            .setPositiveButton(R.string.button_open_whatsapp) { _, _ ->
                // Registra o estado antes de sair — onResume marca como enviado ao retornar.
                // Estado atualizado antes de startActivity para que, em caso de crash ou
                // interrupção dentro do helper, o onResume ainda consiga retomar corretamente.
                whatsAppState.pendingParticipanteId = p.id
                whatsAppState.pendingList = lista
                whatsAppState.pendingMensagens = mensagensMap
                whatsAppState.pendingNextIndex = index + 1
                whatsAppState.launched = true
                CompartilharHelper.compartilharWhatsAppComTelefone(this, telefoneDisplay, mensagem, getString(R.string.button_open_whatsapp))
            }
            .setNegativeButton(R.string.button_skip) { _, _ ->
                whatsAppState.pendingParticipanteId = -1
                mainHandler.post { enviarWhatsAppSequencial(lista, mensagensMap, index + 1) }
            }
            .setNeutralButton(R.string.button_cancel_all) { _, _ ->
                whatsAppState.pendingParticipanteId = -1
                whatsAppState.pendingList = null
                whatsAppState.pendingMensagens = null
                whatsAppState.pendingNextIndex = -1
            }
            .setCancelable(false)
            .show()
    }

    private fun exibirDialogRegras(p: Participante) {
        val outros = listaParticipantes.filter { it.id != p.id }
        if (outros.isEmpty()) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_regras, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tv_dialog_subtitle)
        val layoutLista = dialogView.findViewById<android.widget.LinearLayout>(R.id.layout_lista_participantes)

        tvTitle.text = getString(R.string.dialog_restrictions_title_format, p.nome)
        tvSubtitle.setText(R.string.dialog_restrictions_subtitle)

        // Criar checkboxes para cada participante
        val selecionados = BooleanArray(outros.size) { i -> p.idsExcluidos.contains(outros[i].id) }
        for (i in outros.indices) {
            val outro = outros[i]
            val itemView = layoutInflater.inflate(R.layout.item_regra_checkbox, layoutLista, false)
            val tvAvatar = itemView.findViewById<TextView>(R.id.tv_avatar_regra)
            val tvNome = itemView.findViewById<TextView>(R.id.tv_nome_regra)
            val checkbox = itemView.findViewById<MaterialCheckBox>(R.id.checkbox_regra)

            tvAvatar.text = outro.nome?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            tvNome.text = outro.nome
            checkbox.isChecked = selecionados[i]

            itemView.setOnClickListener {
                checkbox.isChecked = !checkbox.isChecked
                selecionados[i] = checkbox.isChecked
            }

            checkbox.setOnCheckedChangeListener { _, isChecked ->
                selecionados[i] = isChecked
            }

            layoutLista.addView(itemView)
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.button_save) { _, _ ->
                val adicionar = mutableListOf<Int>()
                val remover = mutableListOf<Int>()
                for (i in outros.indices) {
                    if (selecionados[i]) adicionar.add(outros[i].id)
                    else remover.add(outros[i].id)
                }
                viewModel.salvarExclusoes(p.id, adicionar, remover)
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_participantes, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_historico -> {
                startActivity(
                    Intent(this, HistoricoSorteiosActivity::class.java)
                        .putExtra(Grupo.EXTRA_GRUPO, grupoAtual)
                )
                true
            }
            R.id.action_dashboard -> {
                startActivity(
                    Intent(this, DashboardActivity::class.java)
                        .putExtra(Grupo.EXTRA_GRUPO, grupoAtual)
                )
                true
            }
            R.id.action_configuracoes_grupo -> {
                configuracoesGrupoLauncher.launch(
                    Intent(this, ConfiguracoesGrupoActivity::class.java)
                        .putExtra(Grupo.EXTRA_GRUPO, grupoAtual)
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private inner class ParticipantesAdapter(
        private val ctx: Context,
        private val itens: List<Participante>
    ) : BaseAdapter() {

        private var desejosCountMap = mapOf<Int, Int>()

        fun setDesejosCountMap(map: Map<Int, Int>) {
            desejosCountMap = map
        }

        override fun getCount() = itens.size
        override fun getItem(position: Int) = itens[position]
        override fun getItemId(position: Int) = itens[position].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(ctx).inflate(R.layout.item_participante, parent, false)

            val p = itens[position]
            val tvNumero = view.findViewById<TextView>(R.id.tv_numero)
            val tvAvatar = view.findViewById<TextView>(R.id.tv_avatar)
            val tvNome = view.findViewById<TextView>(R.id.tv_nome)
            val tvEmail = view.findViewById<TextView>(R.id.tv_email)
            val tvDesejosCount = view.findViewById<TextView>(R.id.tv_desejos_count)
            val btnDesejos = view.findViewById<ImageButton>(R.id.btn_desejos)
            val btnRegras = view.findViewById<ImageButton>(R.id.btn_regras)
            val btnShare = view.findViewById<ImageButton>(R.id.btn_share)
            val btnQrCode = view.findViewById<ImageButton>(R.id.btn_qrcode)
            val btnEditar = view.findViewById<ImageButton>(R.id.btn_editar)
            val btnRemover = view.findViewById<ImageButton>(R.id.btn_remover)

            tvNumero.text = (position + 1).toString()
            tvAvatar.text = p.nome?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            tvNome.text = p.nome

            // Obter count de desejos do map pré-carregado
            val countDesejos = desejosCountMap[p.id] ?: 0

            if (countDesejos > 0) {
                tvDesejosCount.text = ctx.resources.getQuantityString(R.plurals.label_wishes_count, countDesejos, countDesejos)
                tvDesejosCount.visibility = View.VISIBLE
            } else {
                tvDesejosCount.visibility = View.GONE
            }

            val amigoId = p.amigoSorteadoId
            if (amigoId != null && amigoId > 0) {
                tvEmail.text = if (p.isEnviado) getString(R.string.status_result_sent) else getString(R.string.status_ready_share)
                tvEmail.setTextColor(ContextCompat.getColor(ctx, if (p.isEnviado) R.color.text_secondary else R.color.colorAccent))
                btnShare.visibility = View.VISIBLE
                btnQrCode.visibility = View.VISIBLE
            } else {
                tvEmail.text = if (p.idsExcluidos.isEmpty())
                    getString(R.string.status_no_restrictions)
                else
                    ctx.resources.getQuantityString(R.plurals.label_restrictions_count, p.idsExcluidos.size, p.idsExcluidos.size)
                tvEmail.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                btnShare.visibility = View.GONE
                btnQrCode.visibility = View.GONE
            }

            btnDesejos.setOnClickListener {
                val intent = Intent(ctx, ParticipanteDesejosActivity::class.java)
                intent.putExtra("participante", p)
                ctx.startActivity(intent)
            }

            btnRegras.setOnClickListener { exibirDialogRegras(p) }

            btnShare.setOnClickListener { v ->
                // Desabilitar imediatamente para evitar multiplos taps que disparariam
                // requisicoes duplicadas. Reabilitado quando notifyDataSetChanged() recria
                // as views apos atualizarLista() no observer de mensagemCompartilhamento.
                v.isEnabled = false
                compartilharResultado(p)
            }

            btnQrCode.setOnClickListener {
                abrirQrCode(p)
            }

            btnEditar.setOnClickListener { exibirDialogEditar(p) }

            btnRemover.setOnClickListener { viewModel.removerParticipante(p.id) }

            return view
        }

        private fun compartilharResultado(p: Participante) {
            // ViewModel prepara mensagem em background; resultado chega via observer de
            // mensagemCompartilhamentoResult em onCreate(). atualizarLista() no observer
            // dispara notifyDataSetChanged(), que recria as views e reabilita o botão.
            viewModel.prepararMensagemCompartilhamento(p)
        }

        private fun abrirQrCode(p: Participante) {
            // Delega ao ViewModel para buscar o nome do amigo em background.
            // O resultado chega via observer de qrCodeResult em onCreate().
            viewModel.obterNomeAmigoParaQr(p)
        }
    }
}
