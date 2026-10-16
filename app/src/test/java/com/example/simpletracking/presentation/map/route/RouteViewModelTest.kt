package com.example.simpletracking.presentation.map.route

import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.TransitLeg
import com.example.simpletracking.domain.model.TravelMode
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.domain.usecase.FindCarRouteUseCase
import com.example.simpletracking.domain.usecase.FindTransitRoutesUseCase
import com.example.simpletracking.domain.usecase.FindWalkingRouteUseCase
import com.example.simpletracking.presentation.map.RouteLine
import com.example.simpletracking.testutil.FakeRouteRepository
import com.example.simpletracking.testutil.Fixtures
import com.example.simpletracking.testutil.MainDispatcherRule
import com.example.simpletracking.testutil.place
import com.example.simpletracking.testutil.route
import com.example.simpletracking.testutil.transitLeg
import com.example.simpletracking.testutil.transitRoute
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RouteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeRouteRepository()
    private val viewModel by lazy {
        RouteViewModel(FindCarRouteUseCase(repository), FindWalkingRouteUseCase(repository), FindTransitRoutesUseCase(repository))
    }
    private val state get() = viewModel.uiState.value

    private val origin = Fixtures.cityHall
    private val destination = place("dest", Fixtures.gangnamStation.latitude, Fixtures.gangnamStation.longitude)

    /** 이펙트 채널은 버퍼가 있어 늦게 구독해도 앞선 이펙트가 들어온다. 준비 단계의 이펙트는 테스트에서 clear()로 비운다. */
    private fun TestScope.collectEffects(): MutableList<RouteEffect> = mutableListOf<RouteEffect>().also { list ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(list) }
    }

    @Test
    fun `시작하면 세 이동 수단을 모두 조회한다`() = runTest {
        viewModel.start(destination, origin)

        assertEquals(setOf("car", "walk", "transit"), repository.calls.toSet())
        assertTrue(state.isActive)
        assertTrue(state.results.values.none { it == ModeResult.Loading })
    }

    @Test
    fun `기본은 대중교통이고, 그 경로에 카메라를 맞춘다`() = runTest {
        val effects = collectEffects()

        viewModel.start(destination, origin)

        assertEquals(TravelMode.TRANSIT, state.selectedMode)
        assertEquals(1, effects.filterIsInstance<RouteEffect.FitCamera>().size)
    }

    @Test
    fun `대중교통 경로는 출발지·도착지와 이어지는 도보 선을 덧붙인다`() = runTest {
        val stopA = GeoPoint(37.56, 126.98)
        val stopB = GeoPoint(37.50, 127.02)
        repository.transitResult = AppResult.Success(listOf(transitRoute(40, legs = listOf(transitLeg(TransitLeg.Kind.SUBWAY, stopA, stopB)))))

        viewModel.start(destination, origin)

        val lines = state.overlay!!.lines
        assertEquals(listOf(RouteLine.Kind.WALK, RouteLine.Kind.SUBWAY, RouteLine.Kind.WALK), lines.map { it.kind })
        assertEquals(listOf(origin, stopA), lines.first().points)
        assertEquals(listOf(stopB, destination.location), lines.last().points)
    }

    @Test
    fun `이동 수단을 바꾸면 그 경로를 그리고 카메라를 맞춘다`() = runTest {
        val effects = collectEffects()
        viewModel.start(destination, origin)
        effects.clear()

        viewModel.selectMode(TravelMode.WALK)

        assertEquals(RouteLine.Kind.WALK, state.overlay!!.lines.single().kind)
        assertEquals(listOf(RouteEffect.FitCamera(state.overlay!!.allPoints)), effects)
    }

    @Test
    fun `다른 대중교통 후보를 고르면 그 경로로 바뀐다`() = runTest {
        val fast = transitRoute(30, legs = listOf(transitLeg(TransitLeg.Kind.BUS, origin, destination.location)))
        val slow = transitRoute(50, legs = listOf(transitLeg(TransitLeg.Kind.SUBWAY, origin, destination.location)))
        repository.transitResult = AppResult.Success(listOf(slow, fast))
        viewModel.start(destination, origin)
        assertEquals(RouteLine.Kind.BUS, state.overlay!!.lines.single().kind)

        viewModel.selectTransitRoute(1)

        assertEquals(RouteLine.Kind.SUBWAY, state.overlay!!.lines.single().kind)
    }

    @Test
    fun `한 이동 수단이 실패해도 나머지는 그대로 보여준다`() = runTest {
        repository.walkResult = AppResult.Error(DomainError.RouteNotFound("TOO_FAR_AWAY"))

        viewModel.start(destination, origin)

        assertEquals(ModeResult.Failed(DomainError.RouteNotFound("TOO_FAR_AWAY")), state.results[TravelMode.WALK])
        assertTrue(state.results[TravelMode.CAR] is ModeResult.Found)
        assertTrue(state.results[TravelMode.TRANSIT] is ModeResult.Transit)
    }

    @Test
    fun `내 위치를 모르면 안내만 하고 API를 부르지 않는다`() = runTest {
        val effects = collectEffects()

        viewModel.start(destination, origin = null)

        assertFalse(state.isActive)
        assertEquals(emptyList<String>(), repository.calls)
        assertEquals(listOf(RouteEffect.ShowMessage(R.string.message_locating)), effects)
    }

    @Test
    fun `너무 가까우면 길찾기 화면을 열지 않는다`() = runTest {
        val effects = collectEffects()

        viewModel.start(place("near", origin.latitude + 0.0001, origin.longitude), origin)

        assertFalse(state.isActive)
        assertEquals(listOf(RouteEffect.ShowError(DomainError.DestinationTooClose)), effects)
    }

    @Test
    fun `닫으면 상태를 지우되 고른 이동 수단은 남긴다`() = runTest {
        repository.carResult = AppResult.Success(route(minutes = 30))
        viewModel.start(destination, origin)
        viewModel.selectMode(TravelMode.CAR)

        viewModel.close()

        assertEquals(RouteUiState(selectedMode = TravelMode.CAR), state)
    }
}
