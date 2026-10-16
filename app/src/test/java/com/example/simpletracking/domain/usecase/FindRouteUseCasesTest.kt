package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.testutil.FakeRouteRepository
import com.example.simpletracking.testutil.transitRoute
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindRouteUseCasesTest {

    private val repository = FakeRouteRepository()
    private val origin = GeoPoint(37.5, 127.0)
    private val tooClose = GeoPoint(37.5001, 127.0) // 약 11m
    private val farEnough = GeoPoint(37.51, 127.0) // 약 1.1km

    @Test
    fun `목적지가 20m 이내면 어떤 이동 수단도 API를 호출하지 않고 DestinationTooClose`() = runTest {
        val expected = AppResult.Error(DomainError.DestinationTooClose)

        assertEquals(expected, FindCarRouteUseCase(repository)(origin, tooClose))
        assertEquals(expected, FindWalkingRouteUseCase(repository)(origin, tooClose))
        assertEquals(expected, FindTransitRoutesUseCase(repository)(origin, tooClose))
        assertEquals(emptyList<String>(), repository.calls)
    }

    @Test
    fun `충분히 멀면 Repository에 위임한다`() = runTest {
        assertTrue(FindCarRouteUseCase(repository)(origin, farEnough) is AppResult.Success)
        assertTrue(FindWalkingRouteUseCase(repository)(origin, farEnough) is AppResult.Success)

        assertEquals(listOf("car", "walk"), repository.calls)
    }

    @Test
    fun `대중교통은 빠른 순으로 정렬한다`() = runTest {
        repository.transitResult = AppResult.Success(listOf(transitRoute(50), transitRoute(30), transitRoute(40)))

        val result = FindTransitRoutesUseCase(repository)(origin, farEnough)

        assertEquals(listOf(30, 40, 50).map { it * 60 }, (result as AppResult.Success).data.map { it.durationSeconds })
    }

    @Test
    fun `대중교통 결과가 비어 있으면 RouteNotFound`() = runTest {
        repository.transitResult = AppResult.Success(emptyList())

        val result = FindTransitRoutesUseCase(repository)(origin, farEnough)

        assertTrue((result as AppResult.Error).error is DomainError.RouteNotFound)
    }
}
