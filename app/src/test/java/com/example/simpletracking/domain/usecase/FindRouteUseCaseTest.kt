package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.repository.RouteRepository
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindRouteUseCaseTest {

    private class FakeRouteRepository : RouteRepository {
        var callCount = 0
        override suspend fun findShortestRoute(origin: GeoPoint, destination: GeoPoint): AppResult<Route> {
            callCount++
            return AppResult.Success(Route(listOf(origin, destination), distanceMeters = 100, durationSeconds = 60))
        }
    }

    private val repository = FakeRouteRepository()
    private val findRoute = FindRouteUseCase(repository)

    @Test
    fun `목적지가 20m 이내면 API 호출 없이 DestinationTooClose`() = runTest {
        val origin = GeoPoint(37.5, 127.0)
        val destination = GeoPoint(37.5001, 127.0) // 약 11m

        val result = findRoute(origin, destination)

        assertEquals(AppResult.Error(DomainError.DestinationTooClose), result)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `충분히 멀면 Repository에 위임한다`() = runTest {
        val origin = GeoPoint(37.5, 127.0)
        val destination = GeoPoint(37.51, 127.0) // 약 1.1km

        val result = findRoute(origin, destination)

        assertTrue(result is AppResult.Success)
        assertEquals(1, repository.callCount)
    }
}
