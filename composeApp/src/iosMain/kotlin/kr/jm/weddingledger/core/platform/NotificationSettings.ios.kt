package kr.jm.weddingledger.core.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual fun openAppNotificationSettings(): Boolean {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return false
    val application = UIApplication.sharedApplication
    if (!application.canOpenURL(url)) return false
    return runCatching {
        application.openURL(url)
        true
    }.getOrDefault(false)
}
