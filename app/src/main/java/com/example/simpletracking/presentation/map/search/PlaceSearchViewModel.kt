package com.example.simpletracking.presentation.map.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.PlaceCategory
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.usecase.RecentSearchesUseCase
import com.example.simpletracking.domain.usecase.SearchPlacesByCategoryUseCase
import com.example.simpletracking.domain.usecase.SearchPlacesUseCase
import com.example.simpletracking.domain.usecase.normalizeQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 키워드·카테고리 검색과 결과 선택. 검색 페이지, 하단 시트, 결과 마커가 이 상태를 함께 본다. */
@HiltViewModel
class PlaceSearchViewModel @Inject constructor(
    private val searchPlaces: SearchPlacesUseCase,
    private val searchPlacesByCategory: SearchPlacesByCategoryUseCase,
    private val recentSearches: RecentSearchesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceSearchUiState())
    val uiState: StateFlow<PlaceSearchUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PlaceSearchEffect>(Channel.BUFFERED)
    val effects: Flow<PlaceSearchEffect> = _effects.receiveAsFlow()

    /** 새 검색을 시작하면 이전 검색을 취소한다. 늦게 도착한 이전 결과가 최신 결과를 덮어쓰지 않는다. */
    private var searchJob: Job? = null

    /** 추천 검색용 입력 스트림. 검색창의 글자가 바뀔 때마다 흘려보낸다. */
    private val queryInput = MutableStateFlow("")
    /** 추천 결과의 거리 계산 기준(보통 내 위치). 검색 페이지를 열 때 정한다. */
    private var suggestionCenter: GeoPoint? = null

    init {
        viewModelScope.launch {
            val recents = recentSearches.list()
            _uiState.update { it.copy(recentQueries = recents) }
        }
        observeSuggestions()
    }

    /**
     * 입력이 [SUGGESTION_DEBOUNCE_MILLIS] 동안 멈추면 그 글자로 장소를 찾아 추천 목록을 바꾼다(RxDart의 debounce + switchMap).
     * - debounce: 글자마다 API를 부르지 않는다. 다 지웠을 때는 기다리지 않고 바로 비운다.
     * - distinctUntilChanged: 공백만 바뀌는 등 정리하면 같은 검색어면 다시 부르지 않는다.
     * - mapLatest: 응답을 기다리는 중에 또 입력하면 이전 요청을 취소해, 늦게 온 옛 결과가 화면을 덮지 않는다.
     * 입력 중 실패는 알리지 않고 목록만 비운다(타이핑할 때마다 에러가 뜨면 방해된다).
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeSuggestions() {
        viewModelScope.launch {
            queryInput
                .map(::normalizeQuery)
                .distinctUntilChanged()
                .debounce { if (it.isEmpty()) 0L else SUGGESTION_DEBOUNCE_MILLIS }
                .mapLatest { query ->
                    if (query.isEmpty()) emptyList()
                    else (searchPlaces(query, suggestionCenter) as? AppResult.Success)?.data.orEmpty()
                }
                .collect { suggestions -> _uiState.update { it.copy(suggestions = suggestions) } }
        }
    }

    // region 검색 페이지

    /** @param center 추천 결과의 거리 계산 기준(보통 내 위치) */
    fun openSearchPage(center: GeoPoint?) {
        suggestionCenter = center
        setQuery(_uiState.value.keyword.orEmpty())
        _uiState.update { it.copy(isSearchPageVisible = true) }
    }

    fun closeSearchPage() {
        _uiState.update { it.copy(isSearchPageVisible = false) }
    }

    fun onQueryChange(query: String) = setQuery(query)

    /** 추천 장소를 누르면 페이지를 닫고 그 장소를 바로 고른다. 추천 목록 전체는 결과로 지도에 남긴다. */
    fun selectSuggestion(place: Place) {
        beginSearch()
        val state = _uiState.value
        _uiState.update {
            it.copy(
                isSearchPageVisible = false,
                source = SearchSource.Keyword(normalizeQuery(state.query)),
                phase = SearchPhase.Loaded(state.suggestions),
            )
        }
        select(place)
        viewModelScope.launch {
            val recents = recentSearches.add(place.name)
            _uiState.update { it.copy(recentQueries = recents) }
        }
    }

    /**
     * 키워드 검색. 성공하면 검색 페이지를 닫고 결과를 지도에 띄운다. 실패하면 페이지에 머문다.
     * @param center 거리 계산 기준(보통 내 위치)
     */
    fun submit(center: GeoPoint?) {
        beginSearch()
        val query = _uiState.value.query
        _uiState.update { it.copy(isSubmitting = true) }
        searchJob = viewModelScope.launch {
            when (val result = searchPlaces(query, center)) {
                is AppResult.Success -> {
                    val keyword = normalizeQuery(query)
                    val recents = recentSearches.add(keyword)
                    _uiState.update { it.copy(isSubmitting = false, isSearchPageVisible = false, recentQueries = recents) }
                    show(result.data, SearchSource.Keyword(keyword))
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.send(PlaceSearchEffect.ShowError(result.error))
                }
            }
        }
    }

    fun searchRecent(query: String, center: GeoPoint?) {
        onQueryChange(query)
        submit(center)
    }

    fun removeRecent(query: String) {
        viewModelScope.launch {
            val recents = recentSearches.remove(query)
            _uiState.update { it.copy(recentQueries = recents) }
        }
    }

    fun clearRecents() {
        viewModelScope.launch {
            recentSearches.clear()
            _uiState.update { it.copy(recentQueries = emptyList()) }
        }
    }

    // endregion

    // region 카테고리

    /** 같은 칩을 다시 누르면 결과를 지운다. */
    fun toggleCategory(category: PlaceCategory, center: GeoPoint) {
        if (_uiState.value.selectedCategory == category) {
            clearResults()
            return
        }
        beginSearch()
        _uiState.update { it.copy(source = SearchSource.Category(category), phase = SearchPhase.Loading) }
        searchJob = viewModelScope.launch {
            when (val result = searchPlacesByCategory(category, center)) {
                is AppResult.Success -> show(result.data, SearchSource.Category(category))
                is AppResult.Error -> {
                    clearResults()
                    _effects.send(PlaceSearchEffect.ShowError(result.error))
                }
            }
        }
    }

    // endregion

    // region 결과·선택

    fun select(place: Place) {
        _uiState.update { it.copy(selectedPlace = place) }
        _effects.trySend(PlaceSearchEffect.Focus(place.location))
    }

    /** 지도 마커를 눌렀을 때. 지금 결과에 없는 id는 무시한다. */
    fun selectPlace(id: String) {
        _uiState.value.places.firstOrNull { it.id == id }?.let(::select)
    }

    /** 상세에서 목록으로 돌아간다. */
    fun deselectPlace() {
        _uiState.update { it.copy(selectedPlace = null) }
    }

    fun clearResults() {
        searchJob?.cancel()
        setQuery("")
        _uiState.update { it.copy(source = null, phase = null, selectedPlace = null, isSubmitting = false) }
    }

    // endregion

    private fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        queryInput.value = query
    }

    private fun beginSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(selectedPlace = null, isSubmitting = false) }
    }

    private suspend fun show(places: List<Place>, source: SearchSource) {
        _uiState.update { it.copy(source = source, phase = SearchPhase.Loaded(places)) }
        if (places.isNotEmpty()) _effects.send(PlaceSearchEffect.FitCamera(densestCluster(places.map { it.location })))
    }

    companion object {
        const val SUGGESTION_DEBOUNCE_MILLIS = 300L
    }
}
