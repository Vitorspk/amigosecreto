package activity.amigosecreto

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import activity.amigosecreto.util.QrCodeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Exibe o QR Code do resultado do sorteio para que o participante possa
 * revelar seu amigo secreto de forma privada, apresentando a tela a outra pessoa.
 *
 * Extras de entrada (obrigatórios):
 * - [EXTRA_NOME_PARTICIPANTE]: nome do participante dono do QR Code
 * - [EXTRA_CONTEUDO_QR]: conteúdo a ser codificado (ex: nome do amigo sorteado)
 */
class QrCodeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOME_PARTICIPANTE = "qr_nome_participante"
        const val EXTRA_CONTEUDO_QR = "qr_conteudo"
    }

    private lateinit var imageViewQr: ImageView
    private var qrBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_qrcode)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_qrcode)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val rootView = findViewById<View>(R.id.root_qrcode)
        val padTop = rootView.paddingTop
        val padBottom = rootView.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, padTop + systemBars.top, v.paddingRight, padBottom + systemBars.bottom)
            insets
        }

        val nomeParticipante = intent.getStringExtra(EXTRA_NOME_PARTICIPANTE) ?: ""
        val conteudoQr = intent.getStringExtra(EXTRA_CONTEUDO_QR) ?: ""

        findViewById<TextView>(R.id.tv_qr_instrucao).text =
            getString(R.string.qr_instrucao, nomeParticipante)

        imageViewQr = findViewById(R.id.iv_qrcode)
        val btnSalvar = findViewById<MaterialButton>(R.id.btn_salvar_qr)

        if (conteudoQr.isBlank()) {
            Toast.makeText(this, R.string.qr_erro_sem_sorteio, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        gerarEExibirQr(conteudoQr)

        btnSalvar.setOnClickListener { salvarQrNaGaleria(nomeParticipante) }
    }

    private fun gerarEExibirQr(conteudo: String) {
        val bitmap = QrCodeHelper.gerar(conteudo)
        if (bitmap != null) {
            qrBitmap = bitmap
            imageViewQr.setImageBitmap(bitmap)
        } else {
            Toast.makeText(this, R.string.qr_erro_gerar, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun salvarQrNaGaleria(nomeParticipante: String) {
        val bitmap = qrBitmap ?: return
        val nomeArquivo = "amigo_secreto_${nomeParticipante.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}_qr.png"

        lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) {
                try {
                    salvarBitmap(bitmap, nomeArquivo)
                } catch (_: Exception) {
                    null
                }
            }
            val msgRes = if (uri != null) R.string.qr_salvo_sucesso else R.string.qr_erro_salvar
            Toast.makeText(this@QrCodeActivity, msgRes, Toast.LENGTH_SHORT).show()
        }
    }

    private fun salvarBitmap(bitmap: Bitmap, nomeArquivo: String): Uri? {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, nomeArquivo)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AmigoSecreto")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, contentValues) ?: return null

        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        return uri
    }

    override fun onDestroy() {
        qrBitmap?.recycle()
        qrBitmap = null
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
