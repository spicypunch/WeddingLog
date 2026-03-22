package kr.jm.weddingledger.feature.notifications.data.platform

import platform.Foundation.NSUserDefaults

private const val PUSH_TOKEN_KEY: String = "wl_partner_activity_fcm_token"

actual suspend fun fetchRemotePushTokenOrNull(): String? {
    return NSUserDefaults.standardUserDefaults.stringForKey(PUSH_TOKEN_KEY)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

actual fun currentPushPlatform(): PushPlatform = PushPlatform.Ios
