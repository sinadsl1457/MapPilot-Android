package com.example.simpletracking.data.repository

import com.example.simpletracking.data.remote.api.KakaoNaviApi
import com.example.simpletracking.data.remote.api.KakaoRoutingApi
import com.example.simpletracking.data.remote.dto.DirectionsResponse
import com.example.simpletracking.data.remote.dto.RouteDto
import com.example.simpletracking.data.remote.dto.RouteSummaryDto
import com.example.simpletracking.data.remote.dto.TransitResponse
import com.example.simpletracking.data.remote.dto.WalkResponse
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.TransitLeg
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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

    private class FakeRoutingApi(
        private val walk: WalkResponse = WalkResponse("OK"),
        private val transit: TransitResponse = TransitResponse("OK"),
    ) : KakaoRoutingApi {
        var lastStart: Pair<Double, Double>? = null
        override suspend fun walk(startLongitude: Double, startLatitude: Double, endLongitude: Double, endLatitude: Double): WalkResponse {
            lastStart = startLongitude to startLatitude
            return walk
        }
        override suspend fun publicTraffic(startLongitude: Double, startLatitude: Double, endLongitude: Double, endLatitude: Double): TransitResponse {
            lastStart = startLongitude to startLatitude
            return transit
        }
    }

    /** NetworkModule과 같은 설정 */
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val origin = GeoPoint(latitude = 37.5, longitude = 127.0)
    private val destination = GeoPoint(latitude = 37.51, longitude = 127.01)

    private fun repository(navi: DirectionsResponse = DirectionsResponse(), routing: FakeRoutingApi = FakeRoutingApi()) =
        RouteRepositoryImpl(FakeNaviApi(navi), routing)

    @Test
    fun `자동차는 경도,위도 순서로 추천 경로를 요청한다`() = runTest {
        val api = FakeNaviApi(DirectionsResponse(listOf(RouteDto(resultCode = 0, summary = RouteSummaryDto()))))

        RouteRepositoryImpl(api, FakeRoutingApi()).findCarRoute(origin, destination)

        assertEquals("127.0,37.5", api.lastOrigin)
        assertEquals("RECOMMEND", api.lastPriority)
    }

    @Test
    fun `HTTP 200이어도 result_code가 0이 아니면 RouteNotFound`() = runTest {
        val response = DirectionsResponse(listOf(RouteDto(resultCode = 104, resultMsg = "출발지와 도착지가 5 m 이내로 설정된 경우 경로를 탐색할 수 없음")))

        val result = repository(navi = response).findCarRoute(origin, destination)

        assertEquals(AppResult.Error(DomainError.RouteNotFound("출발지와 도착지가 5 m 이내로 설정된 경우 경로를 탐색할 수 없음")), result)
    }

    @Test
    fun `routes가 비어 있으면 RouteNotFound`() = runTest {
        val result = repository(navi = DirectionsResponse(emptyList())).findCarRoute(origin, destination)

        assertTrue((result as AppResult.Error).error is DomainError.RouteNotFound)
    }

    @Test
    fun `도보는 x(경도)·y(위도)를 따로 보내고 실패 status는 에러로 바꾼다`() = runTest {
        val routing = FakeRoutingApi(walk = WalkResponse("TOO_FAR_AWAY"))

        val result = repository(routing = routing).findWalkingRoute(origin, destination)

        assertEquals(127.0 to 37.5, routing.lastStart)
        assertEquals(AppResult.Error(DomainError.RouteNotFound("TOO_FAR_AWAY")), result)
    }

    @Test
    fun `대중교통 실제 응답 모양을 해석한다`() = runTest {
        val body = """
            {"status":"OK",
             "properties":{"total":1,"bus":0,"subway":1,"busAndSubway":0,"landingURL":"x"},
             "routes":[{"properties":{"type":"SUBWAY","totalDistance":25950,"totalTime":3573,"transfers":1,"fare":{"value":1850}},
               "steps":[
                 {"properties":{"guidance":"수인분당선 (태평 > 왕십리)","type":"SUBWAY","distance":20100,"time":1980,
                   "stops":[{"name":"태평"},{"name":"왕십리"}],"vehicles":[{"name":"수인분당선","type":"일반"}]},
                  "path":{"points":[[127.13,37.44],[127.03,37.56]]}},
                 {"properties":{"guidance":"왕십리역 환승","type":"WALKING","distance":88,"time":144,"stops":[{"name":"왕십리"}],"vehicles":null},
                  "path":{"points":[[127.03,37.56],[127.031,37.561]]}}
               ]}]}
        """.trimIndent()
        val routing = FakeRoutingApi(transit = json.decodeFromString<TransitResponse>(body))

        val routes = (repository(routing = routing).findTransitRoutes(origin, destination) as AppResult.Success).data

        assertEquals(1, routes.size)
        assertEquals(3_573, routes[0].durationSeconds)
        assertEquals(listOf(TransitLeg.Kind.SUBWAY, TransitLeg.Kind.WALK), routes[0].legs.map { it.kind })
    }

    @Test
    fun `대중교통 실패 status는 에러로 바꾼다`() = runTest {
        val routing = FakeRoutingApi(transit = json.decodeFromString<TransitResponse>("""{"status":"EQUAL_POINTS"}"""))

        val result = repository(routing = routing).findTransitRoutes(origin, destination)

        assertEquals(AppResult.Error(DomainError.DestinationTooClose), result)
    }
}
