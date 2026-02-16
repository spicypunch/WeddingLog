package kr.jm.weddingledger.feature.auth.presentation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AuthDeepLinkBridge {
    private val _deepLinks = MutableSharedFlow<String>(extraBufferCapacity = 32)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val deepLinks: SharedFlow<String> = _deepLinks.asSharedFlow()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun emit(url: String) {
        val normalized = url.trim()
        if (normalized.isNotEmpty()) {
            _deepLinks.tryEmit(normalized)
        }
    }

    fun emitMessage(message: String) {
        val normalized = message.trim()
        if (normalized.isNotEmpty()) {
            _messages.tryEmit(normalized)
        }
    }
}
