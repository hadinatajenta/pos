package id.umkm.pos.pro.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object PosColors {
    val ForestGreen = Color(0xFF0D5C46)
    val DeepGreen = Color(0xFF083D2E)
    val GreenElevated = Color(0xFF127357)
    val GreenSoft = Color(0xFFE3F5EE)

    val Secondary = Color(0xFF156E54)
    val Accent = Color(0xFF108562)

    val Background = Color(0xFFF6F8F7)
    val Surface = Color(0xFFFFFFFF)
    val SoftNeutral = Color(0xFFEEF2F0)
    val Border = Color(0xFFE4EAE7)

    val TextPrimary = Color(0xFF111C18)
    val TextSecondary = Color(0xFF52605B)
    val TextMuted = Color(0xFF7A8782)
    val TextInk = Color(0xFF18221F)

    val Gold = Color(0xFFD97706)
    val GoldContainer = Color(0xFFFEF3C7)
    val OnGoldContainer = Color(0xFF78350F)

    val Success = Color(0xFF0E835C)
    val SuccessContainer = Color(0xFFE2F6EE)
    val Warning = Color(0xFFB45309)
    val WarningContainer = Color(0xFFFEF3C7)
    val Danger = Color(0xFFDC2626)
    val DangerContainer = Color(0xFFFEE2E2)
    val NeutralContainer = Color(0xFFEEF2F0)

    val SuccessText = Color(0xFF0B6B4A)
    val WarningText = Color(0xFF92400E)
    val DangerText = Color(0xFFB91C1C)

    val ChartGrid = Color(0xFFE4EAE7)
    val ChartLine = Color(0xFF0D5C46)
    val ChartArea = Color(0x1F108562)
    val ChartBar = Color(0xFF0D5C46)
    val ChartBarMuted = Color(0xFFB8D4C9)
}

object PosColorsDark {
    val ForestGreen = Color(0xFF102620)
    val DeepGreen = Color(0xFF0A1814)
    val GreenElevated = Color(0xFF193D32)
    val GreenSoft = Color(0xFF15352B)

    val Secondary = Color(0xFF8CD4BC)
    val Accent = Color(0xFF5CE0B5)

    val Background = Color(0xFF0D1210)
    val Surface = Color(0xFF151C19)
    val SoftNeutral = Color(0xFF1E2623)
    val Border = Color(0xFF28322E)

    val TextPrimary = Color(0xFFECF2EF)
    val TextSecondary = Color(0xFF9BA8A3)
    val TextMuted = Color(0xFF75827D)

    val Gold = Color(0xFFF59E0B)
    val GoldContainer = Color(0xFF3B2A10)
    val OnGoldContainer = Color(0xFFFDE68A)

    val Success = Color(0xFF5CE0B5)
    val SuccessContainer = Color(0xFF13392C)
    val Warning = Color(0xFFFBBF24)
    val WarningContainer = Color(0xFF3B2A10)
    val Danger = Color(0xFFF87171)
    val DangerContainer = Color(0xFF3F1D1D)
    val NeutralContainer = Color(0xFF222B27)

    val SuccessText = Success
    val WarningText = Warning
    val DangerText = Danger

    val ChartGrid = Color(0xFF28322E)
    val ChartLine = Color(0xFF5CE0B5)
    val ChartArea = Color(0x335CE0B5)
    val ChartBar = Color(0xFF349B7A)
    val ChartBarMuted = Color(0xFF26473C)
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
    val successText: Color,
    val warningText: Color,
    val dangerText: Color,
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
    successText = PosColors.SuccessText,
    warningText = PosColors.WarningText,
    dangerText = PosColors.DangerText,
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
    successText = PosColorsDark.SuccessText,
    warningText = PosColorsDark.WarningText,
    dangerText = PosColorsDark.DangerText,
    chartGrid = PosColorsDark.ChartGrid,
    chartLine = PosColorsDark.ChartLine,
    chartArea = PosColorsDark.ChartArea,
    chartBar = PosColorsDark.ChartBar,
    chartBarMuted = PosColorsDark.ChartBarMuted,
)

val LocalPosSemanticPalette = staticCompositionLocalOf { PosLightSemanticPalette }
