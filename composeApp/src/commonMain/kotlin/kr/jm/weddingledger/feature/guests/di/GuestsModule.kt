package kr.jm.weddingledger.feature.guests.di

import kr.jm.weddingledger.feature.guests.data.repository.GuestRepositoryImpl
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository
import kr.jm.weddingledger.feature.guests.domain.usecase.AddGuestUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.DeleteGuestUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.FetchGuestSettingsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.FetchGuestsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.SetGuestAttendanceUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.UpdateGuestSettingsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.UpdateGuestUseCase
import kr.jm.weddingledger.feature.guests.presentation.GuestsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val guestsModule = module {
    single<GuestRepository> { GuestRepositoryImpl(get()) }

    factory { FetchGuestsUseCase(get()) }
    factory { FetchGuestSettingsUseCase(get()) }
    factory { UpdateGuestSettingsUseCase(get()) }
    factory { AddGuestUseCase(get()) }
    factory { UpdateGuestUseCase(get()) }
    factory { DeleteGuestUseCase(get()) }
    factory { SetGuestAttendanceUseCase(get()) }

    viewModel { GuestsViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
