package id.umkm.pos.pro.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PersonAvatarTest {

    @Test
    fun singleWordUsesFirstTwoLetters() {
        assertThat(personInitials("Dini")).isEqualTo("DI")
    }

    @Test
    fun twoWordsUseTheFirstLetterOfEach() {
        assertThat(personInitials("Kasir Utama")).isEqualTo("KU")
        assertThat(personInitials("Budi Santoso")).isEqualTo("BS")
    }

    @Test
    fun singleLetterStaysAsIs() {
        assertThat(personInitials("A")).isEqualTo("A")
    }

    @Test
    fun blankNameFallsBackToQuestionMark() {
        assertThat(personInitials("   ")).isEqualTo("?")
        assertThat(personInitials("")).isEqualTo("?")
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertThat(personInitials("  dini  ")).isEqualTo("DI")
    }
}
