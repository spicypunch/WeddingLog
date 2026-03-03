package kr.jm.weddingledger.feature.auth.di

import kr.jm.weddingledger.feature.auth.data.repository.AuthRepositoryImpl
import kr.jm.weddingledger.feature.auth.data.platform.createKakaoNativeSignInClient
import kr.jm.weddingledger.feature.auth.domain.usecase.HandleAuthDeepLinkUseCase
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository
import kr.jm.weddingledger.feature.auth.domain.usecase.GetCurrentUserUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ObserveAuthStateUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResendSignUpVerificationEmailUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResetPasswordUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SendEmailOtpUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInWithAppleUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInWithGoogleUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInWithKakaoUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignOutUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignUpUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.VerifyEmailOtpUseCase
import kr.jm.weddingledger.feature.auth.presentation.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single { createKakaoNativeSignInClient() }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    
    factory { SignUpUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { HandleAuthDeepLinkUseCase(get()) }
    factory { ResetPasswordUseCase(get()) }
    factory { ResendSignUpVerificationEmailUseCase(get()) }
    factory { SignInWithGoogleUseCase(get()) }
    factory { SignInWithAppleUseCase(get()) }
    factory { SignInWithKakaoUseCase(get()) }
    factory { SendEmailOtpUseCase(get()) }
    factory { VerifyEmailOtpUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { ObserveAuthStateUseCase(get()) }

    viewModel {
        AuthViewModel(
            signInUseCase = get(),
            signInWithKakaoUseCase = get(),
            signInWithGoogleUseCase = get(),
            signInWithAppleUseCase = get(),
            sendEmailOtpUseCase = get(),
            verifyEmailOtpUseCase = get(),
            resetPasswordUseCase = get(),
            resendSignUpVerificationEmailUseCase = get(),
        )
    }
}
