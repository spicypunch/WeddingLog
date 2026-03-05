package kr.jm.weddingledger.feature.money.di

import kr.jm.weddingledger.feature.money.data.repository.MoneyFilterPreferenceRepositoryImpl
import kr.jm.weddingledger.feature.money.data.repository.MoneyRepositoryImpl
import kr.jm.weddingledger.feature.money.domain.repository.MoneyFilterPreferenceRepository
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository
import kr.jm.weddingledger.feature.money.domain.usecase.AddCategoryUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.AddBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.AddExpenseUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.DeleteBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.DeleteExpenseUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.FetchExpenseFilterPreferenceUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.FetchMoneySnapshotUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.SaveExpenseFilterPreferenceUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.UpdateBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.UpdateCategoryUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.UpdateExpenseUseCase
import kr.jm.weddingledger.feature.money.presentation.MoneyViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val moneyModule = module {
    single<MoneyRepository> { MoneyRepositoryImpl(get()) }
    single<MoneyFilterPreferenceRepository> { MoneyFilterPreferenceRepositoryImpl() }

    factory { FetchMoneySnapshotUseCase(get()) }
    factory { FetchExpenseFilterPreferenceUseCase(get()) }
    factory { SaveExpenseFilterPreferenceUseCase(get()) }
    factory { AddCategoryUseCase(get()) }
    factory { AddBudgetItemUseCase(get()) }
    factory { AddExpenseUseCase(get()) }
    factory { UpdateCategoryUseCase(get()) }
    factory { UpdateBudgetItemUseCase(get()) }
    factory { UpdateExpenseUseCase(get()) }
    factory { DeleteBudgetItemUseCase(get()) }
    factory { DeleteExpenseUseCase(get()) }

    viewModel { MoneyViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
