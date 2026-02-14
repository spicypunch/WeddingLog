package kr.jm.weddingledger.feature.auth.di

import kr.jm.weddingledger.feature.auth.data.repository.AuthRepositoryImpl
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository
import kr.jm.weddingledger.feature.auth.domain.usecase.GetCurrentUserUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ObserveAuthStateUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResetPasswordUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignOutUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignUpUseCase
import kr.jm.weddingledger.feature.auth.presentation.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    
    factory { SignUpUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { ResetPasswordUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { ObserveAuthStateUseCase(get()) }

    viewModel { AuthViewModel(get(), get(), get()) }
}
