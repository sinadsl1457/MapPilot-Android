package com.example.simpletracking.domain.result

/**
 * dartz의 Either<L, R>에 해당. 왼쪽 타입을 DomainError로 고정했다.
 * `out T` 덕분에 AppResult.Error(Nothing 타입)를 어떤 AppResult<T> 자리에도 넣을 수 있다.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val error: DomainError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}

/** dartz의 fold와 같다: 실패/성공 각각을 처리해 하나의 값으로 만든다. */
inline fun <T, R> AppResult<T>.fold(onError: (DomainError) -> R, onSuccess: (T) -> R): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Error -> onError(error)
}
