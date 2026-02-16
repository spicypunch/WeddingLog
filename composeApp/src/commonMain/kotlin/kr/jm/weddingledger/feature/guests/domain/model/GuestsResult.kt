package kr.jm.weddingledger.feature.guests.domain.model

sealed class GuestsResult<out T> {
    data class Success<T>(val data: T) : GuestsResult<T>()
    data class Error(val message: String) : GuestsResult<Nothing>()
}
