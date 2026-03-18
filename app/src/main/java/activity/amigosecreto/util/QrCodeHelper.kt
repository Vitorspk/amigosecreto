package activity.amigosecreto.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
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

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )

        val bitMatrix = QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, tamanho, tamanho, hints)
        val bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.RGB_565)

        for (x in 0 until tamanho) {
            for (y in 0 until tamanho) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
