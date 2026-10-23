package com.ferbotz.momentic.data.result

/**
 * The result of a data-layer operation: [Success] with a value or [Failure] with a typed
 * [AppError]. Repositories return this instead of throwing, so view models handle both outcomes
 * explicitly.
 */
sealed interface AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>

    val isSuccess: Boolean get() = this is Success

    /** The value on success, or `null` on failure. */
    fun getOrNull(): T? = (this as? Success)?.data

    /** The error on failure, or `null` on success. */
    fun errorOrNull(): AppError? = (this as? Failure)?.error
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}
