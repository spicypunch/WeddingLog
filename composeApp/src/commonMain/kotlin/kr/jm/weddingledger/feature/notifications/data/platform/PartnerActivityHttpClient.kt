package kr.jm.weddingledger.feature.notifications.data.platform

import io.ktor.client.HttpClient

expect fun createPartnerActivityHttpClient(): HttpClient
