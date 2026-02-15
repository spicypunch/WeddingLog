package kr.jm.weddingledger.feature.money.domain.model

sealed interface MoneyResult<out T> {
    data class Success<T>(val data: T) : MoneyResult<T>
    data class Error(val message: String) : MoneyResult<Nothing>
}

