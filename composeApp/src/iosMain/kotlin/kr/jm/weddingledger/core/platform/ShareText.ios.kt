package kr.jm.weddingledger.core.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

actual fun shareText(text: String) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return

    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    val presenter = topViewController(root)
    val sheet = UIActivityViewController(
        activityItems = listOf(trimmed),
        applicationActivities = null,
    )

    runCatching { presenter.presentViewController(sheet, animated = true, completion = null) }
}

private tailrec fun topViewController(viewController: UIViewController): UIViewController {
    val presented = viewController.presentedViewController
    return if (presented != null) topViewController(presented) else viewController
}
