package kr.jm.weddingledger.feature.schedule.domain.model

import kotlinx.datetime.LocalDate

data class ChecklistTask(
    val id: String,
    val title: String,
    val dTag: String?,
    val groupTitle: String?,
    val isChecked: Boolean,
    val targetDate: LocalDate?,
    val memo: String?,
)

