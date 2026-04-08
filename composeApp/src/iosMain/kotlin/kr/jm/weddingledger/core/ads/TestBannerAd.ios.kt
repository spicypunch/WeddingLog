package kr.jm.weddingledger.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import platform.Foundation.NSNotificationCenter

private const val IOS_SHOW_HOST_BANNER_NOTIFICATION = "WeddingLedgerShowHostBanner"
private const val IOS_HIDE_HOST_BANNER_NOTIFICATION = "WeddingLedgerHideHostBanner"

@Composable
actual fun TestBannerAd(modifier: Modifier) = Unit

actual fun syncHostBannerVisibility(visible: Boolean) {
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = if (visible) IOS_SHOW_HOST_BANNER_NOTIFICATION else IOS_HIDE_HOST_BANNER_NOTIFICATION,
        `object` = null,
    )
}
