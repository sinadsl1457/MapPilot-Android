package com.example.simpletracking.data.repository

import com.example.simpletracking.data.remote.api.KakaoNaviApi
import com.example.simpletracking.data.remote.dto.DirectionsResponse
import com.example.simpletracking.data.remote.dto.RouteDto
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRepositoryImplTest {

    /** Retrofit API도 인터페이스라 네트워크 없이 Fake로 대체할 수 있다. */
    private class FakeNaviApi(private val response: DirectionsResponse) : KakaoNaviApi {
        var lastOrigin: String? = null
        var lastPriority: String? = null
        override suspend fun directions(origin: String, destination: String, priority: String): DirectionsResponse {
            lastOrigin = origin
            lastPriority = priority
            return response
        }
    }

    private val origin = GeoPoint(latitude = 37.5, longitude = 127.0)
    private val destination = GeoPoint(latitude = 37.51, longitude = 127.01)

    @Test
    fun `경도,위도 순서로 요청하고 최단거리 우선으로 요청한다`() = runTest {
        val api = FakeNaviApi(DirectionsResponse(listOf(RouteDto(resultCode = 0))))

        RouteRepositoryImpl(api).findShortestRoute(origin, destination)

        assertEquals("127.0,37.5", api.lastOrigin)
        assertEquals("DISTANCE", api.lastPriority)
    }

    @Test
    fun `HTTP 200이어도 result_code가 0이 아니면 RouteNotFound`() = runTest {
        val api = FakeNaviApi(DirectionsResponse(listOf(RouteDto(resultCode = 104, resultMsg = "출발지와 도착지가 5 m 이내로 설정된 경우 경로를 탐색할 수 없음"))))

        val result = RouteRepositoryImpl(api).findShortestRoute(origin, destination)

        assertTrue((result as AppResult.Error).error is DomainError.RouteNotFound)
    }

    @Test
    fun `routes가 비어 있으면 RouteNotFound`() = runTest {
        val result = RouteRepositoryImpl(FakeNaviApi(DirectionsResponse(emptyList()))).findShortestRoute(origin, destination)

        assertTrue((result as AppResult.Error).error is DomainError.RouteNotFound)
    }
}
