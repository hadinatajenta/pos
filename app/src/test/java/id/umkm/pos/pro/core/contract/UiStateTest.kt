package id.umkm.pos.pro.core.contract

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UiStateTest {

    @Test
    fun loadingAndEmptyAreSingletons() {
        assertThat(UiState.Loading).isSameInstanceAs(UiState.Loading)
        assertThat(UiState.Empty).isSameInstanceAs(UiState.Empty)
    }

    @Test
    fun successCarriesItsData() {
        val state = UiState.Success(listOf("a", "b"))

        assertThat(state.data).containsExactly("a", "b").inOrder()
    }

    @Test
    fun errorCarriesItsError() {
        val error = AppError.Storage("database is locked")
        val state = UiState.Error(error)

        assertThat(state.error).isEqualTo(error)
    }
}
