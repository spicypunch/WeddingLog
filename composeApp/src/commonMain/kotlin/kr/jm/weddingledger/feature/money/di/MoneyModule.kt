package kr.jm.weddingledger.feature.money.di

import kr.jm.weddingledger.feature.money.data.repository.MoneyRepositoryImpl
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository
import kr.jm.weddingledger.feature.money.domain.usecase.AddBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.AddExpenseUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.FetchMoneySnapshotUseCase
import kr.jm.weddingledger.feature.money.presentation.MoneyViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val moneyModule = module {
    single<MoneyRepository> { MoneyRepositoryImpl(get()) }

    factory { FetchMoneySnapshotUseCase(get()) }
    factory { AddBudgetItemUseCase(get()) }
    factory { AddExpenseUseCase(get()) }

    viewModel { MoneyViewModel(get(), get(), get()) }
}

