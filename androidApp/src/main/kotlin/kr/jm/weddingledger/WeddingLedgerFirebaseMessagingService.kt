package kr.jm.weddingledger

import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.notifications.domain.repository.PartnerActivityNotifier
import org.koin.core.context.GlobalContext

class WeddingLedgerFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        if (token.isBlank()) return
        serviceScope.launch {
            runCatching {
                GlobalContext.get().get<PartnerActivityNotifier>().syncCurrentDevicePushToken()
            }
        }
    }
}
