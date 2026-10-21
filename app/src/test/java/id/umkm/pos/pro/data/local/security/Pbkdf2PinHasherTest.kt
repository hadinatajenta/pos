package id.umkm.pos.pro.data.local.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Pbkdf2PinHasherTest {

    private val fixedSalt = ByteArray(16) { 7 }
    private val hasher = Pbkdf2PinHasher(
        defaultAlgorithm = PinHashAlgorithm.PBKDF2_SHA256,
        randomBytes = { size -> fixedSalt.copyOf(size) },
    )

    @Test
    fun encodedHashCarriesSchemeAlgorithmIterationsAndSalt() {
        val encoded = hasher.hash("1234")

        val parts = encoded.split('$')
        assertThat(parts).hasSize(5)
        assertThat(parts[0]).isEqualTo("pbkdf2")
        assertThat(parts[1]).isEqualTo("sha256")
        assertThat(parts[2]).isEqualTo("100000")
        assertThat(parts[3]).isEqualTo("07070707070707070707070707070707")
        assertThat(parts[4]).hasLength(64)
    }

    @Test
    fun hashingIsDeterministicForTheSameSalt() {
        assertThat(hasher.hash("1234")).isEqualTo(hasher.hash("1234"))
    }

    @Test
    fun verifyAcceptsTheCorrectSecretAndRejectsOthers() {
        val encoded = hasher.hash("1234")

        assertThat(hasher.verify("1234", encoded)).isTrue()
        assertThat(hasher.verify("1235", encoded)).isFalse()
        assertThat(hasher.verify("", encoded)).isFalse()
    }

    @Test
    fun verifyRejectsAMalformedOrUnknownEncoding() {
        assertThat(hasher.verify("1234", "plain text")).isFalse()
        assertThat(hasher.verify("1234", "pbkdf2\$md5\$1\$00\$00")).isFalse()
        assertThat(hasher.verify("1234", "pbkdf2\$sha256\$0\$00\$00")).isFalse()
        assertThat(hasher.verify("1234", "pbkdf2\$sha256\$100000\$zz\$zz")).isFalse()
        assertThat(hasher.verify("1234", "pbkdf2\$sha256\$100000\$0f\$0f")).isFalse()
    }

    @Test
    fun verifyUsesTheAlgorithmStoredInsideTheEncoding() {
        val legacyHasher = Pbkdf2PinHasher(
            defaultAlgorithm = PinHashAlgorithm.PBKDF2_SHA1,
            randomBytes = { size -> fixedSalt.copyOf(size) },
        )
        val legacyEncoded = legacyHasher.hash("9876")

        assertThat(legacyEncoded).contains("pbkdf2\$sha1\$60000\$")
        assertThat(hasher.verify("9876", legacyEncoded)).isTrue()
    }

    @Test
    fun twoDifferentSaltsProduceDifferentHashes() {
        val first = Pbkdf2PinHasher(randomBytes = { ByteArray(it) { value -> 1 } }).hash("1234")
        val second = Pbkdf2PinHasher(randomBytes = { ByteArray(it) { value -> 2 } }).hash("1234")

        assertThat(first).isNotEqualTo(second)
    }
}
