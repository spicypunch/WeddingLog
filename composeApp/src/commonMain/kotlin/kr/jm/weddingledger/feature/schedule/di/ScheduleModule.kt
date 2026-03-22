package kr.jm.weddingledger.feature.schedule.di

import kr.jm.weddingledger.feature.schedule.data.repository.ChecklistRepositoryImpl
import kr.jm.weddingledger.feature.schedule.data.repository.ScheduleEventRepositoryImpl
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository
import kr.jm.weddingledger.feature.schedule.domain.repository.ScheduleEventRepository
import kr.jm.weddingledger.feature.schedule.domain.usecase.AddChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.AddScheduleEventUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.DeleteChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.DeleteScheduleEventUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchScheduleEventsUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.SetChecklistCheckedUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.UpdateChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.UpdateScheduleEventUseCase
import kr.jm.weddingledger.feature.schedule.presentation.ScheduleViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val scheduleModule = module {
    single<ChecklistRepository> { ChecklistRepositoryImpl(get()) }
    single<ScheduleEventRepository> { ScheduleEventRepositoryImpl(get(), get()) }

    factory { FetchChecklistUseCase(get()) }
    factory { AddChecklistUseCase(get()) }
    factory { UpdateChecklistUseCase(get()) }
    factory { SetChecklistCheckedUseCase(get()) }
    factory { DeleteChecklistUseCase(get()) }
    factory { FetchScheduleEventsUseCase(get()) }
    factory { AddScheduleEventUseCase(get()) }
    factory { UpdateScheduleEventUseCase(get()) }
    factory { DeleteScheduleEventUseCase(get()) }

    viewModel { ScheduleViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
