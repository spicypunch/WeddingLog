package kr.jm.weddingledger.core.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIColor
import platform.UIKit.UIView

private const val IOS_ATTACH_BANNER_NOTIFICATION = "WeddingLedgerAdMobBannerAttach"
private const val IOS_DETACH_BANNER_NOTIFICATION = "WeddingLedgerAdMobBannerDetach"

@Composable
actual fun AdMobBanner(modifier: Modifier) {
    UIKitView(
        factory = {
            UIView().apply {
                backgroundColor = UIColor.clearColor
                postAttachBannerView(this)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        update = { view -> postAttachBannerView(view) },
        onRelease = { view -> postDetachBannerView(view) },
    )
}

private fun postAttachBannerView(containerView: UIView) {
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = IOS_ATTACH_BANNER_NOTIFICATION,
        `object` = containerView,
    )
}

private fun postDetachBannerView(containerView: UIView) {
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = IOS_DETACH_BANNER_NOTIFICATION,
        `object` = containerView,
    )
}
