package kr.jm.weddingledger.feature.tools.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValuesSurveyAnswerRowDto(
    @SerialName("question_id")
    val questionId: String,
    @SerialName("option_id")
    val optionId: String,
    val score: Int,
)

@Serializable
data class ValuesSurveyAnswerInsertDto(
    @SerialName("wedding_id")
    val weddingId: String,
    @SerialName("question_id")
    val questionId: String,
    @SerialName("option_id")
    val optionId: String,
    val score: Int,
)

