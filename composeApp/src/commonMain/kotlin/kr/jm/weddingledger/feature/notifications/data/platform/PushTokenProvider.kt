package kr.jm.weddingledger.feature.notifications.data.platform

enum class PushPlatform(val dbValue: String) {
    Android("android"),
    Ios("ios"),
}

expect suspend fun fetchRemotePushTokenOrNull(): String?
expect fun currentPushPlatform(): PushPlatform
