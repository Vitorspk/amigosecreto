package activity.amigosecreto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import timber.log.Timber
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import activity.amigosecreto.adapter.GruposRecyclerAdapter
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.util.LembreteScheduler
import activity.amigosecreto.util.StateViewHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date

@AndroidEntryPoint
class GruposActivity : AppCompatActivity(), GruposRecyclerAdapter.OnGrupoActionListener {

    private companion object {
        const val BACKUP_MIME_TYPE = "application/json"
        const val PREFS_NAME = "grupos_prefs"
        const val PREF_SORT_ORDER = "sort_order"
    }

    private val viewModel: GruposViewModel by viewModels()

    private lateinit var rvGrupos: RecyclerView
    private lateinit var btnCriarGrupo: MaterialButton
    private lateinit var adapter: GruposRecyclerAdapter
    private lateinit var stateHelper: StateViewHelper

    // Estado corrente da ordenação (lido de SharedPreferences na inicialização).
    private var sortOrder: Int = GruposViewModel.SORT_CRIACAO

    // Referência ao dialog de edição em andamento — usada pelo observer de operacaoSucesso.
    private var pendingEditDialog: AlertDialog? = null
    private var pendingEditGrupo: Grupo? = null
    private var pendingEditNomeOriginal: String? = null
    private var pendingEditButton: View? = null

    // Arrays de emojis e gradientes para variar os cards
    private val emojis = arrayOf("🎅", "🏝️", "🎄", "🎉", "🎊", "🎁", "🎈", "🌟", "💝", "🎂")
    private val gradientes = intArrayOf(
        R.drawable.card_gradient_orange,
        R.drawable.card_gradient_blue,
        R.drawable.card_gradient_green
    )

    private val solicitarPermissaoNotificacao = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) LembreteScheduler.agendar(this)
    }

    private val exportarLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri -> uri?.let { viewModel.exportarBackup(); pendingExportUri = it } }

    private val importarLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { confirmarEImportarBackup(it) } }

    // URI aguardando o resultado de exportarBackup — preenchido pelo ActivityResultCallback.
    private var pendingExportUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Onboarding gate — show onboarding on first launch then return
        if (!OnboardingActivity.isOnboardingConcluido(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_listar_grupos)

        sortOrder = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(PREF_SORT_ORDER, GruposViewModel.SORT_CRIACAO)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_grupos)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        rvGrupos = findViewById(R.id.rv_grupos)
        btnCriarGrupo = findViewById(R.id.btn_criar_grupo)

        stateHelper = StateViewHelper(
            stubLoading = findViewById(R.id.stub_loading),
            stubEmpty = findViewById(R.id.stub_empty),
            contentView = rvGrupos
        )

        adapter = GruposRecyclerAdapter(this, emojis, gradientes, this)
        rvGrupos.layoutManager = LinearLayoutManager(this)
        rvGrupos.adapter = adapter

        btnCriarGrupo.setOnClickListener { exibirDialogAdd() }

        observeViewModel()

        viewModel.carregarGrupos(sortOrder)
        agendarLembreteSePermitido()
    }

    override fun onResume() {
        super.onResume()
        viewModel.carregarGrupos(sortOrder)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
        ) {
            LembreteScheduler.agendar(this)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            if (loading) stateHelper.showLoading()
        }

        viewModel.grupos.observe(this) { lista ->
            adapter.setItens(lista)
            if (lista.isEmpty()) stateHelper.showEmpty() else stateHelper.showContent()
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (msg == null) return@observe
            viewModel.clearErrorMessage()
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }

        viewModel.operacaoSucesso.observe(this) { sucesso ->
            if (sucesso == null) return@observe
            viewModel.clearOperacaoSucesso()
            pendingEditButton?.isEnabled = true
            if (sucesso == true) {
                pendingEditDialog?.dismiss()
            } else {
                // Restaura nome original no modelo se a atualização falhou
                pendingEditGrupo?.nome = pendingEditNomeOriginal ?: pendingEditGrupo?.nome
            }
            pendingEditDialog = null
            pendingEditGrupo = null
            pendingEditNomeOriginal = null
            pendingEditButton = null
        }

        viewModel.exportarResultado.observe(this) { json ->
            if (json == null) return@observe
            viewModel.clearExportarResultado()
            val uri = pendingExportUri ?: return@observe
            pendingExportUri = null
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                Toast.makeText(this, R.string.backup_exportado_sucesso, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Timber.e(e, "escreverBackupNoUri: falha ao escrever")
                Toast.makeText(this, R.string.backup_erro_exportar, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.importarResultado.observe(this) { resultado ->
            if (resultado == null) return@observe
            viewModel.clearImportarResultado()
            when (resultado) {
                is GruposViewModel.ImportarResultado.Sucesso ->
                    Toast.makeText(this,
                        getString(R.string.backup_importado_sucesso, resultado.gruposImportados),
                        Toast.LENGTH_LONG).show()
                is GruposViewModel.ImportarResultado.Falha ->
                    Toast.makeText(this, R.string.backup_erro_importar, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun agendarLembreteSePermitido() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED -> LembreteScheduler.agendar(this)
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Usuário já negou antes — não insistimos; o app funciona sem notificações
                }
                else -> solicitarPermissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            LembreteScheduler.agendar(this)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_grupos, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == R.id.action_more) {
            exibirMenuMais()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun exibirMenuMais() {
        val popup = PopupMenu(this, findViewById(R.id.action_more))
        popup.menuInflater.inflate(R.menu.menu_mais_opcoes, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sobre -> { exibirSobre(); true }
                R.id.action_compartilhar -> { compartilharApp(); true }
                R.id.action_avaliar -> { abrirPlayStore(); true }
                R.id.action_limpar_dados -> { confirmarLimparTodosDados(); true }
                R.id.action_exportar_backup -> { iniciarExportacao(); true }
                R.id.action_importar_backup -> { iniciarImportacao(); true }
                R.id.action_estatisticas -> { startActivity(Intent(this, EstatisticasActivity::class.java)); true }
                R.id.action_ordenar_grupos -> { exibirDialogOrdenacao(); true }
                else -> false
            }
        }

        popup.show()
    }

    private fun exibirSobre() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_about_title)
            .setMessage(R.string.dialog_about_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun compartilharApp() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app_subject))
        intent.putExtra(
            Intent.EXTRA_TEXT,
            getString(R.string.share_app_body) + "\n\n" +
                "https://play.google.com/store/apps/details?id=$packageName"
        )
        startActivity(Intent.createChooser(intent, getString(R.string.action_share_app)))
    }

    private fun abrirPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName")))
        } catch (e: android.content.ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun confirmarLimparTodosDados() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_clear_all_title)
            .setMessage(R.string.dialog_clear_all_message)
            .setPositiveButton(R.string.button_clear_all_yes) { _, _ ->
                viewModel.limparTudo()
                Toast.makeText(this, R.string.toast_all_data_cleared, Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun iniciarExportacao() {
        val dataHora = SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(Date())
        Toast.makeText(this, R.string.backup_exportando, Toast.LENGTH_SHORT).show()
        exportarLauncher.launch("amigosecreto_backup_$dataHora.json")
    }

    private fun iniciarImportacao() {
        importarLauncher.launch(arrayOf(BACKUP_MIME_TYPE))
    }

    private fun confirmarEImportarBackup(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(R.string.backup_confirmar_titulo)
            .setMessage(R.string.backup_confirmar_mensagem)
            .setPositiveButton(R.string.backup_confirmar_sim) { _, _ -> lerEImportarBackup(uri) }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun lerEImportarBackup(uri: Uri) {
        Toast.makeText(this, R.string.backup_importando, Toast.LENGTH_SHORT).show()
        val json = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            Timber.e(e, "lerEImportarBackup: failed to read URI")
            null
        }
        if (json == null) {
            Toast.makeText(this, R.string.backup_erro_importar, Toast.LENGTH_LONG).show()
            return
        }
        viewModel.importarBackup(json)
    }

    private fun exibirDialogOrdenacao() {
        val opcoes = arrayOf(
            getString(R.string.ordenar_criacao),
            getString(R.string.ordenar_nome),
            getString(R.string.ordenar_evento)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_ordenar_titulo)
            .setSingleChoiceItems(opcoes, sortOrder) { dialog, which ->
                sortOrder = which
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putInt(PREF_SORT_ORDER, sortOrder).apply()
                dialog.dismiss()
                viewModel.carregarGrupos(sortOrder)
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun exibirDialogAdd() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_criar_grupo, null)

        val etNome = dialogView.findViewById<TextInputEditText>(R.id.et_nome_grupo)
        val btnCriar = dialogView.findViewById<MaterialButton>(R.id.btn_criar)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btn_cancelar)

        val chipFamilia = dialogView.findViewById<Chip>(R.id.chip_familia)
        val chipTrabalho = dialogView.findViewById<Chip>(R.id.chip_trabalho)
        val chipAmigos = dialogView.findViewById<Chip>(R.id.chip_amigos)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        chipFamilia.setOnClickListener { etNome.setText(getString(R.string.chip_sugestao_familia)) }
        chipTrabalho.setOnClickListener { etNome.setText(getString(R.string.chip_sugestao_trabalho)) }
        chipAmigos.setOnClickListener { etNome.setText(getString(R.string.chip_sugestao_amigos)) }

        btnCriar.setOnClickListener {
            val nome = etNome.text?.toString()?.trim() ?: ""
            if (nome.isNotEmpty()) {
                val data = activity.amigosecreto.util.FormatUtils.formatDate(Date(), getString(R.string.date_format_short))
                viewModel.inserirGrupo(nome, data)
                dialog.dismiss()
            } else {
                Toast.makeText(this, R.string.grupo_erro_nome_obrigatorio, Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // --- GruposRecyclerAdapter.OnGrupoActionListener ---

    override fun onGrupoClick(grupo: Grupo) {
        startActivity(
            Intent(this, ParticipantesActivity::class.java)
                .putExtra(Grupo.EXTRA_GRUPO, grupo)
        )
    }

    override fun onEditarNome(grupo: Grupo, novoNome: String, dialog: AlertDialog, button: View) {
        // Cria cópia com o novo nome — não muta o objeto no adapter
        // para que DiffUtil possa detectar a mudança ao comparar com o resultado do DB.
        val grupoAtualizado = Grupo(
            id = grupo.id, nome = novoNome, data = grupo.data, descricao = grupo.descricao,
            dataEvento = grupo.dataEvento, localEvento = grupo.localEvento,
            dataLimiteSorteio = grupo.dataLimiteSorteio, valorMinimo = grupo.valorMinimo,
            valorMaximo = grupo.valorMaximo, regras = grupo.regras,
            permitirVerDesejos = grupo.permitirVerDesejos,
            exigirConfirmacaoCompra = grupo.exigirConfirmacaoCompra,
        )
        pendingEditNomeOriginal = grupo.nome
        pendingEditGrupo = grupoAtualizado
        pendingEditDialog = dialog
        pendingEditButton = button
        button.isEnabled = false
        viewModel.atualizarNomeGrupo(grupoAtualizado)
    }

    override fun onRemover(grupo: Grupo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.grupo_dialog_excluir_titulo)
            .setMessage(getString(R.string.grupo_dialog_excluir_mensagem, grupo.nome))
            .setPositiveButton(R.string.button_remove_yes) { _, _ -> viewModel.removerGrupo(grupo) }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }
}
