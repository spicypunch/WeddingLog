package kr.jm.weddingledger.core.ui.keyboard

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun rememberImeVisible(): Boolean {
    val density = LocalDensity.current
    return WindowInsets.ime.getBottom(density) > 0
}

@Composable
fun keyboardAwareBottomPadding(
    normalBottomPadding: Dp,
    imeVisibleBottomPadding: Dp,
): Dp = if (rememberImeVisible()) imeVisibleBottomPadding else normalBottomPadding
