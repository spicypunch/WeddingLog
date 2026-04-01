package kr.jm.weddingledger.feature.notifications.di

import kr.jm.weddingledger.feature.notifications.data.repository.NotificationCenterRepositoryImpl
import kr.jm.weddingledger.feature.notifications.data.repository.PartnerActivityNotifierImpl
import kr.jm.weddingledger.feature.notifications.data.platform.createPartnerActivityHttpClient
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository
import kr.jm.weddingledger.feature.notifications.domain.repository.PartnerActivityNotifier
import kr.jm.weddingledger.feature.notifications.domain.usecase.CancelReminderUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.FetchNotificationFeedUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.FetchScheduledReminderIdsUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.MarkActivityItemsReadUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.ScheduleReminderUseCase
import kr.jm.weddingledger.feature.notifications.presentation.NotificationCenterViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val notificationsModule = module {
    single { createPartnerActivityHttpClient() }
    single<PartnerActivityNotifier> { PartnerActivityNotifierImpl(get(), get()) }
    single<NotificationCenterRepository> { NotificationCenterRepositoryImpl(get(), get(), get(), get()) }

    factory { FetchNotificationFeedUseCase(get()) }
    factory { ScheduleReminderUseCase(get()) }
    factory { CancelReminderUseCase(get()) }
    factory { FetchScheduledReminderIdsUseCase(get()) }
    factory { MarkActivityItemsReadUseCase(get()) }

    viewModel { NotificationCenterViewModel(get(), get(), get(), get(), get()) }
}
