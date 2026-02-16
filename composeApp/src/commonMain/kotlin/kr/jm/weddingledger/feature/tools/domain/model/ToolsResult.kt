package kr.jm.weddingledger.feature.tools.domain.model

sealed class ToolsResult<out T> {
    data class Success<T>(val data: T) : ToolsResult<T>()
    data class Error(val message: String) : ToolsResult<Nothing>()
}

