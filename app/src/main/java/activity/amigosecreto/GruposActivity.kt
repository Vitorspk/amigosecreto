package activity.amigosecreto

import android.Manifest
import android.content.Context
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.ParticipanteDAO
import activity.amigosecreto.repository.BackupRepository
import activity.amigosecreto.util.AsyncDatabaseHelper
import activity.amigosecreto.util.BackupManager
import activity.amigosecreto.util.HapticFeedbackUtils
import activity.amigosecreto.util.LembreteScheduler
import activity.amigosecreto.util.StateViewHelper
import activity.amigosecreto.util.WindowInsetsUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GruposActivity : AppCompatActivity() {

    private companion object {
        const val TAG = "GruposActivity"
        const val MENU_EDITAR = 1
        const val MENU_EXCLUIR = 2
        const val BACKUP_MIME_TYPE = "application/json"
    }

    private lateinit var rvGrupos: RecyclerView
    private lateinit var btnCriarGrupo: MaterialButton
    private lateinit var dao: GrupoDAO
    private lateinit var participanteDao: ParticipanteDAO
    private lateinit var backupRepository: BackupRepository
    private val listaGrupos = mutableListOf<Grupo>()
    private lateinit var adapter: GruposRecyclerAdapter
    private lateinit var stateHelper: StateViewHelper

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
    ) { uri -> uri?.let { escreverBackupNoUri(it) } }

    private val importarLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { confirmarEImportarBackup(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listar_grupos)

        // Configurar toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar_grupos)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        dao = GrupoDAO(this)
        participanteDao = ParticipanteDAO(this)
        backupRepository = BackupRepository(this)
        rvGrupos = findViewById(R.id.rv_grupos)
        btnCriarGrupo = findViewById(R.id.btn_criar_grupo)

        stateHelper = StateViewHelper(
            stubLoading = findViewById(R.id.stub_loading),
            stubEmpty = findViewById(R.id.stub_empty),
            contentView = rvGrupos
        )

        adapter = GruposRecyclerAdapter(this, listaGrupos, emojis, gradientes)
        rvGrupos.layoutManager = LinearLayoutManager(this)
        rvGrupos.adapter = adapter

        btnCriarGrupo.setOnClickListener { exibirDialogAdd() }

        atualizarLista()
        // Solicitação de permissão e agendamento apenas em onCreate — evita dialog repetido em onResume
        agendarLembreteSePermitido()
    }

    override fun onResume() {
        super.onResume()
        // Atualizar a lista sempre que voltar para esta tela
        atualizarLista()
        // Re-agendamento idempotente (KEEP policy) — retoma lembrete se Worker se auto-cancelou
        // quando não havia grupos pendentes e novos grupos foram criados.
        // Verifica permissão antes de agendar para não gerar trabalho desnecessário em Android 13+
        // quando o usuário negou POST_NOTIFICATIONS.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
        ) {
            LembreteScheduler.agendar(this)
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
                AsyncDatabaseHelper.executeSimple(
                    {
                        dao.open()
                        dao.limparTudo()
                        dao.close()
                    },
                    {
                        atualizarLista()
                        Toast.makeText(this, R.string.toast_all_data_cleared, Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun iniciarExportacao() {
        val dataHora = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        exportarLauncher.launch("amigosecreto_backup_$dataHora.json")
    }

    private fun escreverBackupNoUri(uri: Uri) {
        Toast.makeText(this, R.string.backup_exportando, Toast.LENGTH_SHORT).show()
        AsyncDatabaseHelper.execute(
            object : AsyncDatabaseHelper.BackgroundTask<String> {
                override fun doInBackground(): String = backupRepository.exportar()
            },
            object : AsyncDatabaseHelper.ResultCallback<String> {
                override fun onSuccess(result: String) {
                    try {
                        contentResolver.openOutputStream(uri)?.use { it.write(result.toByteArray()) }
                        Toast.makeText(this@GruposActivity, R.string.backup_exportado_sucesso, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Timber.e(e, "escreverBackupNoUri: falha ao escrever")
                        Toast.makeText(this@GruposActivity, R.string.backup_erro_exportar, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onError(e: Exception) {
                    Timber.e(e, "escreverBackupNoUri: erro no background")
                    Toast.makeText(this@GruposActivity, R.string.backup_erro_exportar, Toast.LENGTH_LONG).show()
                }
            }
        )
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
        AsyncDatabaseHelper.execute(
            object : AsyncDatabaseHelper.BackgroundTask<BackupManager.ImportResult> {
                override fun doInBackground(): BackupManager.ImportResult {
                    val json = contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: return BackupManager.ImportResult.Failure("Não foi possível ler o arquivo")
                    return backupRepository.importar(json)
                }
            },
            object : AsyncDatabaseHelper.ResultCallback<BackupManager.ImportResult> {
                override fun onSuccess(result: BackupManager.ImportResult) {
                    when (result) {
                        is BackupManager.ImportResult.Success -> {
                            atualizarLista()
                            Toast.makeText(this@GruposActivity,
                                getString(R.string.backup_importado_sucesso, result.gruposImportados),
                                Toast.LENGTH_LONG).show()
                        }
                        is BackupManager.ImportResult.Failure -> {
                            Toast.makeText(this@GruposActivity, R.string.backup_erro_importar, Toast.LENGTH_LONG).show()
                            Timber.e("lerEImportarBackup: ${result.reason}")
                        }
                    }
                }
                override fun onError(e: Exception) {
                    Timber.e(e, "lerEImportarBackup: erro no background")
                    Toast.makeText(this@GruposActivity, R.string.backup_erro_importar, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun atualizarLista() {
        stateHelper.showLoading()
        AsyncDatabaseHelper.execute(
            object : AsyncDatabaseHelper.BackgroundTask<List<Grupo>> {
                override fun doInBackground(): List<Grupo> {
                    dao.open()
                    return try { dao.listar() } finally { dao.close() }
                }
            },
            object : AsyncDatabaseHelper.ResultCallback<List<Grupo>> {
                @Suppress("NotifyDataSetChanged")
                override fun onSuccess(result: List<Grupo>) {
                    listaGrupos.clear()
                    listaGrupos.addAll(result)
                    if (listaGrupos.isEmpty()) stateHelper.showEmpty() else stateHelper.showContent()
                    // Primeiro notify: exibe nomes imediatamente com contagens em "0".
                    // recarregarContagensAsync fará um segundo notify com as contagens reais.
                    // TODO: unificar em uma única query para evitar o double-bind.
                    adapter.notifyDataSetChanged()
                    adapter.recarregarContagensAsync()
                }
                override fun onError(e: Exception) {
                    Timber.e(e, "atualizarLista: failed")
                    Toast.makeText(this@GruposActivity, R.string.error_load_groups, Toast.LENGTH_LONG).show()
                    stateHelper.showEmpty()
                }
            }
        )
    }

    private fun exibirDialogAdd() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_criar_grupo, null)

        val etNome = dialogView.findViewById<TextInputEditText>(R.id.et_nome_grupo)
        val btnCriar = dialogView.findViewById<MaterialButton>(R.id.btn_criar)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btn_cancelar)

        // Chips de sugestões
        val chipFamilia = dialogView.findViewById<Chip>(R.id.chip_familia)
        val chipTrabalho = dialogView.findViewById<Chip>(R.id.chip_trabalho)
        val chipAmigos = dialogView.findViewById<Chip>(R.id.chip_amigos)

        // Criar dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Tornar o fundo transparente para mostrar os cantos arredondados
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Listeners dos chips
        chipFamilia.setOnClickListener { etNome.setText(getString(R.string.chip_sugestao_familia)) }
        chipTrabalho.setOnClickListener { etNome.setText(getString(R.string.chip_sugestao_trabalho)) }
        chipAmigos.setOnClickListener { etNome.setText(getString(R.string.chip_sugestao_amigos)) }

        // Botão criar
        btnCriar.setOnClickListener {
            val nome = etNome.text?.toString()?.trim() ?: ""
            if (nome.isNotEmpty()) {
                val g = Grupo()
                g.nome = nome
                g.data = SimpleDateFormat("dd/MM/yyyy", WindowInsetsUtils.LOCALE_PT_BR).format(Date())
                dao.open()
                dao.inserir(g)
                dao.close()
                atualizarLista()
                dialog.dismiss()
            } else {
                Toast.makeText(this, R.string.grupo_erro_nome_obrigatorio, Toast.LENGTH_SHORT).show()
            }
        }

        // Botão cancelar
        btnCancelar.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // TODO: extract to adapter/ package when DAO/dialog coupling is reduced
    @Suppress("NotifyDataSetChanged")
    private inner class GruposRecyclerAdapter(
        private val ctx: Context,
        private val itens: MutableList<Grupo>,
        private val emojis: Array<String>,
        private val gradientes: IntArray
    ) : RecyclerView.Adapter<GruposRecyclerAdapter.ViewHolder>() {

        private val contagemParticipantes = mutableMapOf<Int, Int>()

        fun recarregarContagensAsync() {
            AsyncDatabaseHelper.execute(
                object : AsyncDatabaseHelper.BackgroundTask<Map<Int, Int>> {
                    override fun doInBackground(): Map<Int, Int> {
                        participanteDao.open()
                        val mapa = participanteDao.contarPorGrupo()
                        participanteDao.close()
                        return mapa
                    }
                },
                object : AsyncDatabaseHelper.ResultCallback<Map<Int, Int>> {
                    override fun onSuccess(mapa: Map<Int, Int>) {
                        contagemParticipantes.clear()
                        contagemParticipantes.putAll(mapa)
                        notifyDataSetChanged()
                    }

                    override fun onError(e: Exception) {
                        Timber.e(e, "Erro ao carregar contagem de participantes")
                    }
                }
            )
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNome: TextView = view.findViewById(R.id.tv_grupo_nome)
            val tvParticipantes: TextView = view.findViewById(R.id.tv_grupo_participantes)
            val tvEmoji: TextView = view.findViewById(R.id.tv_grupo_emoji)
            val layoutContent: LinearLayout = view.findViewById(R.id.layout_grupo_content)

            init {
                itemView.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                    val g = itens[pos]
                    val intent = Intent(this@GruposActivity, ParticipantesActivity::class.java)
                    intent.putExtra("grupo", g)
                    startActivity(intent)
                }
                itemView.setOnLongClickListener { v ->
                    val pos = bindingAdapterPosition
                    if (pos == RecyclerView.NO_POSITION) return@setOnLongClickListener false
                    val g = itens[pos]
                    HapticFeedbackUtils.performMediumFeedback(v)
                    exibirMenuContextoGrupo(v, g)
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(ctx).inflate(R.layout.item_grupo, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount() = itens.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // Use bindingAdapterPosition for consistency with ViewHolder.init click handlers —
            // position parameter can be stale if partial-update notifications are introduced later.
            val pos = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
            val g = itens[pos]

            holder.tvNome.text = g.nome
            // Use g.id (stable) instead of position so emoji/gradient don't shift on delete/reorder.
            holder.tvEmoji.text = emojis[g.id % emojis.size]
            holder.layoutContent.setBackgroundResource(gradientes[g.id % gradientes.size])

            val numParticipantes = contagemParticipantes[g.id] ?: 0
            holder.tvParticipantes.text = ctx.resources.getQuantityString(
                R.plurals.label_participants, numParticipantes, numParticipantes
            )
        }

        private fun exibirMenuContextoGrupo(anchorView: View, g: Grupo) {
            val popup = PopupMenu(ctx, anchorView)
            popup.menu.add(0, MENU_EDITAR, 0, ctx.getString(R.string.grupo_menu_editar_nome))
            popup.menu.add(0, MENU_EXCLUIR, 1, ctx.getString(R.string.grupo_menu_excluir))

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_EDITAR -> { exibirDialogEditarNome(g); true }
                    MENU_EXCLUIR -> { confirmarRemoverGrupo(g); true }
                    else -> false
                }
            }

            popup.show()
        }

        private fun exibirDialogEditarNome(g: Grupo) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_criar_grupo, null)

            val etNome = dialogView.findViewById<TextInputEditText>(R.id.et_nome_grupo)
            val btnCriar = dialogView.findViewById<MaterialButton>(R.id.btn_criar)
            val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btn_cancelar)

            // Ocultar chips de sugestões no modo edição
            dialogView.findViewById<View>(R.id.chip_group_sugestoes)?.visibility = View.GONE

            etNome.setText(g.nome)
            etNome.setSelection(etNome.text?.length ?: 0)
            btnCriar.setText(R.string.button_save)

            val dialog = AlertDialog.Builder(ctx)
                .setView(dialogView)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnCriar.setOnClickListener {
                val novoNome = etNome.text?.toString()?.trim() ?: ""
                if (novoNome.isEmpty()) {
                    Toast.makeText(this@GruposActivity, R.string.grupo_erro_nome_obrigatorio, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val nomeOriginal = g.nome
                g.nome = novoNome
                btnCriar.isEnabled = false
                AsyncDatabaseHelper.execute(
                    object : AsyncDatabaseHelper.BackgroundTask<Int> {
                        override fun doInBackground(): Int {
                            dao.open()
                            val rows = dao.atualizarNome(g)
                            dao.close()
                            return rows
                        }
                    },
                    object : AsyncDatabaseHelper.ResultCallback<Int> {
                        override fun onSuccess(rows: Int) {
                            if (rows > 0) {
                                atualizarLista()
                                dialog.dismiss()
                            } else {
                                g.nome = nomeOriginal
                                btnCriar.isEnabled = true
                                Toast.makeText(this@GruposActivity, R.string.grupo_erro_salvar, Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onError(e: Exception) {
                            g.nome = nomeOriginal
                            notifyDataSetChanged()
                            btnCriar.isEnabled = true
                            Timber.e(e, "Erro ao atualizar nome do grupo")
                            Toast.makeText(this@GruposActivity, R.string.grupo_erro_salvar, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            btnCancelar.setOnClickListener { dialog.dismiss() }

            dialog.show()
        }

        private fun confirmarRemoverGrupo(g: Grupo) {
            AlertDialog.Builder(ctx)
                .setTitle(R.string.grupo_dialog_excluir_titulo)
                .setMessage(ctx.getString(R.string.grupo_dialog_excluir_mensagem, g.nome))
                .setPositiveButton(R.string.button_remove_yes) { _, _ ->
                    AsyncDatabaseHelper.executeSimple(
                        {
                            dao.open()
                            dao.remover(g.id)
                            dao.close()
                        },
                        { atualizarLista() }
                    )
                }
                .setNegativeButton(R.string.button_cancel, null)
                .show()
        }
    }
}
