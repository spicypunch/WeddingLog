package kr.jm.weddingledger.feature.notifications.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DevicePushTokenSyncRequest(
    val action: String,
    @SerialName("installation_id")
    val installationId: String,
    val platform: String? = null,
    @SerialName("push_token")
    val pushToken: String? = null,
)

@Serializable
data class PartnerActivityDispatchRequest(
    val type: String,
    @SerialName("entity_id")
    val entityId: String,
)

@Serializable
data class PartnerActivityNotificationRowDto(
    val id: String,
    @SerialName("wedding_id")
    val weddingId: String,
    @SerialName("actor_user_id")
    val actorUserId: String,
    @SerialName("target_user_id")
    val targetUserId: String,
    val type: String,
    @SerialName("entity_id")
    val entityId: String,
    val title: String,
    val body: String,
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("pushed_at")
    val pushedAt: String? = null,
)
