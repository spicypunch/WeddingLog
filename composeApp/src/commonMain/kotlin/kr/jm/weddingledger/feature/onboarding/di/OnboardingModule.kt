package kr.jm.weddingledger.feature.onboarding.di

import kr.jm.weddingledger.feature.onboarding.data.repository.OnboardingRepositoryImpl
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository
import kr.jm.weddingledger.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.ConnectPartnerUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.GetOrCreateInviteCodeUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.UpdateMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val onboardingModule = module {
    single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }

    factory { FetchMyProfileUseCase(get()) }
    factory { UpdateMyProfileUseCase(get()) }
    factory { GetOrCreateInviteCodeUseCase(get()) }
    factory { ConnectPartnerUseCase(get()) }
    factory { CompleteOnboardingUseCase(get()) }

    viewModel { OnboardingViewModel(get(), get(), get(), get(), get()) }
}

