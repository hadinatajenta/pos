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

val DefaultPosSemanticPalette = PosSemanticPalette(
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

val LocalPosSemanticPalette = staticCompositionLocalOf { DefaultPosSemanticPalette }
