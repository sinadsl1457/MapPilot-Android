package com.example.simpletracking.data.util

import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SafeApiCallTest {

    private fun httpError(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody()))

    @Test
    fun `401은 Unauthorized`() = runTest {
        assertEquals(AppResult.Error(DomainError.Unauthorized), safeApiCall { throw httpError(401) })
    }

    @Test
    fun `429는 QuotaExceeded`() = runTest {
        assertEquals(AppResult.Error(DomainError.QuotaExceeded), safeApiCall { throw httpError(429) })
    }

    @Test
    fun `IOException은 Network`() = runTest {
        assertEquals(AppResult.Error(DomainError.Network), safeApiCall { throw IOException() })
    }

    @Test(expected = CancellationException::class)
    fun `취소는 에러로 바꾸지 않고 그대로 다시 던진다`() = runTest {
        safeApiCall { throw CancellationException("검색어가 바뀌어 이전 요청 취소") }
    }

    @Test
    fun `분류하지 못한 예외도 앱을 죽이지 않고 Unknown`() = runTest {
        val result = safeApiCall { throw IllegalStateException("예상 못 한 오류") }

        assertTrue((result as AppResult.Error).error is DomainError.Unknown)
    }
}
