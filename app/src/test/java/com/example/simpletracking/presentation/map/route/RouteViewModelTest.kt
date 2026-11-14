package com.example.simpletracking.presentation.map.route

import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.RouteEndpoint
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Flutter route_notifier_test · iOS RouteViewModelTests와 같은 경우. */
class RouteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeRouteRepository()
    private val viewModel by lazy {
        RouteViewModel(FindCarRouteUseCase(repository), FindWalkingRouteUseCase(repository), FindTransitRoutesUseCase(repository))
    }
    private val state get() = viewModel.uiState.value

    private val me = Fixtures.cityHall
    private val destination = place("dest", Fixtures.gangnamStation.latitude, Fixtures.gangnamStation.longitude)
    private val other = place("other", 37.55, 127.05)

    /** 이펙트 채널은 버퍼가 있어 늦게 구독해도 앞선 이펙트가 들어온다. */
    private fun TestScope.collectEffects(): MutableList<RouteEffect> = mutableListOf<RouteEffect>().also { list ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(list) }
    }

    // region 시작

    @Test
    fun `시작하면 세 이동 수단을 모두 조회한다`() = runTest {
        viewModel.start(destination, me)

        assertEquals(setOf("car", "walk", "transit"), repository.calls.toSet())
        assertTrue(state.isActive)
        assertEquals(RouteEndpoint.MyLocation, state.origin)
        assertTrue(state.results.values.none { it == ModeResult.Loading })
    }

    @Test
    fun `찾으면 목록 단계다 - 기본 탭은 대중교통이고 지도에 선을 그리지 않는다`() = runTest {
        val effects = collectEffects()

        viewModel.start(destination, me)

        assertEquals(TravelMode.TRANSIT, state.selectedMode)
        assertTrue(state.isShowingList)
        assertNull(state.overlay)
        assertTrue(effects.none { it is RouteEffect.FitCamera })
    }

    @Test
    fun `목록에서 경로를 고르면 지도에 그 경로를 그리고 카메라를 맞춘다`() = runTest {
        repository.transitResult = AppResult.Success(listOf(transitRoute(40), transitRoute(50)))
        viewModel.start(destination, me)
        val effects = collectEffects()

        viewModel.showRoute(1)

        assertTrue(state.isShowingRoute)
        assertFalse(state.isShowingList)
        assertEquals(50 * 60, state.selectedTransitRoute?.durationSeconds)
        assertEquals(listOf(RouteEffect.FitCamera(state.overlay!!.allPoints)), effects)
    }

    @Test
    fun `목록으로 돌아가면 지도 선을 지운다(결과는 유지)`() = runTest {
        viewModel.start(destination, me)
        viewModel.showRoute(0)

        viewModel.backToList()

        assertFalse(state.isShowingRoute)
        assertNull(state.overlay)
        assertTrue(state.results[TravelMode.TRANSIT] is ModeResult.Transit)
    }

    @Test
    fun `경로를 보던 중 다시 찾으면 목록 단계로 돌아간다`() = runTest {
        viewModel.start(destination, me)
        viewModel.showRoute(0)

        viewModel.swap(me)

        assertFalse(state.isShowingRoute)
        assertEquals(0, state.selectedTransitIndex)
    }

    @Test
    fun `대중교통 경로는 출발지·도착지와 도보 선으로 잇는다`() = runTest {
        val stopA = GeoPoint(37.56, 126.98)
        val stopB = GeoPoint(37.50, 127.02)
        repository.transitResult = AppResult.Success(listOf(transitRoute(40, legs = listOf(transitLeg(TransitLeg.Kind.SUBWAY, stopA, stopB)))))
        viewModel.start(destination, me)

        viewModel.showRoute(0)

        val lines = state.overlay!!.lines
        assertEquals(listOf(RouteLine.Kind.WALK, RouteLine.Kind.SUBWAY, RouteLine.Kind.WALK), lines.map { it.kind })
        assertEquals(listOf(me, stopA), lines.first().points)
        assertEquals(listOf(stopB, destination.location), lines.last().points)
    }

    // endregion

    // region 자동차·도보는 바로 지도에

    @Test
    fun `도보 탭을 고르면 고를 것 없이 바로 도보 선을 그리고 카메라를 맞춘다`() = runTest {
        viewModel.start(destination, me)
        val effects = collectEffects()

        viewModel.selectMode(TravelMode.WALK)

        assertTrue(state.isShowingRoute)
        assertEquals(listOf(RouteLine.Kind.WALK), state.overlay!!.lines.map { it.kind })
        assertEquals(1, effects.filterIsInstance<RouteEffect.FitCamera>().size)
    }

    @Test
    fun `자동차로 찾으면 목록 없이 결과가 오는 대로 지도에 그린다`() = runTest {
        viewModel.selectMode(TravelMode.CAR)
        val effects = collectEffects()

        viewModel.start(destination, me)

        assertTrue(state.isShowingRoute)
        assertFalse(state.isShowingList)
        assertEquals(1, effects.filterIsInstance<RouteEffect.FitCamera>().size)
    }

    @Test
    fun `도보를 보다가 대중교통 탭을 고르면 목록 단계로 돌아간다`() = runTest {
        viewModel.start(destination, me)
        viewModel.selectMode(TravelMode.WALK)

        viewModel.selectMode(TravelMode.TRANSIT)

        assertTrue(state.isShowingList)
    }

    // endregion

    // region 실패·안내

    @Test
    fun `한 이동 수단이 실패해도 나머지는 그대로 보여준다`() = runTest {
        repository.walkResult = AppResult.Error(DomainError.RouteNotFound("TOO_FAR_AWAY"))

        viewModel.start(destination, me)

        assertTrue(state.results[TravelMode.WALK] is ModeResult.Failed)
        assertTrue(state.results[TravelMode.CAR] is ModeResult.Found)
        viewModel.selectMode(TravelMode.WALK)
        assertNull(state.overlay)
    }

    @Test
    fun `내 위치를 모르면 안내만 하고 API를 부르지 않는다`() = runTest {
        val effects = collectEffects()

        viewModel.start(destination, myLocation = null)

        assertFalse(state.isActive)
        assertEquals(listOf(RouteEffect.ShowMessage(R.string.message_locating)), effects)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `너무 가까우면 길찾기 화면을 열지 않는다`() = runTest {
        val effects = collectEffects()

        viewModel.start(place("near", me.latitude, me.longitude), me)

        assertFalse(state.isActive)
        assertEquals(listOf(RouteEffect.ShowError(DomainError.DestinationTooClose)), effects)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `닫으면 상태를 지우되 고른 이동 수단은 남긴다`() = runTest {
        viewModel.start(destination, me)
        viewModel.selectMode(TravelMode.CAR)

        viewModel.close()

        assertFalse(state.isActive)
        assertFalse(state.isShowingList)
        assertTrue(state.results.isEmpty())
        assertEquals(TravelMode.CAR, state.selectedMode)
    }

    // endregion

    // region 출발지·도착지 바꾸기

    @Test
    fun `출발지를 장소로 바꾸면 그 장소에서 다시 찾는다`() = runTest {
        viewModel.start(destination, me)

        viewModel.changeOrigin(RouteEndpoint.PlaceEndpoint(other), me)

        assertEquals(RouteEndpoint.PlaceEndpoint(other), state.origin)
        assertEquals(other.location, repository.carRequests.last().first)
    }

    @Test
    fun `맞바꾸면 내 위치가 도착지가 된다`() = runTest {
        viewModel.start(destination, me)

        viewModel.swap(me)

        assertEquals(RouteEndpoint.PlaceEndpoint(destination), state.origin)
        assertEquals(RouteEndpoint.MyLocation, state.destination)
        assertEquals(destination.location to me, repository.carRequests.last())
    }

    @Test
    fun `내 위치를 모르면 맞바꾸지 않고 안내한다`() = runTest {
        viewModel.start(destination, me)
        val effects = collectEffects()

        viewModel.swap(myLocation = null)

        assertEquals(RouteEndpoint.MyLocation, state.origin)
        assertEquals(listOf(RouteEffect.ShowMessage(R.string.message_locating)), effects)
    }

    @Test
    fun `도착지와 너무 가까운 출발지는 받지 않는다`() = runTest {
        viewModel.start(destination, me)
        val effects = collectEffects()

        viewModel.changeOrigin(RouteEndpoint.PlaceEndpoint(place("next", destination.location.latitude, destination.location.longitude)), me)

        assertEquals(RouteEndpoint.MyLocation, state.origin)
        assertEquals(listOf(RouteEffect.ShowError(DomainError.DestinationTooClose)), effects)
    }

    @Test
    fun `도착지를 내 위치로 바꿀 수 있다`() = runTest {
        viewModel.plan(RoutePlan(RouteEndpoint.PlaceEndpoint(other), RouteEndpoint.PlaceEndpoint(destination), TravelMode.CAR), me)

        viewModel.changeDestination(RouteEndpoint.MyLocation, me)

        assertEquals(RouteEndpoint.MyLocation, state.destination)
        assertEquals(me, repository.carRequests.last().second)
    }

    // endregion

    @Test
    fun `준비 화면의 계획대로 이동 수단을 정하고 찾는다`() = runTest {
        viewModel.plan(RoutePlan(RouteEndpoint.PlaceEndpoint(other), RouteEndpoint.PlaceEndpoint(destination), TravelMode.WALK), myLocation = null)

        assertEquals(TravelMode.WALK, state.selectedMode)
        assertEquals(RouteEndpoint.PlaceEndpoint(other), state.origin)
        assertTrue(state.isShowingRoute)
    }

    @Test
    fun `자동차 경로 결과는 그대로 쓴다`() = runTest {
        repository.carResult = AppResult.Success(route(minutes = 12))
        viewModel.selectMode(TravelMode.CAR)

        viewModel.start(destination, me)

        assertEquals(12 * 60, (state.selectedResult as ModeResult.Found).route.durationSeconds)
    }
}
