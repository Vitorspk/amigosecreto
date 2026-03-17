package activity.amigosecreto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.GrupoDAO
import activity.amigosecreto.db.ParticipanteDAO
import activity.amigosecreto.util.AsyncDatabaseHelper
import activity.amigosecreto.util.HapticFeedbackUtils
import activity.amigosecreto.util.StateViewHelper
import activity.amigosecreto.util.WindowInsetsUtils
import java.text.SimpleDateFormat
import java.util.Date

class GruposActivity : AppCompatActivity() {

    private companion object {
        const val TAG = "GruposActivity"
        const val MENU_EDITAR = 1
        const val MENU_EXCLUIR = 2
    }

    private lateinit var lvGrupos: android.widget.ListView
    private lateinit var btnCriarGrupo: MaterialButton
    private lateinit var dao: GrupoDAO
    private lateinit var participanteDao: ParticipanteDAO
    private val listaGrupos = mutableListOf<Grupo>()
    private lateinit var adapter: GruposAdapter
    private lateinit var stateHelper: StateViewHelper

    // Arrays de emojis e gradientes para variar os cards
    private val emojis = arrayOf("🎅", "🏝️", "🎄", "🎉", "🎊", "🎁", "🎈", "🌟", "💝", "🎂")
    private val gradientes = intArrayOf(
        R.drawable.card_gradient_orange,
        R.drawable.card_gradient_blue,
        R.drawable.card_gradient_green
    )

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
        lvGrupos = findViewById(R.id.lv_grupos)
        btnCriarGrupo = findViewById(R.id.btn_criar_grupo)

        stateHelper = StateViewHelper(
            stubLoading = findViewById(R.id.stub_loading),
            stubEmpty = findViewById(R.id.stub_empty),
            contentView = lvGrupos
        )

        adapter = GruposAdapter(this, listaGrupos)
        lvGrupos.adapter = adapter

        btnCriarGrupo.setOnClickListener { exibirDialogAdd() }

        atualizarLista()
    }

    override fun onResume() {
        super.onResume()
        // Atualizar a lista sempre que voltar para esta tela
        atualizarLista()
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

    private fun atualizarLista() {
        stateHelper.showLoading()
        AsyncDatabaseHelper.execute(
            object : AsyncDatabaseHelper.BackgroundTask<List<activity.amigosecreto.db.Grupo>> {
                override fun doInBackground(): List<activity.amigosecreto.db.Grupo> {
                    dao.open()
                    return try { dao.listar() } finally { dao.close() }
                }
            },
            object : AsyncDatabaseHelper.ResultCallback<List<activity.amigosecreto.db.Grupo>> {
                override fun onSuccess(result: List<activity.amigosecreto.db.Grupo>) {
                    listaGrupos.clear()
                    listaGrupos.addAll(result)
                    if (listaGrupos.isEmpty()) stateHelper.showEmpty() else stateHelper.showContent()
                    adapter.notifyDataSetChanged()
                    adapter.recarregarContagensAsync()
                }
                override fun onError(e: Exception) {
                    Log.e(TAG, "atualizarLista: failed", e)
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

    private inner class GruposAdapter(
        private val ctx: Context,
        private val itens: MutableList<Grupo>
    ) : BaseAdapter() {

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
                        Log.e(TAG, "Erro ao carregar contagem de participantes", e)
                    }
                }
            )
        }

        override fun getCount() = itens.size
        override fun getItem(position: Int) = itens[position]
        override fun getItemId(position: Int) = itens[position].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(ctx).inflate(R.layout.item_grupo_modern, parent, false)

            val g = itens[position]
            val tvNome = view.findViewById<TextView>(R.id.tv_grupo_nome)
            val tvParticipantes = view.findViewById<TextView>(R.id.tv_grupo_participantes)
            val tvEmoji = view.findViewById<TextView>(R.id.tv_grupo_emoji)
            val layoutContent = view.findViewById<LinearLayout>(R.id.layout_grupo_content)

            tvNome.text = g.nome

            // Set participantes count - usar contagem pré-carregada
            val numParticipantes = contagemParticipantes[g.id] ?: 0
            tvParticipantes.text = ctx.resources.getQuantityString(R.plurals.label_participants, numParticipantes, numParticipantes)

            // Set emoji baseado na posição
            tvEmoji.text = emojis[position % emojis.size]

            // Set gradiente baseado na posição
            layoutContent.setBackgroundResource(gradientes[position % gradientes.size])

            view.setOnClickListener {
                val intent = Intent(this@GruposActivity, ParticipantesActivity::class.java)
                intent.putExtra("grupo", g)
                startActivity(intent)
            }

            view.setOnLongClickListener { v ->
                HapticFeedbackUtils.performMediumFeedback(v)
                exibirMenuContextoGrupo(v, g)
                true
            }

            return view
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
                            Log.e(TAG, "Erro ao atualizar nome do grupo", e)
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
