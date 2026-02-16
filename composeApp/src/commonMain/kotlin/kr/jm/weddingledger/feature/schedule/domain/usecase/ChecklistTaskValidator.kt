package kr.jm.weddingledger.feature.schedule.domain.usecase

import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult

internal fun validateChecklistTask(task: NewChecklistTask): ScheduleResult.Error? {
    val title = task.title.trim()
    if (title.isEmpty()) return ScheduleResult.Error("할 일 제목을 입력해주세요.")
    if (title.length < 2) return ScheduleResult.Error("할 일 제목은 2자 이상 입력해주세요.")
    if (title.length > 40) return ScheduleResult.Error("할 일 제목은 40자 이하로 입력해주세요.")

    val normalizedDTag = task.dTag?.trim()?.uppercase().orEmpty()
    if (normalizedDTag.isEmpty()) {
        return ScheduleResult.Error("D-Tag를 선택해주세요.")
    }
    if (!normalizedDTag.startsWith("D-")) {
        return ScheduleResult.Error("D-Tag 형식이 올바르지 않습니다. (예: D-30)")
    }
    val dTagNumber = normalizedDTag.removePrefix("D-").toIntOrNull()
    if (dTagNumber == null || dTagNumber !in 0..365) {
        return ScheduleResult.Error("D-Tag는 D-0부터 D-365 사이여야 합니다.")
    }

    val memoLength = task.memo?.trim()?.length ?: 0
    if (memoLength > 120) return ScheduleResult.Error("메모는 120자 이하로 입력해주세요.")

    return null
}
