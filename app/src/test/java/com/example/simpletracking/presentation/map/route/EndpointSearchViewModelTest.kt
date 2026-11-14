package com.example.simpletracking.presentation.map.route

import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.domain.usecase.RecentPlacesUseCase
import com.example.simpletracking.domain.usecase.RecentSearchesUseCase
import com.example.simpletracking.domain.usecase.SearchPlacesUseCase
import com.example.simpletracking.testutil.FakePlaceRepository
import com.example.simpletracking.testutil.FakeRecentPlaceRepository
import com.example.simpletracking.testutil.FakeRecentSearchRepository
import com.example.simpletracking.testutil.MainDispatcherRule
import com.example.simpletracking.testutil.place
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Flutter endpoint_search_notifier_test · iOS EndpointSearchViewModelTests와 같은 경우. */
class EndpointSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val places = FakePlaceRepository()
    private val recents = FakeRecentSearchRepository()
    private val recentPlaces = FakeRecentPlaceRepository()
    private val viewModel by lazy {
        EndpointSearchViewModel(SearchPlacesUseCase(places), RecentSearchesUseCase(recents), RecentPlacesUseCase(recentPlaces))
    }
    private val state get() = viewModel.uiState.value
    private val cafes = listOf(place("c1"), place("c2"))

    private fun TestScope.collectErrors(): MutableList<DomainError> = mutableListOf<DomainError>().also { list ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.errors.toList(list) }
    }

    @Test
    fun `검색하면 결과를 보이고 최근 검색어에 넣는다`() = runTest {
        places.keywordResult = AppResult.Success(cafes)
        viewModel.onQueryChange(" 카페 ", center = null)

        viewModel.submit(center = null)
        advanceUntilIdle()

        assertEquals(cafes, state.results)
        assertEquals("카페", state.resultsQuery)
        assertEquals(listOf("카페"), recents.saved)
    }

    @Test
    fun `빈 검색어는 오류를 보이고 결과는 그대로다`() = runTest {
        val errors = collectErrors()

        viewModel.submit(center = null)
        advanceUntilIdle()

        assertEquals(listOf<DomainError>(DomainError.EmptyQuery), errors)
        assertNull(state.results)
    }

    @Test
    fun `입력을 지우면 최근 검색어로 돌아가고 대기 중인 요청은 보내지 않는다`() = runTest {
        places.keywordResult = AppResult.Success(cafes)

        viewModel.onQueryChange("카페", center = null)
        viewModel.onQueryChange("", center = null)
        advanceUntilIdle()

        assertTrue(places.keywordQueries.isEmpty())
        assertNull(state.results)
    }

    @Test
    fun `최근 검색어로 다시 검색한다`() = runTest {
        places.keywordResult = AppResult.Success(cafes)

        viewModel.searchRecent("카페", center = null)
        advanceUntilIdle()

        assertEquals("카페", state.query)
        assertEquals(cafes, state.results)
    }

    @Test
    fun `엔터 없이 입력이 0점3초 멈추면 목록을 보인다`() = runTest {
        places.keywordResult = AppResult.Success(cafes)

        viewModel.onQueryChange("카페", center = null)
        advanceTimeBy(EndpointSearchViewModel.SEARCH_DELAY_MILLIS - 1)
        assertTrue("아직 기다리는 중", places.keywordQueries.isEmpty())
        advanceUntilIdle()

        assertEquals(cafes, state.results)
        assertEquals("카페", state.resultsQuery)
    }

    @Test
    fun `연달아 입력하면 마지막 입력으로 한 번만 찾는다`() = runTest {
        listOf("카", "카페").forEach {
            viewModel.onQueryChange(it, center = null)
            advanceTimeBy(100)
        }
        advanceUntilIdle()

        assertEquals(listOf("카페"), places.keywordQueries)
    }

    @Test
    fun `새 결과가 올 때까지 이전 목록을 그대로 둔다`() = runTest {
        places.keywordResult = AppResult.Success(cafes)
        viewModel.onQueryChange("카페", center = null)
        advanceUntilIdle()

        viewModel.onQueryChange("은행", center = null)

        assertEquals(cafes, state.results)
    }

    @Test
    fun `실패해도 오류를 띄우지 않는다(엔터 검색만 오류를 보인다)`() = runTest {
        places.keywordResult = AppResult.Error(DomainError.Network)
        val errors = collectErrors()

        viewModel.onQueryChange("카페", center = null)
        advanceUntilIdle()

        assertTrue(errors.isEmpty())
        assertNull(state.results)
    }

    @Test
    fun `목록에서 장소를 고르면 그 목록의 검색어를 최근 검색어에, 장소를 최근 장소에 남긴다`() = runTest {
        places.keywordResult = AppResult.Success(cafes)
        viewModel.onQueryChange("카페", center = null)
        advanceUntilIdle()

        viewModel.remember(cafes[0])
        advanceUntilIdle()

        assertEquals(listOf("카페"), recents.saved)
        assertEquals(listOf("c1"), recentPlaces.saved.map { it.place.id })
    }

    @Test
    fun `다시 열면 지난 입력과 결과를 지운다`() = runTest {
        places.keywordResult = AppResult.Success(cafes)
        viewModel.onQueryChange("카페", center = null)
        advanceUntilIdle()

        viewModel.open()

        assertEquals("", state.query)
        assertNull(state.results)
    }
}
