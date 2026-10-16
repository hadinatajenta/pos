package id.umkm.pos.pro.hardware.scanner

import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.ScanSource
import org.junit.Test

class KeyboardWedgeScanDecoderTest {

    @Test
    fun rapidCharactersThenTerminatorEmitOneScanEvent() {
        val decoder = KeyboardWedgeScanDecoder()

        assertThat(decoder.onCharacter('1', 0)).isNull()
        assertThat(decoder.onCharacter('2', 5)).isNull()
        assertThat(decoder.onCharacter('3', 10)).isNull()

        val scanEvent = decoder.onCharacter('\n', 15)

        assertThat(scanEvent).isNotNull()
        assertThat(scanEvent!!.code).isEqualTo("123")
        assertThat(scanEvent.source).isEqualTo(ScanSource.HARDWARE_HID)
        assertThat(scanEvent.timestampMillis).isEqualTo(15L)
    }

    @Test
    fun slowTypingResetsThePendingCode() {
        val decoder = KeyboardWedgeScanDecoder(maxInterCharacterMillis = 120)

        decoder.onCharacter('1', 0)
        decoder.onCharacter('2', 500)

        val scanEvent = decoder.onCharacter('\n', 505)

        assertThat(scanEvent!!.code).isEqualTo("2")
    }

    @Test
    fun terminatorWithoutPendingCharactersEmitsNothing() {
        val decoder = KeyboardWedgeScanDecoder()

        assertThat(decoder.onCharacter('\n', 10)).isNull()
        assertThat(decoder.onCharacter('\r', 20)).isNull()
        assertThat(decoder.onCharacter('\t', 30)).isNull()
    }

    @Test
    fun flushReleasesPendingCharactersWithoutTerminator() {
        val decoder = KeyboardWedgeScanDecoder()

        decoder.onCharacter('A', 0)
        decoder.onCharacter('B', 5)

        val scanEvent = decoder.flush(9)

        assertThat(scanEvent!!.code).isEqualTo("AB")
        assertThat(scanEvent.timestampMillis).isEqualTo(9L)
        assertThat(decoder.flush(10)).isNull()
    }

    @Test
    fun resetDiscardsPendingCharacters() {
        val decoder = KeyboardWedgeScanDecoder()

        decoder.onCharacter('9', 0)
        decoder.onCharacter('9', 5)
        decoder.reset()

        assertThat(decoder.onCharacter('\n', 10)).isNull()
    }

    @Test
    fun controlCharactersAreIgnored() {
        val decoder = KeyboardWedgeScanDecoder()

        assertThat(decoder.onCharacter('\u0002', 0)).isNull()
        decoder.onCharacter('7', 5)

        assertThat(decoder.onCharacter('\n', 10)!!.code).isEqualTo("7")
    }

    @Test
    fun codeLengthIsCappedAndResetsTheBuffer() {
        val decoder = KeyboardWedgeScanDecoder(maxCodeLength = 3)

        decoder.onCharacter('1', 0)
        decoder.onCharacter('2', 1)
        decoder.onCharacter('3', 2)
        decoder.onCharacter('4', 3)

        assertThat(decoder.onCharacter('\n', 4)!!.code).isEqualTo("4")
    }
}
