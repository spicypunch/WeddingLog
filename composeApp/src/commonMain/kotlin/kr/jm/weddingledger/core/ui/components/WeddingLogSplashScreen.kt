package kr.jm.weddingledger.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle

@Composable
fun WeddingLogSplashScreen(
    themeStyle: WeddingLogThemeStyle,
) {
    val palette = remember(themeStyle) { splashPalette(themeStyle) }
    val transition = rememberInfiniteTransition(label = "splash")
    val dotOne = transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, delayMillis = 0),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotOne",
    )
    val dotTwo = transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, delayMillis = 150),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotTwo",
    )
    val dotThree = transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, delayMillis = 300),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotThree",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.backgroundTop,
                        palette.backgroundBottom,
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-84).dp, y = (-40).dp)
                .clip(CircleShape)
                .background(palette.glowTop)
                .align(Alignment.TopStart)
        )
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(x = 96.dp, y = 120.dp)
                .clip(CircleShape)
                .background(palette.glowBottom)
                .align(Alignment.BottomEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(156.dp)
                            .clip(CircleShape)
                            .background(palette.iconHalo)
                    )
                    Surface(
                        modifier = Modifier.size(132.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.96f),
                        shadowElevation = 10.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(56.dp),
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = palette.accent.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-18).dp, y = 8.dp),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = palette.accent.copy(alpha = 0.35f),
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.CenterStart)
                            .offset(x = 2.dp, y = 8.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "WeddingLog",
                        color = palette.accent,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Text(
                        text = "로맨틱하고 부드러운 웨딩 로그",
                        color = palette.subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SplashDot(alpha = dotOne.value, color = palette.accent)
                    SplashDot(alpha = dotTwo.value, color = palette.accent)
                    SplashDot(alpha = dotThree.value, color = palette.accent)
                }
                Text(
                    text = "BEGINNING FOREVER",
                    color = palette.tagline,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SplashDot(
    alpha: Float,
    color: Color,
) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

private data class SplashPalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val glowTop: Color,
    val glowBottom: Color,
    val accent: Color,
    val iconHalo: Color,
    val subtitle: Color,
    val tagline: Color,
)

private fun splashPalette(
    themeStyle: WeddingLogThemeStyle,
): SplashPalette {
    return when (themeStyle) {
        WeddingLogThemeStyle.Minimal -> SplashPalette(
            backgroundTop = Color(0xFFFFF8F8),
            backgroundBottom = Color(0xFFFFF4F4),
            glowTop = Color(0xFFFFE3E3),
            glowBottom = Color(0xFFFFEEEE),
            accent = Color(0xFFFF7F86),
            iconHalo = Color(0xFFFFE7E8),
            subtitle = Color(0xFFFFA4A9),
            tagline = Color(0xFFFFB7BC),
        )

        WeddingLogThemeStyle.Lovely -> SplashPalette(
            backgroundTop = Color(0xFFFFF6F7),
            backgroundBottom = Color(0xFFFFF0F1),
            glowTop = Color(0xFFFFE1E4),
            glowBottom = Color(0xFFFFEBEE),
            accent = Color(0xFFFF8288),
            iconHalo = Color(0xFFFFE5E8),
            subtitle = Color(0xFFFFA8AF),
            tagline = Color(0xFFFFB9BE),
        )
    }
}
