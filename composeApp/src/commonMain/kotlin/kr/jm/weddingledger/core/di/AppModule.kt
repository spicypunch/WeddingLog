package kr.jm.weddingledger.core.di

import kr.jm.weddingledger.feature.auth.di.authModule
import kr.jm.weddingledger.feature.guests.di.guestsModule
import kr.jm.weddingledger.feature.home.di.homeModule
import kr.jm.weddingledger.feature.money.di.moneyModule
import kr.jm.weddingledger.feature.notifications.di.notificationsModule
import kr.jm.weddingledger.feature.onboarding.di.onboardingModule
import kr.jm.weddingledger.feature.schedule.di.scheduleModule
import kr.jm.weddingledger.feature.settings.di.settingsModule
import kr.jm.weddingledger.feature.tools.di.toolsModule
import org.koin.core.module.Module

fun appModules(): List<Module> = listOf(
    coreModule,
    authModule,
    onboardingModule,
    homeModule,
    moneyModule,
    notificationsModule,
    scheduleModule,
    guestsModule,
    settingsModule,
    toolsModule,
)
