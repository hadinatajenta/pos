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

    @Volatile
    private var isEnabled: Boolean = true

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            decoder.reset()
        }
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (!isEnabled) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount > 0) return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER ->
                return consumeTerminator(TERMINATOR_LINE_FEED, event)

            KeyEvent.KEYCODE_TAB ->
                return consumeTerminator(TERMINATOR_TAB, event)
        }
        val unicodeCharacter = event.unicodeChar
        if (unicodeCharacter == 0) return false
        val character = unicodeCharacter.toChar()
        if (character.isISOControl()) return false
        publishScanEvent(decoder.onCharacter(character, event.eventTime))
        return false
    }

    private fun consumeTerminator(terminator: Char, event: KeyEvent): Boolean {
        if (!decoder.hasLivePendingCharacters(event.eventTime)) {
            decoder.reset()
            return false
        }
        publishScanEvent(decoder.onCharacter(terminator, event.eventTime))
        return true
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
