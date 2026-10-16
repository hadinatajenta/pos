package id.umkm.pos.pro.hardware.scanner

import android.view.KeyEvent
import id.umkm.pos.pro.core.contract.ScanEvent
import id.umkm.pos.pro.core.contract.ScannerInput
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class HardwareScannerInput(
    private val decoder: KeyboardWedgeScanDecoder = KeyboardWedgeScanDecoder(),
) : ScannerInput {

    private val scanEventFlow = MutableSharedFlow<ScanEvent>(
        replay = 0,
        extraBufferCapacity = SCAN_EVENT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val scanEvents: Flow<ScanEvent> = scanEventFlow.asSharedFlow()

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount > 0) return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                publishScanEvent(decoder.onCharacter(TERMINATOR_LINE_FEED, event.eventTime))
                return true
            }

            KeyEvent.KEYCODE_TAB -> {
                publishScanEvent(decoder.onCharacter(TERMINATOR_TAB, event.eventTime))
                return true
            }
        }
        val unicodeCharacter = event.unicodeChar
        if (unicodeCharacter == 0) return false
        val character = unicodeCharacter.toChar()
        if (character.isISOControl()) return false
        publishScanEvent(decoder.onCharacter(character, event.eventTime))
        return false
    }

    fun reset() {
        decoder.reset()
    }

    private fun publishScanEvent(scanEvent: ScanEvent?) {
        if (scanEvent != null) scanEventFlow.tryEmit(scanEvent)
    }

    private companion object {
        const val SCAN_EVENT_BUFFER = 64
        const val TERMINATOR_LINE_FEED = '\n'
        const val TERMINATOR_TAB = '\t'
    }
}
