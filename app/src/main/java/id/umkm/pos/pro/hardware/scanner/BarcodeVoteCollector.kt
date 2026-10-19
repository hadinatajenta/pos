package id.umkm.pos.pro.hardware.scanner

class BarcodeVoteCollector(private val requiredVotes: Int = DEFAULT_REQUIRED_VOTES) {

    private var candidate: String? = null
    private var votes = 0

    fun submit(code: String?): String? {
        if (code.isNullOrBlank()) return null
        candidate = if (code == candidate) {
            votes += 1
            code
        } else {
            votes = 1
            code
        }
        return code.takeIf { votes >= requiredVotes }
    }

    fun reset() {
        candidate = null
        votes = 0
    }

    companion object {
        const val DEFAULT_REQUIRED_VOTES = 2
    }
}
