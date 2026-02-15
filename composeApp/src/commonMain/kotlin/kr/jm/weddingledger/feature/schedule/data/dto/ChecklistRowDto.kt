package kr.jm.weddingledger.feature.schedule.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChecklistRowDto(
    val id: String,
    @SerialName("wedding_id")
    val weddingId: String,
    val title: String,
    @SerialName("group_title")
    val groupTitle: String? = null,
    @SerialName("d_day_tag")
    val dDayTag: String? = null,
    @SerialName("is_checked")
    val isChecked: Boolean = false,
    @SerialName("target_date")
    val targetDate: String? = null,
    val memo: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class ChecklistInsertDto(
    @SerialName("wedding_id")
    val weddingId: String,
    val title: String,
    @SerialName("group_title")
    val groupTitle: String? = null,
    @SerialName("d_day_tag")
    val dDayTag: String? = null,
    @SerialName("is_checked")
    val isChecked: Boolean = false,
    @SerialName("target_date")
    val targetDate: String? = null,
    val memo: String? = null,
)

