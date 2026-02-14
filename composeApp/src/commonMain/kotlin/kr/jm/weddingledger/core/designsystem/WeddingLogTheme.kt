package kr.jm.weddingledger.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Font
import weddingledger.composeapp.generated.resources.Res
import weddingledger.composeapp.generated.resources.pretendard_bold
import weddingledger.composeapp.generated.resources.pretendard_medium
import weddingledger.composeapp.generated.resources.pretendard_regular
import weddingledger.composeapp.generated.resources.pretendard_semibold

enum class WeddingLogThemeStyle {
    Minimal,
    Lovely
}

private val MinimalLightColorScheme = lightColorScheme(
    primary = Color(0xFFEF3948),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F6F6),
    onBackground = Color(0xFF1F2937),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE5E7EB),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

private val LovelyLightColorScheme = lightColorScheme(
    primary = Color(0xFFFF8FA3),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF0F3),
    onBackground = Color(0xFF590D22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF590D22),
    surfaceVariant = Color(0xFFFFE3EA),
    onSurfaceVariant = Color(0xFF7A2D3A),
    outline = Color(0xFFF2C6D0),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

private val WeddingLogShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

@Composable
private fun rememberPretendardFontFamily(): FontFamily {
    // Font(...) is @Composable in Compose Multiplatform resources.
    val regular = Font(Res.font.pretendard_regular, FontWeight.Normal, FontStyle.Normal)
    val medium = Font(Res.font.pretendard_medium, FontWeight.Medium, FontStyle.Normal)
    val semibold = Font(Res.font.pretendard_semibold, FontWeight.SemiBold, FontStyle.Normal)
    val bold = Font(Res.font.pretendard_bold, FontWeight.Bold, FontStyle.Normal)

    return remember(regular, medium, semibold, bold) {
        FontFamily(regular, medium, semibold, bold)
    }
}

@Composable
private fun rememberWeddingLogTypography(): Typography {
    val fontFamily = rememberPretendardFontFamily()
    val base = Typography()
    return remember(base, fontFamily) { base.withFontFamily(fontFamily) }
}

@Composable
fun WeddingLogTheme(
    style: WeddingLogThemeStyle,
    content: @Composable () -> Unit
) {
    val colorScheme = when (style) {
        WeddingLogThemeStyle.Minimal -> MinimalLightColorScheme
        WeddingLogThemeStyle.Lovely -> LovelyLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = rememberWeddingLogTypography(),
        shapes = WeddingLogShapes,
        content = content
    )
}

private fun Typography.withFontFamily(fontFamily: FontFamily): Typography = Typography(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)
