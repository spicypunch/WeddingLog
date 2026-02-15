package kr.jm.weddingledger.feature.schedule.di

import kr.jm.weddingledger.feature.schedule.data.repository.ChecklistRepositoryImpl
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository
import kr.jm.weddingledger.feature.schedule.domain.usecase.AddChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.DeleteChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.SetChecklistCheckedUseCase
import kr.jm.weddingledger.feature.schedule.presentation.ScheduleViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val scheduleModule = module {
    single<ChecklistRepository> { ChecklistRepositoryImpl(get()) }

    factory { FetchChecklistUseCase(get()) }
    factory { AddChecklistUseCase(get()) }
    factory { SetChecklistCheckedUseCase(get()) }
    factory { DeleteChecklistUseCase(get()) }

    viewModel { ScheduleViewModel(get(), get(), get(), get()) }
}

