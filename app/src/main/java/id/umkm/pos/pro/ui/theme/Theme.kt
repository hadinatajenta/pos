package id.umkm.pos.pro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

private val DarkColors = darkColorScheme(
    primary = PosColorsDark.Success,
    onPrimary = Color(0xFF06322A),
    primaryContainer = PosColorsDark.GreenElevated,
    onPrimaryContainer = Color(0xFFC7EDDF),
    inversePrimary = PosColorsDark.Success,
    secondary = PosColorsDark.Success,
    onSecondary = Color(0xFF06322A),
    secondaryContainer = PosColorsDark.SuccessContainer,
    onSecondaryContainer = Color(0xFFC7EDDF),
    tertiary = PosColorsDark.Gold,
    onTertiary = PosColorsDark.OnGoldContainer,
    tertiaryContainer = PosColorsDark.GoldContainer,
    onTertiaryContainer = PosColorsDark.OnGoldContainer,
    error = PosColorsDark.Danger,
    onError = Color(0xFF3E1211),
    errorContainer = PosColorsDark.DangerContainer,
    onErrorContainer = Color(0xFFF7DAD8),
    background = PosColorsDark.Background,
    onBackground = PosColorsDark.TextPrimary,
    surface = PosColorsDark.Surface,
    onSurface = PosColorsDark.TextPrimary,
    surfaceVariant = PosColorsDark.SoftNeutral,
    onSurfaceVariant = PosColorsDark.TextSecondary,
    surfaceContainerLowest = Color(0xFF0A100D),
    surfaceContainerLow = Color(0xFF121915),
    surfaceContainer = PosColorsDark.SoftNeutral,
    surfaceContainerHigh = Color(0xFF232C27),
    surfaceContainerHighest = PosColorsDark.Border,
    outline = Color(0xFF3C463F),
    outlineVariant = PosColorsDark.Border,
    inverseSurface = PosColorsDark.TextPrimary,
    inverseOnSurface = PosColorsDark.Background,
    scrim = Color.Black,
)

@Composable
fun UmkmPosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }
    val semanticPalette = if (darkTheme) PosDarkSemanticPalette else PosLightSemanticPalette
    CompositionLocalProvider(
        LocalAppTextStyles provides PosTextStyles,
        LocalPosSemanticPalette provides semanticPalette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
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
