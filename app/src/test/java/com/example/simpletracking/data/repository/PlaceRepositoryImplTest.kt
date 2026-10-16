package com.example.simpletracking.data.repository

import com.example.simpletracking.data.remote.api.KakaoLocalApi
import com.example.simpletracking.data.remote.dto.KeywordSearchResponse
import com.example.simpletracking.data.remote.dto.PlaceDocument
import com.example.simpletracking.domain.model.PlaceCategory
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.testutil.Fixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceRepositoryImplTest {

    private class FakeLocalApi(private val response: KeywordSearchResponse) : KakaoLocalApi {
        var lastCategoryCall: List<Any>? = null
        override suspend fun searchKeyword(query: String, longitude: Double?, latitude: Double?, size: Int) = response
        override suspend fun searchCategory(
            code: String, longitude: Double, latitude: Double, radiusMeters: Int, sort: String, size: Int,
        ): KeywordSearchResponse {
            lastCategoryCall = listOf(code, longitude, latitude, radiusMeters, sort)
            return response
        }
    }

    private fun document(id: String, x: String = "127.0", y: String = "37.5") =
        PlaceDocument(id = id, placeName = id, x = x, y = y)

    @Test
    fun `카테고리 검색은 코드·중심·반경을 보내고 가까운 순으로 요청하며 좌표가 깨진 항목은 버린다`() = runTest {
        val api = FakeLocalApi(KeywordSearchResponse(listOf(document("1"), document("2", x = "abc"))))

        val result = PlaceRepositoryImpl(api).searchByCategory(PlaceCategory.CAFE, Fixtures.cityHall, 1_000)

        assertEquals(listOf("CE7", 126.9784, 37.5666, 1_000, "distance"), api.lastCategoryCall)
        assertEquals(listOf("1"), (result as AppResult.Success).data.map { it.id })
    }
}
