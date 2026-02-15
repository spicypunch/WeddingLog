package kr.jm.weddingledger.core.di

import kr.jm.weddingledger.feature.auth.di.authModule
import kr.jm.weddingledger.feature.money.di.moneyModule
import kr.jm.weddingledger.feature.onboarding.di.onboardingModule
import kr.jm.weddingledger.feature.schedule.di.scheduleModule
import org.koin.core.module.Module

fun appModules(): List<Module> = listOf(
    coreModule,
    authModule,
    onboardingModule,
    moneyModule,
    scheduleModule,
)
