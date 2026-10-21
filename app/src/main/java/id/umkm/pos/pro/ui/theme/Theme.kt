package id.umkm.pos.pro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PosColors.ForestGreen,
    onPrimary = Color.White,
    primaryContainer = PosColors.GreenSoft,
    onPrimaryContainer = PosColors.DeepGreen,
    inversePrimary = PosColors.GreenElevated,
    secondary = PosColors.Success,
    onSecondary = Color.White,
    secondaryContainer = PosColors.SuccessContainer,
    onSecondaryContainer = PosColors.DeepGreen,
    tertiary = PosColors.Gold,
    onTertiary = PosColors.OnGoldContainer,
    tertiaryContainer = PosColors.GoldContainer,
    onTertiaryContainer = PosColors.OnGoldContainer,
    error = PosColors.Danger,
    onError = Color.White,
    errorContainer = PosColors.DangerContainer,
    onErrorContainer = Color(0xFF5B1917),
    background = PosColors.Background,
    onBackground = PosColors.TextPrimary,
    surface = PosColors.Surface,
    onSurface = PosColors.TextPrimary,
    surfaceVariant = PosColors.SoftNeutral,
    onSurfaceVariant = PosColors.TextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FBFA),
    surfaceContainer = PosColors.SoftNeutral,
    surfaceContainerHigh = Color(0xFFE6ECE9),
    surfaceContainerHighest = PosColors.Border,
    outline = Color(0xFFCBD5D0),
    outlineVariant = PosColors.Border,
    inverseSurface = PosColors.TextPrimary,
    inverseOnSurface = PosColors.Background,
    scrim = Color.Black,
)

@Composable
fun UmkmPosTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppTextStyles provides PosTextStyles,
        LocalPosSemanticPalette provides DefaultPosSemanticPalette,
    ) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = PosTypography,
            shapes = PosShapes,
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides PosTypography.bodyMedium,
                content = content,
            )
        }
    }
}
