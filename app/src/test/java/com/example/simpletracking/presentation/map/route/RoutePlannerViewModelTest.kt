package com.example.simpletracking.presentation.map.route

import com.example.simpletracking.domain.model.RecentPlace
import com.example.simpletracking.domain.model.RouteEndpoint
import com.example.simpletracking.domain.model.TravelMode
import com.example.simpletracking.domain.usecase.RecentPlacesUseCase
import com.example.simpletracking.testutil.FakeRecentPlaceRepository
import com.example.simpletracking.testutil.MainDispatcherRule
import com.example.simpletracking.testutil.place
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Flutter route_planner_notifier_test · iOS RoutePlannerViewModelTests와 같은 경우. */
class RoutePlannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recent = RecentPlace(place("r"), usedAtMillis = 1)
    private val repository = FakeRecentPlaceRepository(listOf(recent))
    private val viewModel by lazy { RoutePlannerViewModel(RecentPlacesUseCase(repository)) }
    private val state get() = viewModel.uiState.value

    private fun TestScope.collectEffects(): MutableList<RoutePlannerEffect> = mutableListOf<RoutePlannerEffect>().also { list ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(list) }
    }

    @Test
    fun `출발지는 내 위치, 도착지는 비어 있고 최근 장소를 불러온다`() = runTest {
        viewModel.open(TravelMode.WALK)
        advanceUntilIdle()

        assertEquals(RouteEndpoint.MyLocation, state.origin)
        assertNull(state.destination)
        assertEquals(TravelMode.WALK, state.mode)
        assertEquals(listOf(recent), state.recentPlaces)
    }

    @Test
    fun `최근 장소를 누르면 도착지가 되고 바로 길찾기로 넘어간다`() = runTest {
        viewModel.open(TravelMode.TRANSIT)
        viewModel.selectMode(TravelMode.CAR)
        val effects = collectEffects()

        viewModel.chooseRecent(recent)
        advanceUntilIdle()

        val plan = RoutePlan(RouteEndpoint.MyLocation, RouteEndpoint.PlaceEndpoint(recent.place), TravelMode.CAR)
        assertEquals(listOf(RoutePlannerEffect.Completed(plan)), effects)
        // 다시 고른 장소는 목록 맨 앞으로(고른 시각 갱신)
        assertEquals("r", repository.saved.first().place.id)
        assertNotEquals(1L, repository.saved.first().usedAtMillis)
    }

    @Test
    fun `도착지가 비었을 때 맞바꾸면 출발지가 비고 넘어가지 않는다`() = runTest {
        viewModel.open(TravelMode.TRANSIT)
        val effects = collectEffects()

        viewModel.swap()

        assertNull(state.origin)
        assertEquals(RouteEndpoint.MyLocation, state.destination)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `비어 있는 출발지를 채우면 넘어간다`() = runTest {
        viewModel.open(TravelMode.TRANSIT)
        viewModel.swap()
        val effects = collectEffects()

        viewModel.setOrigin(RouteEndpoint.PlaceEndpoint(recent.place))

        val plan = RoutePlan(RouteEndpoint.PlaceEndpoint(recent.place), RouteEndpoint.MyLocation, TravelMode.TRANSIT)
        assertEquals(listOf(RoutePlannerEffect.Completed(plan)), effects)
    }

    @Test
    fun `최근 장소 삭제`() = runTest {
        viewModel.open(TravelMode.TRANSIT)
        advanceUntilIdle()

        viewModel.removeRecent(recent)
        advanceUntilIdle()

        assertTrue(state.recentPlaces.isEmpty())
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `다시 열면 지난번 선택은 지워진다`() = runTest {
        viewModel.open(TravelMode.TRANSIT)
        viewModel.swap()

        viewModel.open(TravelMode.CAR)

        assertEquals(RouteEndpoint.MyLocation, state.origin)
        assertNull(state.destination)
        assertEquals(TravelMode.CAR, state.mode)
    }
}
