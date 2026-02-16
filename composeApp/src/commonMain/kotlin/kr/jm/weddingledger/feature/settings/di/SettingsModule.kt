package kr.jm.weddingledger.feature.settings.di

import kr.jm.weddingledger.feature.settings.presentation.ProfileEditViewModel
import kr.jm.weddingledger.feature.settings.presentation.AccountSettingsViewModel
import kr.jm.weddingledger.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { ProfileEditViewModel(get(), get()) }
    viewModel { (email: String) ->
        AccountSettingsViewModel(
            email = email,
            resetPasswordUseCase = get(),
            resendSignUpVerificationEmailUseCase = get(),
        )
    }
}
