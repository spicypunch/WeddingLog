package kr.jm.weddingledger.feature.notifications.data.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.notifications.domain.repository.PartnerActivityNotifier
import org.koin.mp.KoinPlatformTools

private val pushTokenBridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun syncPartnerPushTokenFromHost() {
    val koin = runCatching { KoinPlatformTools.defaultContext().get() }.getOrNull() ?: return
    val notifier = runCatching { koin.get<PartnerActivityNotifier>() }.getOrNull() ?: return
    pushTokenBridgeScope.launch {
        notifier.syncCurrentDevicePushToken()
    }
}
