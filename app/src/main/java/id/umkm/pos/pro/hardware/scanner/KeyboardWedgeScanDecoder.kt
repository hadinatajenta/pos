package id.umkm.pos.pro.hardware.scanner

import id.umkm.pos.pro.core.contract.ScanEvent
import id.umkm.pos.pro.core.contract.ScanSource

class KeyboardWedgeScanDecoder(
    private val maxInterCharacterMillis: Long = DEFAULT_MAX_INTER_CHARACTER_MILLIS,
    private val maxCodeLength: Int = DEFAULT_MAX_CODE_LENGTH,
) {

    private val pendingCharacters = StringBuilder()
    private var lastCharacterAtMillis = 0L

    fun onCharacter(character: Char, timestampMillis: Long): ScanEvent? {
        if (pendingCharacters.isNotEmpty() && timestampMillis - lastCharacterAtMillis > maxInterCharacterMillis) {
            pendingCharacters.setLength(0)
        }
        lastCharacterAtMillis = timestampMillis
        if (isTerminator(character)) return releaseScanEvent(timestampMillis)
        if (character.isISOControl()) return null
        if (pendingCharacters.length >= maxCodeLength) pendingCharacters.setLength(0)
        pendingCharacters.append(character)
        return null
    }

    fun flush(timestampMillis: Long): ScanEvent? = releaseScanEvent(timestampMillis)

    fun hasPendingCharacters(): Boolean = pendingCharacters.isNotEmpty()

    fun hasLivePendingCharacters(timestampMillis: Long): Boolean {
        if (pendingCharacters.isEmpty()) return false
        return timestampMillis - lastCharacterAtMillis <= maxInterCharacterMillis
    }

    fun reset() {
        pendingCharacters.setLength(0)
        lastCharacterAtMillis = 0L
    }

    private fun releaseScanEvent(timestampMillis: Long): ScanEvent? {
        if (pendingCharacters.isEmpty()) return null
        val code = pendingCharacters.toString()
        pendingCharacters.setLength(0)
        if (code.length < MIN_CODE_LENGTH) return null
        return ScanEvent(code = code, source = ScanSource.HARDWARE_HID, timestampMillis = timestampMillis)
    }

    private fun isTerminator(character: Char): Boolean =
        character == LINE_FEED || character == CARRIAGE_RETURN || character == TAB

    companion object {
        const val DEFAULT_MAX_INTER_CHARACTER_MILLIS = 120L
        const val DEFAULT_MAX_CODE_LENGTH = 128
        const val MIN_CODE_LENGTH = 1
        private const val LINE_FEED = '\n'
        private const val CARRIAGE_RETURN = '\r'
        private const val TAB = '\t'
    }
}
