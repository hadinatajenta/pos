package id.umkm.pos.pro.hardware.scanner

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import id.umkm.pos.pro.core.contract.ScanEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HardwareScannerInputTest {

    private fun keyDown(keyCode: Int, atMillis: Long, repeat: Int = 0): KeyEvent =
        KeyEvent(atMillis, atMillis, KeyEvent.ACTION_DOWN, keyCode, repeat)

    @Test
    fun aFastCharacterRunFollowedByEnterIsConsumedAndEmitsTheCode() = runBlocking {
        val input = HardwareScannerInput()
        val expectedCode = "8991234567890"
        val event: ScanEvent = withTimeout(2_000) {
            val pending = async(start = CoroutineStart.UNDISPATCHED) { input.scanEvents.first() }
            expectedCode.forEachIndexed { index, character ->
                val keyCode = KeyEvent.KEYCODE_0 + (character - '0')
                assertThat(input.handleKeyEvent(keyDown(keyCode, atMillis = index * 10L))).isFalse()
            }
            assertThat(
                input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_ENTER, atMillis = expectedCode.length * 10L)),
            ).isTrue()
            pending.await()
        }

        assertThat(event.code).isEqualTo(expectedCode)
    }

    @Test
    fun enterWithNoPendingSequenceIsNotConsumed() {
        val input = HardwareScannerInput()

        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_ENTER, atMillis = 0L))).isFalse()
        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_NUMPAD_ENTER, atMillis = 10L))).isFalse()
        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_TAB, atMillis = 20L))).isFalse()
    }

    @Test
    fun aDisabledScannerConsumesNothing() {
        val input = HardwareScannerInput()
        input.setEnabled(false)

        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_8, atMillis = 0L))).isFalse()
        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_ENTER, atMillis = 10L))).isFalse()
    }

    @Test
    fun aStaleSequenceDoesNotSwallowEnter() {
        val input = HardwareScannerInput()

        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_8, atMillis = 0L))).isFalse()
        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_9, atMillis = 10L))).isFalse()
        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_ENTER, atMillis = 5_000L))).isFalse()
        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_ENTER, atMillis = 5_010L))).isFalse()
    }

    @Test
    fun addingAProductDuringAScanSequenceIsNotConsumed() {
        val input = HardwareScannerInput()

        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_A, atMillis = 0L))).isFalse()
        assertThat(input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_B, atMillis = 10L))).isFalse()

        val event: ScanEvent = runBlocking {
            withTimeout(2_000) {
                val pending = async(start = CoroutineStart.UNDISPATCHED) { input.scanEvents.first() }
                input.handleKeyEvent(keyDown(KeyEvent.KEYCODE_ENTER, atMillis = 20L))
                pending.await()
            }
        }

        assertThat(event.code).isEqualTo("ab")
    }
}
