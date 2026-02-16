package kr.jm.weddingledger.feature.schedule.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleEventRowDto(
    val id: String,
    @SerialName("wedding_id")
    val weddingId: String,
    val title: String,
    @SerialName("event_date")
    val eventDate: String,
    @SerialName("start_time")
    val startTime: String? = null,
    val location: String? = null,
    @SerialName("repeat_rule")
    val repeatRule: String? = null,
    val memo: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class ScheduleEventInsertDto(
    @SerialName("wedding_id")
    val weddingId: String,
    val title: String,
    @SerialName("event_date")
    val eventDate: String,
    @SerialName("start_time")
    val startTime: String? = null,
    val location: String? = null,
    @SerialName("repeat_rule")
    val repeatRule: String? = null,
    val memo: String? = null,
)
