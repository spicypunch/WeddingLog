package kr.jm.weddingledger.feature.notifications.domain.repository

import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedItem
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationReminderPlan

interface NotificationCenterRepository {
    suspend fun fetchFeed(): NotificationCenterResult<List<NotificationFeedItem>>
    suspend fun scheduleReminder(plan: NotificationReminderPlan): NotificationCenterResult<Unit>
    suspend fun cancelReminder(notificationId: String): NotificationCenterResult<Unit>
    suspend fun fetchScheduledReminderIds(): NotificationCenterResult<Set<String>>
}
