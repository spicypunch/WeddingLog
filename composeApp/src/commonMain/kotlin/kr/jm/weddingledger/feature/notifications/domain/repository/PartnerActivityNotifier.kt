package kr.jm.weddingledger.feature.notifications.domain.repository

interface PartnerActivityNotifier {
    suspend fun syncCurrentDevicePushToken()
    suspend fun deactivateCurrentDevicePushToken()
    suspend fun notifyExpenseAdded(expenseId: String)
    suspend fun notifyScheduleAdded(eventId: String)
}
