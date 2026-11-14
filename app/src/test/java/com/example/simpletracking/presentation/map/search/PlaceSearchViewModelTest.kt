package com.example.simpletracking.presentation.map.search

import kotlinx.coroutines.test.advanceUntilIdle
import com.example.simpletracking.testutil.FakeRecentPlaceRepository
import com.example.simpletracking.domain.usecase.RecentPlacesUseCase
import com.example.simpletracking.domain.model.PlaceCategory
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.domain.usecase.RecentSearchesUseCase
import com.example.simpletracking.domain.usecase.SearchPlacesByCategoryUseCase
import com.example.simpletracking.domain.usecase.SearchPlacesUseCase
import com.example.simpletracking.testutil.FakePlaceRepository
import com.example.simpletracking.testutil.FakeRecentSearchRepository
import com.example.simpletracking.testutil.Fixtures
import com.example.simpletracking.testutil.MainDispatcherRule
import com.example.simpletracking.testutil.place
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaceSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val places = FakePlaceRepository()
    private val recents = FakeRecentSearchRepository(initial = listOf("서울역"))
    private val recentPlaces = FakeRecentPlaceRepository()
    private val viewModel by lazy {
        PlaceSearchViewModel(
            SearchPlacesUseCase(places),
            SearchPlacesByCategoryUseCase(places),
            RecentSearchesUseCase(recents),
            RecentPlacesUseCase(recentPlaces),
        )
    }
    private val state get() = viewModel.uiState.value

    /** 이펙트 채널은 버퍼가 있어 늦게 구독해도 앞선 이펙트가 들어온다. 준비 단계의 이펙트는 테스트에서 clear()로 비운다. */
    private fun TestScope.collectEffects(): MutableList<PlaceSearchEffect> = mutableListOf<PlaceSearchEffect>().also { list ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(list) }
    }

    @Test
    fun `시작하면 최근 검색어를 불러온다`() = runTest {
        assertEquals(listOf("서울역"), state.recentQueries)
    }

    @Test
    fun `검색에 성공하면 결과를 보여주고 페이지를 닫고 카메라를 맞추고 최근 검색어에 넣는다`() = runTest {
        val found = listOf(place("1", 37.10, 127.10), place("2", 37.11, 127.11)) // 약 1.4km 거리라 둘 다 맞춘다
        places.keywordResult = AppResult.Success(found)
        val effects = collectEffects()
        viewModel.openSearchPage(center = null)
        viewModel.onQueryChange("  강남역  ")

        viewModel.submit(center = Fixtures.cityHall)

        assertFalse(state.isSearchPageVisible)
        assertFalse(state.isSubmitting)
        assertEquals(SearchSource.Keyword("강남역"), state.source)
        assertEquals(found, state.places)
        assertEquals(Fixtures.cityHall, places.lastNear)
        assertEquals(listOf("강남역", "서울역"), state.recentQueries)
        assertEquals(listOf(PlaceSearchEffect.FitCamera(found.map { it.location })), effects)
    }

    @Test
    fun `빈 검색어는 페이지에 머물며 에러를 알린다`() = runTest {
        val effects = collectEffects()
        viewModel.openSearchPage(center = null)
        viewModel.onQueryChange("   ")

        viewModel.submit(center = null)

        assertTrue(state.isSearchPageVisible)
        assertNull(state.source)
        assertEquals(listOf(PlaceSearchEffect.ShowError(DomainError.EmptyQuery)), effects)
    }

    @Test
    fun `네트워크 오류면 페이지에 머문다`() = runTest {
        places.keywordResult = AppResult.Error(DomainError.Network)
        viewModel.openSearchPage(center = null)
        viewModel.onQueryChange("강남역")

        viewModel.submit(center = null)

        assertTrue(state.isSearchPageVisible)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `결과가 없으면 시트는 보이되 카메라는 움직이지 않는다`() = runTest {
        val effects = collectEffects()
        viewModel.onQueryChange("없는곳")

        viewModel.submit(center = null)

        assertEquals(SearchPhase.Loaded(emptyList()), state.phase)
        assertEquals(emptyList<PlaceSearchEffect>(), effects)
    }

    @Test
    fun `최근 검색어를 누르면 다시 검색한다`() = runTest {
        viewModel.searchRecent("서울역", center = null)

        assertEquals("서울역", places.lastQuery)
        assertEquals(SearchSource.Keyword("서울역"), state.source)
    }

    @Test
    fun `검색 페이지를 다시 열면 지금 검색어를 채워 둔다`() = runTest {
        viewModel.onQueryChange("강남역")
        viewModel.submit(center = null)

        viewModel.onQueryChange("")
        viewModel.openSearchPage(center = null)

        assertEquals("강남역", state.query)
    }

    @Test
    fun `카테고리를 누르면 중심 좌표 주변을 찾는다`() = runTest {
        places.categoryResult = AppResult.Success(listOf(place("1")))

        viewModel.toggleCategory(PlaceCategory.CAFE, Fixtures.cityHall)

        assertEquals(PlaceCategory.CAFE, state.selectedCategory)
        assertEquals(Fixtures.cityHall, places.lastCenter)
        assertEquals(1, state.places.size)
    }

    @Test
    fun `같은 카테고리를 다시 누르면 결과를 지운다`() = runTest {
        viewModel.toggleCategory(PlaceCategory.CAFE, Fixtures.cityHall)

        viewModel.toggleCategory(PlaceCategory.CAFE, Fixtures.cityHall)

        assertNull(state.source)
        assertNull(state.phase)
    }

    @Test
    fun `카테고리 검색이 실패하면 결과를 지우고 에러를 알린다`() = runTest {
        places.categoryResult = AppResult.Error(DomainError.Network)
        val effects = collectEffects()

        viewModel.toggleCategory(PlaceCategory.CAFE, Fixtures.cityHall)

        assertNull(state.source)
        assertEquals(listOf(PlaceSearchEffect.ShowError(DomainError.Network)), effects)
    }

    @Test
    fun `장소를 고르면 그 위치로 카메라를 옮기고, 모르는 마커 id는 무시한다`() = runTest {
        val target = place("1", 37.3, 127.3)
        places.categoryResult = AppResult.Success(listOf(target))
        val effects = collectEffects()
        viewModel.toggleCategory(PlaceCategory.CAFE, Fixtures.cityHall)
        effects.clear()

        viewModel.selectPlace("없는 id")
        viewModel.selectPlace("1")

        assertEquals(target, state.selectedPlace)
        assertEquals(listOf(PlaceSearchEffect.Focus(target.location)), effects)
    }

    @Test
    fun `새로 검색하면 선택이 풀리고, 결과 지우기는 모두 초기화한다`() = runTest {
        places.categoryResult = AppResult.Success(listOf(place("1")))
        viewModel.toggleCategory(PlaceCategory.CAFE, Fixtures.cityHall)
        viewModel.selectPlace("1")

        viewModel.toggleCategory(PlaceCategory.PHARMACY, Fixtures.cityHall)
        assertNull(state.selectedPlace)

        viewModel.onQueryChange("약국")
        viewModel.clearResults()
        assertEquals(PlaceSearchUiState(recentQueries = state.recentQueries), state)
    }

    @Test
    fun `최근 검색어를 하나 또는 전부 지운다`() = runTest {
        viewModel.searchRecent("강남역", center = null)

        viewModel.removeRecent("서울역")
        assertEquals(listOf("강남역"), state.recentQueries)

        viewModel.clearRecents()
        assertEquals(emptyList<String>(), recents.saved)
    }

    @Test
    fun `추천 검색은 입력이 300ms 멈춘 뒤 마지막 글자로 한 번만 찾는다`() = runTest {
        val found = listOf(place("1"), place("2"))
        places.keywordResult = AppResult.Success(found)
        viewModel.openSearchPage(center = Fixtures.cityHall)

        listOf("현", "현대", "현대백", "현대백화점").forEach {
            viewModel.onQueryChange(it)
            advanceTimeBy(100)
        }
        assertEquals("아직 입력 중이면 부르지 않는다", emptyList<String>(), places.keywordQueries)

        advanceTimeBy(PlaceSearchViewModel.SUGGESTION_DEBOUNCE_MILLIS)

        assertEquals(listOf("현대백화점"), places.keywordQueries)
        assertEquals(Fixtures.cityHall, places.lastNear)
        assertEquals(found, state.suggestions)
    }

    @Test
    fun `공백만 바뀌면 다시 찾지 않고, 다 지우면 바로 추천을 비운다`() = runTest {
        places.keywordResult = AppResult.Success(listOf(place("1")))
        viewModel.openSearchPage(center = null)
        viewModel.onQueryChange("강남역")
        advanceTimeBy(400)

        viewModel.onQueryChange("강남역 ")
        advanceTimeBy(400)
        assertEquals(listOf("강남역"), places.keywordQueries)

        viewModel.onQueryChange("")
        advanceTimeBy(1)
        assertEquals(emptyList<Any>(), state.suggestions)
    }

    @Test
    fun `추천 중 실패하면 에러 없이 목록만 비운다`() = runTest {
        places.keywordResult = AppResult.Error(DomainError.Network)
        val effects = collectEffects()
        viewModel.openSearchPage(center = null)

        viewModel.onQueryChange("강남역")
        advanceTimeBy(400)

        assertEquals(emptyList<Any>(), state.suggestions)
        assertEquals(emptyList<PlaceSearchEffect>(), effects)
    }

    @Test
    fun `추천 장소를 누르면 페이지를 닫고 그 장소를 고르며 최근 검색에 그 검색어를 남긴다`() = runTest {
        val target = place("2", 37.3, 127.3)
        places.keywordResult = AppResult.Success(listOf(place("1"), target))
        val effects = collectEffects()
        viewModel.openSearchPage(center = null)
        viewModel.onQueryChange("현대백화점")
        advanceTimeBy(400)

        viewModel.selectSuggestion(target)

        assertFalse(state.isSearchPageVisible)
        assertEquals(SearchSource.Keyword("현대백화점"), state.source)
        assertEquals(2, state.places.size)
        assertEquals(target, state.selectedPlace)
        assertEquals(listOf(PlaceSearchEffect.Focus(target.location)), effects)
        assertEquals("현대백화점", state.recentQueries.first())
    }

    @Test
    fun `입력 중에 이전 추천을 고르면 그 추천을 찾은 검색어를 남긴다`() = runTest {
        val target = place("1")
        places.keywordResult = AppResult.Success(listOf(target))
        viewModel.openSearchPage(center = null)
        viewModel.onQueryChange("카페")
        advanceTimeBy(400)
        viewModel.onQueryChange("카페 강") // 아직 새 추천은 오지 않았다

        viewModel.selectSuggestion(target)

        assertEquals(SearchSource.Keyword("카페"), state.source)
        assertEquals("카페", state.recentQueries.first())
    }

    @Test
    fun `지금 입력의 추천이 비었을 때만 결과 없음`() = runTest {
        places.keywordResult = AppResult.Success(emptyList())
        viewModel.openSearchPage(center = null)

        viewModel.onQueryChange("없는곳")
        assertFalse("아직 받는 중", state.isSuggestionEmpty)
        advanceTimeBy(400)

        assertTrue(state.isSuggestionEmpty)
    }

    @Test
    fun `선택한 장소는 최근 장소에 남는다`() = runTest {
        val target = place("2")

        viewModel.select(target)
        advanceUntilIdle()

        assertEquals(listOf("2"), recentPlaces.saved.map { it.place.id })
    }
}
