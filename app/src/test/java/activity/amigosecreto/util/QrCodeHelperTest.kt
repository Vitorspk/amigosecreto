package activity.amigosecreto.util

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class QrCodeHelperTest {

    @Test
    fun `gerar com conteudo em branco retorna null`() {
        assertNull(QrCodeHelper.gerar(""))
    }

    @Test
    fun `gerar com conteudo apenas espacos retorna null`() {
        assertNull(QrCodeHelper.gerar("   "))
    }

    @Test
    fun `gerar com conteudo valido retorna bitmap nao nulo`() {
        assertNotNull(QrCodeHelper.gerar("João"))
    }

    @Test
    fun `gerar retorna bitmap com tamanho padrao 512x512`() {
        val bitmap = QrCodeHelper.gerar("Maria")
        assertNotNull(bitmap)
        assertEquals(512, bitmap!!.width)
        assertEquals(512, bitmap.height)
    }

    @Test
    fun `gerar com tamanho customizado retorna bitmap no tamanho correto`() {
        val bitmap = QrCodeHelper.gerar("Carlos", tamanho = 256)
        assertNotNull(bitmap)
        assertEquals(256, bitmap!!.width)
        assertEquals(256, bitmap.height)
    }

    @Test
    fun `gerar retorna bitmap quadrado`() {
        val bitmap = QrCodeHelper.gerar("Teste QR Code")
        assertNotNull(bitmap)
        assertEquals(bitmap!!.width, bitmap.height)
    }

    @Test
    fun `gerar retorna bitmap com config RGB_565`() {
        val bitmap = QrCodeHelper.gerar("Amigo Secreto")
        assertNotNull(bitmap)
        assertEquals(Bitmap.Config.RGB_565, bitmap!!.config)
    }

    @Test
    fun `gerar com conteudo unicode retorna bitmap nao nulo`() {
        assertNotNull(QrCodeHelper.gerar("Revelação: João Café"))
    }

    @Test
    fun `gerar com conteudo longo retorna bitmap nao nulo`() {
        val conteudo = "a".repeat(200)
        assertNotNull(QrCodeHelper.gerar(conteudo))
    }

    @Test
    fun `gerar com conteudo excedendo capacidade do QR retorna null sem lancar excecao`() {
        // Mais de ~7000 chars excede a capacidade máxima do QR Code (level M)
        // WriterException deve ser capturado internamente e retornar null
        val conteudo = "a".repeat(8000)
        assertNull(QrCodeHelper.gerar(conteudo))
    }

    @Test
    fun `gerar chamadas consecutivas com mesmo conteudo retornam bitmaps com mesmas dimensoes`() {
        val bitmap1 = QrCodeHelper.gerar("Mesmo conteudo")
        val bitmap2 = QrCodeHelper.gerar("Mesmo conteudo")
        assertNotNull(bitmap1)
        assertNotNull(bitmap2)
        assertEquals(bitmap1!!.width, bitmap2!!.width)
        assertEquals(bitmap1.height, bitmap2.height)
    }
}
