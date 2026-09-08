package com.nivya.core.network

/**
 * Sealed interface representing network execution outcomes.
 */
sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Error(val code: Int, val message: String) : NetworkResult<Nothing>
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing> {
        val exception: Throwable get() = throwable
    }

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error || this is Exception
}
