package id.umkm.pos.pro.domain.qris

import com.google.common.truth.Truth.assertThat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Test

class QrisPayloadTest {

    private val staticQris =
        "000201" +
            "010211" +
            "52044814" +
            "5303360" +
            "5802ID" +
            "5909TOKO DEMO" +
            "6007JAKARTA" +
            "6304ABCD"

    @Test
    fun crc16MatchesTheStandardCheckValue() {
        assertThat(QrisPayload.crc16("123456789")).isEqualTo(0x29B1)
    }

    @Test
    fun generatedDynamicPayloadCarriesAValidChecksum() {
        val dynamic = QrisPayload.withAmount(staticQris, 50_000L)

        assertThat(dynamic).isNotNull()
        assertThat(QrisPayload.isValid(dynamic!!)).isTrue()
        assertThat(QrisPayload.amountOf(dynamic)).isEqualTo(50_000L)
    }

    @Test
    fun replacingAnExistingAmountKeepsASingleValidAmount() {
        val first = QrisPayload.withAmount(staticQris, 12_500L)!!
        val second = QrisPayload.withAmount(first, 99_000L)!!

        assertThat(QrisPayload.isValid(second)).isTrue()
        assertThat(QrisPayload.amountOf(second)).isEqualTo(99_000L)
        assertThat(second).contains("540599000")
    }

    @Test
    fun rejectsNonPositiveAmounts() {
        assertThat(QrisPayload.withAmount(staticQris, 0L)).isNull()
        assertThat(QrisPayload.withAmount(staticQris, -1L)).isNull()
    }

    @Test
    fun rejectsPayloadsWithBrokenChecksums() {
        val tampered = staticQris.dropLast(4) + "0000"

        assertThat(QrisPayload.isValid(tampered)).isFalse()
    }

    @Test
    fun rejectsPayloadsThatAreNotQris() {
        assertThat(QrisPayload.isValid("")).isFalse()
        assertThat(QrisPayload.isValid("https://example.com")).isFalse()
    }

    @Test
    fun parsesAmountsThatCarryDecimals() {
        val withDecimal = QrisPayload.withAmount(staticQris, 7_500L)!!
            .replace("54047500", "54077500.00")

        assertThat(QrisPayload.amountOf(withDecimal)).isEqualTo(7_500L)
    }

    @Test
    fun generatedQrDecodesBackToTheDynamicPayload() {
        val dynamic = QrisPayload.withAmount(staticQris, 42_000L)!!
        val matrix = QRCodeWriter().encode(dynamic, BarcodeFormat.QR_CODE, 480, 480)
        val pixels = IntArray(matrix.width * matrix.height)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                pixels[y * matrix.width + x] = if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        val source = RGBLuminanceSource(matrix.width, matrix.height, pixels)
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))

        assertThat(decoded.text).isEqualTo(dynamic)
        assertThat(QrisPayload.amountOf(decoded.text)).isEqualTo(42_000L)
    }
}
