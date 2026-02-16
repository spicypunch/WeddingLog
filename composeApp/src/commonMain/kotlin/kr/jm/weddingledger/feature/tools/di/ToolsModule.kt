package kr.jm.weddingledger.feature.tools.di

import kr.jm.weddingledger.feature.tools.data.repository.ToolsRepositoryImpl
import kr.jm.weddingledger.feature.tools.domain.repository.ToolsRepository
import kr.jm.weddingledger.feature.tools.domain.usecase.FetchValuesSurveyAnswersUseCase
import kr.jm.weddingledger.feature.tools.domain.usecase.FetchVenueFavoritesUseCase
import kr.jm.weddingledger.feature.tools.domain.usecase.FetchVenuesUseCase
import kr.jm.weddingledger.feature.tools.domain.usecase.SaveValuesSurveyAnswersUseCase
import kr.jm.weddingledger.feature.tools.domain.usecase.SetVenueFavoriteUseCase
import kr.jm.weddingledger.feature.tools.presentation.ValuesSurveyViewModel
import kr.jm.weddingledger.feature.tools.presentation.VenueComparisonViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val toolsModule = module {
    single<ToolsRepository> { ToolsRepositoryImpl(get()) }

    factory { FetchVenuesUseCase(get()) }
    factory { FetchVenueFavoritesUseCase(get()) }
    factory { SetVenueFavoriteUseCase(get()) }
    factory { FetchValuesSurveyAnswersUseCase(get()) }
    factory { SaveValuesSurveyAnswersUseCase(get()) }

    viewModel { VenueComparisonViewModel(get(), get(), get()) }
    viewModel { ValuesSurveyViewModel(get(), get()) }
}
