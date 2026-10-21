package id.umkm.pos.pro.domain.security

import java.security.SecureRandom

private const val RECOVERY_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

data class RecoveryCode(val groups: List<String>) {
    val formatted: String get() = groups.joinToString(separator = "-")
    val compact: String get() = groups.joinToString(separator = "")

    companion object {
        fun normalize(input: String): String = input.uppercase().filter { it in RECOVERY_ALPHABET }
    }
}

class RecoveryCodeGenerator(
    private val groupCount: Int = 4,
    private val groupLength: Int = 5,
    private val randomByte: () -> Int = { SecureRandom().nextInt(256) },
) {
    fun generate(): RecoveryCode {
        val characters = (0 until groupCount * groupLength).map {
            RECOVERY_ALPHABET[randomByte() % RECOVERY_ALPHABET.length]
        }
        return RecoveryCode(characters.joinToString(separator = "").chunked(groupLength))
    }
}
