package id.umkm.pos.pro.ui.common

import androidx.compose.runtime.Composable

class UiText private constructor(private val resolveBlock: (PosStrings) -> String) {

    fun resolve(strings: PosStrings): String = resolveBlock(strings)

    override fun toString(): String = "UiText"

    companion object {
        fun literal(value: String): UiText = UiText { value }

        fun from(build: (PosStrings) -> String): UiText = UiText(build)
    }
}

@Composable
fun UiText.asText(): String = resolve(LocalPosStrings.current)

@Composable
fun UiText?.asTextOrNull(): String? = this?.resolve(LocalPosStrings.current)

fun UiText?.resolveOrNull(strings: PosStrings): String? = this?.resolve(strings)
