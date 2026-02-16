package kr.jm.weddingledger

import kr.jm.weddingledger.feature.auth.presentation.AuthDeepLinkBridge

fun pushIncomingDeepLink(url: String) {
    AuthDeepLinkBridge.emit(url)
}
