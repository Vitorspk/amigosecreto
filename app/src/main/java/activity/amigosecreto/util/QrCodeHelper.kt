package activity.amigosecreto.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Utilitário stateless para gerar QR Codes como [Bitmap].
 *
 * Usa a biblioteca ZXing Core (pure Java) — sem dependência de câmera ou Activity especial.
 * O conteúdo codificado é apenas o nome do amigo secreto, destinado a ser exibido em tela
 * para que o participante revele seu resultado de forma privada.
 */
object QrCodeHelper {

    private const val DEFAULT_SIZE_PX = 512

    /**
     * Gera um [Bitmap] quadrado com o QR Code do [conteudo] fornecido.
     *
     * @param conteudo Texto a ser codificado no QR Code.
     * @param tamanho  Largura/altura em pixels do bitmap resultante. Padrão: [DEFAULT_SIZE_PX].
     * @return [Bitmap] com o QR Code, ou null se [conteudo] estiver vazio.
     */
    fun gerar(conteudo: String, tamanho: Int = DEFAULT_SIZE_PX): Bitmap? {
        if (conteudo.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8"
                // MARGIN omitido → ZXing usa padrão de 4 módulos (mínimo ISO/IEC 18004)
            )
            val bitMatrix = QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, tamanho, tamanho, hints)
            val pixels = IntArray(tamanho * tamanho) { index ->
                val x = index % tamanho
                val y = index / tamanho
                if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
            val bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.RGB_565)
            bitmap.setPixels(pixels, 0, tamanho, 0, 0, tamanho, tamanho)
            bitmap
        } catch (_: WriterException) {
            null
        }
    }
}
