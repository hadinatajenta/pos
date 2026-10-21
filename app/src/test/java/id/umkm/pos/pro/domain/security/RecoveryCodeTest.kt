package id.umkm.pos.pro.domain.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecoveryCodeTest {

    @Test
    fun generateProducesFourGroupsOfFiveCharacters() {
        val code = RecoveryCodeGenerator().generate()

        assertThat(code.groups).hasSize(4)
        code.groups.forEach { group -> assertThat(group).hasLength(5) }
        assertThat(code.compact).hasLength(20)
        assertThat(code.formatted.split('-')).hasSize(4)
    }

    @Test
    fun generatedCharactersStayInsideTheUnambiguousAlphabet() {
        val joined = (0 until 200)
            .map { RecoveryCodeGenerator().generate().compact }
            .joinToString(separator = "")

        assertThat(joined).hasLength(4_000)
        assertThat(joined).doesNotContain("I")
        assertThat(joined).doesNotContain("L")
        assertThat(joined).doesNotContain("O")
        assertThat(joined).doesNotContain("U")
        assertThat(joined).matches("[0-9A-HJKMNP-TV-Z]+")
    }

    @Test
    fun generateIsDeterministicForAnInjectedByteSource() {
        val zeroBytes = RecoveryCodeGenerator(randomByte = { 0 }).generate()
        val highBytes = RecoveryCodeGenerator(randomByte = { 31 }).generate()

        assertThat(zeroBytes.formatted).isEqualTo("00000-00000-00000-00000")
        assertThat(highBytes.formatted).isEqualTo("ZZZZZ-ZZZZZ-ZZZZZ-ZZZZZ")
    }

    @Test
    fun normalizeAcceptsHowPeopleActuallyTypeTheCode() {
        assertThat(RecoveryCode.normalize(" 4f7zq -8k2mn ")).isEqualTo("4F7ZQ8K2MN")
        assertThat(RecoveryCode.normalize("4f7zq8k2mn")).isEqualTo("4F7ZQ8K2MN")
        assertThat(RecoveryCode.normalize("4F7ZQ-8K2MN")).isEqualTo("4F7ZQ8K2MN")
    }

    @Test
    fun normalizeDropsCharactersThatAreNotInTheAlphabet() {
        assertThat(RecoveryCode.normalize("I-L-O-U")).isEmpty()
        assertThat(RecoveryCode.normalize("ab!@#12")).isEqualTo("AB12")
    }

    @Test
    fun aGeneratedCodeSurvivesNormalizationOfItsPrintedForm() {
        val code = RecoveryCodeGenerator(randomByte = { 7 }).generate()

        assertThat(RecoveryCode.normalize(code.formatted)).isEqualTo(code.compact)
    }
}
