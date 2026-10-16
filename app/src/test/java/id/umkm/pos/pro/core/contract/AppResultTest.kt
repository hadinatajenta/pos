package id.umkm.pos.pro.core.contract

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppResultTest {

    @Test
    fun successCarriesItsValue() {
        val result: AppResult<Long> = AppResult.Success(42L)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).value).isEqualTo(42L)
    }

    @Test
    fun failureCarriesItsError() {
        val error = AppError.Validation("quantity must be positive")
        val result: AppResult<Unit> = AppResult.Failure(error)

        val failure = result as AppResult.Failure
        assertThat(failure.error).isEqualTo(error)
        assertThat(failure.error.message).isEqualTo("quantity must be positive")
    }

    @Test
    fun everyErrorTypeReportsItsOwnMessage() {
        assertThat(AppError.Validation("v").message).isEqualTo("v")
        assertThat(AppError.NotFound("n").message).isEqualTo("n")
        assertThat(AppError.Storage("s").message).isEqualTo("s")
        assertThat(AppError.Hardware("h").message).isEqualTo("h")
        assertThat(AppError.Network("c").message).isEqualTo("c")
        assertThat(AppError.Unexpected("u").message).isEqualTo("u")
    }
}
