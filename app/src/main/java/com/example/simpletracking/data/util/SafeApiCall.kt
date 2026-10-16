package com.example.simpletracking.data.util

import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/**
 * 네트워크 예외를 DomainError로 바꾸는 경계. Flutter에서 DioException을 catch해 Left(...)로 바꾸던 곳이다.
 * 예외 → 타입 변환은 data 계층에서 끝내고, 위 계층(domain/presentation)은 예외를 몰라도 되게 한다.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        // 취소는 에러가 아니다. 삼키면 부모 코루틴이 취소를 모르게 되므로 반드시 다시 던진다.
        throw e
    } catch (e: HttpException) {
        AppResult.Error(
            when (e.code()) {
                401, 403 -> DomainError.Unauthorized
                429 -> DomainError.QuotaExceeded
                else -> DomainError.Server(e.code())
            }
        )
    } catch (e: IOException) {
        AppResult.Error(DomainError.Network)
    } catch (e: SerializationException) {
        AppResult.Error(DomainError.Unknown(e))
    } catch (e: Exception) {
        // 위에서 분류하지 못한 예외도 앱을 죽이지 않고 Unknown으로 넘긴다.
        AppResult.Error(DomainError.Unknown(e))
    }
