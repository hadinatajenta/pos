package id.umkm.pos.pro.ui.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecoveryCodePrintingTest {

    @Test
    fun aShortLineIsKeptAsOneLine() {
        assertThat(wrapRecoveryCodeLine("Kode hanya ditampilkan sekali.", 62))
            .containsExactly("Kode hanya ditampilkan sekali.")
    }

    @Test
    fun aLongParagraphIsSplitOnWordBoundaries() {
        val instruction =
            "Kode ini membuka akses ke aplikasi jika PIN lupa. Simpan di tempat aman dan jangan dibagikan."

        val lines = wrapRecoveryCodeLine(instruction, 40)

        assertThat(lines.size).isGreaterThan(1)
        lines.forEach { line ->
            assertThat(line.length).isAtMost(40)
            assertThat(line).doesNotContain("  ")
        }
        assertThat(lines.joinToString(" ")).isEqualTo(instruction)
    }

    @Test
    fun emptyTextProducesNoLines() {
        assertThat(wrapRecoveryCodeLine("", 40)).isEmpty()
    }
}
