package id.umkm.pos.pro.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object PosColors {
    val ForestGreen = Color(0xFF0B3B32)
    val DeepGreen = Color(0xFF082D27)
    val GreenElevated = Color(0xFF14503F)
    val GreenSoft = Color(0xFFE3EDE8)

    val Background = Color(0xFFF7F5EF)
    val Surface = Color(0xFFFFFFFF)
    val SoftNeutral = Color(0xFFF4F1E9)
    val Border = Color(0xFFE8E5DD)

    val TextPrimary = Color(0xFF17231F)
    val TextSecondary = Color(0xFF6D7771)

    val Gold = Color(0xFFE7C46A)
    val GoldContainer = Color(0xFFF7EDD2)
    val OnGoldContainer = Color(0xFF4A3907)

    val Success = Color(0xFF21845D)
    val SuccessContainer = Color(0xFFDCEFE5)
    val Warning = Color(0xFFB87917)
    val WarningContainer = Color(0xFFF7E8CC)
    val Danger = Color(0xFFC74742)
    val DangerContainer = Color(0xFFF8DEDB)
    val NeutralContainer = Color(0xFFEDEAE1)

    val ChartGrid = Color(0xFFE8E5DD)
    val ChartLine = Color(0xFF0B3B32)
    val ChartArea = Color(0x1F21845D)
    val ChartBar = Color(0xFF1E6B54)
    val ChartBarMuted = Color(0xFFBCD6CA)
}

object PosColorsDark {
    val ForestGreen = Color(0xFF16342B)
    val DeepGreen = Color(0xFF0C211B)
    val GreenElevated = Color(0xFF1D4A3C)
    val GreenSoft = Color(0xFF1B2C25)

    val Background = Color(0xFF0E1411)
    val Surface = Color(0xFF151C18)
    val SoftNeutral = Color(0xFF1C2420)
    val Border = Color(0xFF2A342E)

    val TextPrimary = Color(0xFFE7EDE9)
    val TextSecondary = Color(0xFF9AA69F)

    val Gold = Color(0xFFE7C46A)
    val GoldContainer = Color(0xFF3A3117)
    val OnGoldContainer = Color(0xFFF2E2B4)

    val Success = Color(0xFF7FCBA7)
    val SuccessContainer = Color(0xFF17382B)
    val Warning = Color(0xFFE0AE5C)
    val WarningContainer = Color(0xFF3C2F14)
    val Danger = Color(0xFFE99B96)
    val DangerContainer = Color(0xFF3E1F1D)
    val NeutralContainer = Color(0xFF232C27)

    val ChartGrid = Color(0xFF2A342E)
    val ChartLine = Color(0xFF7FCBA7)
    val ChartArea = Color(0x337FCBA7)
    val ChartBar = Color(0xFF3E7F66)
    val ChartBarMuted = Color(0xFF2C4A3E)
}

@Immutable
data class PosSemanticPalette(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val gold: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
    val neutralContainer: Color,
    val chartGrid: Color,
    val chartLine: Color,
    val chartArea: Color,
    val chartBar: Color,
    val chartBarMuted: Color,
)

val PosLightSemanticPalette = PosSemanticPalette(
    success = PosColors.Success,
    successContainer = PosColors.SuccessContainer,
    warning = PosColors.Warning,
    warningContainer = PosColors.WarningContainer,
    danger = PosColors.Danger,
    dangerContainer = PosColors.DangerContainer,
    gold = PosColors.Gold,
    goldContainer = PosColors.GoldContainer,
    onGoldContainer = PosColors.OnGoldContainer,
    neutralContainer = PosColors.NeutralContainer,
    chartGrid = PosColors.ChartGrid,
    chartLine = PosColors.ChartLine,
    chartArea = PosColors.ChartArea,
    chartBar = PosColors.ChartBar,
    chartBarMuted = PosColors.ChartBarMuted,
)

val PosDarkSemanticPalette = PosSemanticPalette(
    success = PosColorsDark.Success,
    successContainer = PosColorsDark.SuccessContainer,
    warning = PosColorsDark.Warning,
    warningContainer = PosColorsDark.WarningContainer,
    danger = PosColorsDark.Danger,
    dangerContainer = PosColorsDark.DangerContainer,
    gold = PosColorsDark.Gold,
    goldContainer = PosColorsDark.GoldContainer,
    onGoldContainer = PosColorsDark.OnGoldContainer,
    neutralContainer = PosColorsDark.NeutralContainer,
    chartGrid = PosColorsDark.ChartGrid,
    chartLine = PosColorsDark.ChartLine,
    chartArea = PosColorsDark.ChartArea,
    chartBar = PosColorsDark.ChartBar,
    chartBarMuted = PosColorsDark.ChartBarMuted,
)

val LocalPosSemanticPalette = staticCompositionLocalOf { PosLightSemanticPalette }
