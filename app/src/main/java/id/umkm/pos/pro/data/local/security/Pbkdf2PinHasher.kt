package id.umkm.pos.pro.data.local.security

import id.umkm.pos.pro.core.contract.PinHasher
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class PinHashAlgorithm(
    val id: String,
    val jcaName: String,
    val iterations: Int,
    val saltBytes: Int = 16,
    val keyBytes: Int = 32,
) {
    PBKDF2_SHA256("sha256", "PBKDF2WithHmacSHA256", 100_000),
    PBKDF2_SHA1("sha1", "PBKDF2WithHmacSHA1", 60_000),
    ;

    companion object {
        const val SHA256_MIN_API: Int = 26

        fun forApiLevel(sdkInt: Int): PinHashAlgorithm =
            if (sdkInt >= SHA256_MIN_API) PBKDF2_SHA256 else PBKDF2_SHA1

        fun fromId(id: String): PinHashAlgorithm? = entries.firstOrNull { it.id == id }
    }
}

class Pbkdf2PinHasher(
    private val defaultAlgorithm: PinHashAlgorithm = PinHashAlgorithm.PBKDF2_SHA256,
    private val randomBytes: (Int) -> ByteArray = { size ->
        ByteArray(size).also { SecureRandom().nextBytes(it) }
    },
) : PinHasher {

    override fun hash(secret: String): String {
        val salt = randomBytes(defaultAlgorithm.saltBytes)
        val derived = derive(secret, salt, defaultAlgorithm, defaultAlgorithm.iterations)
        return listOf(
            SCHEME,
            defaultAlgorithm.id,
            defaultAlgorithm.iterations.toString(),
            salt.toHex(),
            derived.toHex(),
        ).joinToString(separator = SEPARATOR)
    }

    override fun verify(secret: String, encodedHash: String): Boolean {
        val parsed = EncodedPinHash.parseOrNull(encodedHash) ?: return false
        val algorithm = PinHashAlgorithm.fromId(parsed.algorithmId) ?: return false
        val candidate = runCatching {
            derive(secret, parsed.salt, algorithm, parsed.iterations)
        }.getOrNull() ?: return false
        return MessageDigest.isEqual(candidate, parsed.hash)
    }

    private fun derive(
        secret: String,
        salt: ByteArray,
        algorithm: PinHashAlgorithm,
        iterations: Int,
    ): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, algorithm.keyBytes * 8)
        return try {
            SecretKeyFactory.getInstance(algorithm.jcaName).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        private const val SCHEME = "pbkdf2"
        private const val SEPARATOR = "\$"
    }
}

internal data class EncodedPinHash(
    val algorithmId: String,
    val iterations: Int,
    val salt: ByteArray,
    val hash: ByteArray,
) {
    companion object {
        private const val PART_COUNT = 5

        fun parseOrNull(encoded: String): EncodedPinHash? {
            val parts = encoded.split('$')
            if (parts.size != PART_COUNT || parts[0] != "pbkdf2") return null
            val iterations = parts[2].toIntOrNull() ?: return null
            if (iterations <= 0) return null
            val salt = parts[3].hexToBytesOrNull() ?: return null
            val hash = parts[4].hexToBytesOrNull() ?: return null
            if (salt.isEmpty() || hash.isEmpty()) return null
            return EncodedPinHash(parts[1], iterations, salt, hash)
        }
    }
}

internal fun ByteArray.toHex(): String =
    joinToString(separator = "") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }

internal fun String.hexToBytesOrNull(): ByteArray? {
    if (isEmpty() || length % 2 != 0) return null
    val bytes = ByteArray(length / 2)
    for (index in bytes.indices) {
        val high = this[index * 2].digitToIntOrNull(radix = 16) ?: return null
        val low = this[index * 2 + 1].digitToIntOrNull(radix = 16) ?: return null
        bytes[index] = ((high shl 4) or low).toByte()
    }
    return bytes
}
