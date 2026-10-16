package id.umkm.pos.pro.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object PosColors {
    val ForestGreen = Color(0xFF082923)
    val DeepGreen = Color(0xFF061F1A)
    val GreenElevated = Color(0xFF103D32)
    val GreenSoft = Color(0xFFDDEFE5)

    val Secondary = Color(0xFF245848)
    val Accent = Color(0xFF16845F)

    val Background = Color(0xFFF8F7F3)
    val Surface = Color(0xFFFEFEFD)
    val SoftNeutral = Color(0xFFF1F0EA)
    val Border = Color(0xFFEEEDE8)

    val TextPrimary = Color(0xFF172923)
    val TextSecondary = Color(0xFF5C6361)
    val TextMuted = Color(0xFF858B87)

    val Gold = Color(0xFFE0BC68)
    val GoldContainer = Color(0xFFFBF0D8)
    val OnGoldContainer = Color(0xFF4A3907)

    val Success = Color(0xFF16845F)
    val SuccessContainer = Color(0xFFDDEFE5)
    val Warning = Color(0xFF8A5A00)
    val WarningContainer = Color(0xFFFFF1D5)
    val Danger = Color(0xFFB42318)
    val DangerContainer = Color(0xFFFCE4E2)
    val NeutralContainer = Color(0xFFF1F0EA)

    val ChartGrid = Color(0xFFEEEDE8)
    val ChartLine = Color(0xFF082923)
    val ChartArea = Color(0x1F16845F)
    val ChartBar = Color(0xFF0F3B31)
    val ChartBarMuted = Color(0xFFBFD3CB)
}

object PosColorsDark {
    val ForestGreen = Color(0xFF10231E)
    val DeepGreen = Color(0xFF0A1714)
    val GreenElevated = Color(0xFF1B3B31)
    val GreenSoft = Color(0xFF16332A)

    val Secondary = Color(0xFF9CC6B8)
    val Accent = Color(0xFF6FCBA6)

    val Background = Color(0xFF0E1311)
    val Surface = Color(0xFF161C19)
    val SoftNeutral = Color(0xFF1D2421)
    val Border = Color(0xFF2A322E)

    val TextPrimary = Color(0xFFE8EDEA)
    val TextSecondary = Color(0xFFA2AAA6)
    val TextMuted = Color(0xFF7E8783)

    val Gold = Color(0xFFE0BC68)
    val GoldContainer = Color(0xFF3A3117)
    val OnGoldContainer = Color(0xFFF2E2B4)

    val Success = Color(0xFF6FCBA6)
    val SuccessContainer = Color(0xFF16382C)
    val Warning = Color(0xFFE0AE5C)
    val WarningContainer = Color(0xFF3C2F14)
    val Danger = Color(0xFFE99B96)
    val DangerContainer = Color(0xFF3E1F1D)
    val NeutralContainer = Color(0xFF232B27)

    val ChartGrid = Color(0xFF2A322E)
    val ChartLine = Color(0xFF6FCBA6)
    val ChartArea = Color(0x336FCBA6)
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
