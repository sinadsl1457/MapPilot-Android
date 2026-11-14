package com.example.simpletracking.presentation.map.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.domain.usecase.RecentPlacesUseCase
import com.example.simpletracking.domain.usecase.RecentSearchesUseCase
import com.example.simpletracking.domain.usecase.SearchPlacesUseCase
import com.example.simpletracking.domain.usecase.normalizeQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EndpointSearchUiState(
    val query: String = "",
    val isSubmitting: Boolean = false,
    /**
     * null이면 아직 검색하지 않았다(최근 검색어를 보여준다).
     * 입력 중 새 결과가 올 때까지는 이전 결과를 그대로 둔다(깜빡임 없음).
     */
    val results: List<Place>? = null,
    /** [results]를 찾은 검색어(정리된 형태). 그 목록에서 장소를 고르면 이 검색어를 최근 검색어에 남긴다. */
    val resultsQuery: String = "",
    /** 마지막으로 찾기로 한 검색어(정리된 형태). 공백만 바뀌어 같으면 다시 찾지 않는다. */
    val searchedFor: String = "",
    val recentQueries: List<String> = emptyList(),
)

/**
 * 길찾기 출발지·도착지를 고르는 검색(EndpointPickerPage). 지도 검색(PlaceSearchViewModel)과 달리
 * 결과를 지도에 띄우지 않고 페이지 안 목록에서 바로 고른다. 입력 중 실시간 검색은 지도 검색 추천과 같은 규칙.
 * Activity와 같이 사는 ViewModel이라 페이지를 열 때마다 [open]으로 처음 상태로 되돌린다.
 */
@HiltViewModel
class EndpointSearchViewModel @Inject constructor(
    private val searchPlaces: SearchPlacesUseCase,
    private val recentSearches: RecentSearchesUseCase,
    private val recentPlaces: RecentPlacesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EndpointSearchUiState())
    val uiState: StateFlow<EndpointSearchUiState> = _uiState.asStateFlow()

    /** 엔터 검색 실패만 알린다(실시간 검색 실패는 조용히 넘긴다). */
    private val _errors = Channel<DomainError>(Channel.BUFFERED)
    val errors: Flow<DomainError> = _errors.receiveAsFlow()

    /** 대기 중이거나 진행 중인 검색(실시간·엔터 공통). 새 입력·엔터·페이지 열기 때 취소해 늦게 온 결과를 버린다. */
    private var searchJob: Job? = null

    /** 선택 페이지를 열 때. 지난 입력을 지우고 최근 검색어를 불러온다. */
    fun open() {
        searchJob?.cancel()
        _uiState.value = EndpointSearchUiState()
        viewModelScope.launch {
            val recents = recentSearches.list()
            _uiState.update { it.copy(recentQueries = recents) }
        }
    }

    /**
     * 입력칸이 바뀔 때마다(실시간 검색). [center]는 거리 계산 기준(보통 내 위치).
     * 입력이 [SEARCH_DELAY_MILLIS] 동안 멈추면 찾는다. 연달아 입력하면 마지막 것만 요청한다.
     * 입력을 다 지우면 최근 검색어로 돌아간다.
     */
    fun onQueryChange(query: String, center: GeoPoint?) {
        _uiState.update { it.copy(query = query) }
        val normalized = normalizeQuery(query)
        if (normalized == _uiState.value.searchedFor) return
        searchJob?.cancel()
        if (normalized.isEmpty()) {
            _uiState.update { it.copy(searchedFor = "", results = null, resultsQuery = "") }
            return
        }
        _uiState.update { it.copy(searchedFor = normalized) }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MILLIS)
            val result = searchPlaces(normalized, center)
            if (result is AppResult.Success) {
                _uiState.update { it.copy(results = result.data, resultsQuery = normalized) }
            }
        }
    }

    /** 키보드 엔터. 기다리지 않고 바로 찾고, 실패하면 오류를 보인다. */
    fun submit(center: GeoPoint?) {
        searchJob?.cancel()
        val query = _uiState.value.query
        val normalized = normalizeQuery(query)
        _uiState.update { it.copy(isSubmitting = true, searchedFor = normalized) }
        searchJob = viewModelScope.launch {
            when (val result = searchPlaces(query, center)) {
                is AppResult.Success -> {
                    val recents = recentSearches.add(normalized)
                    _uiState.update {
                        it.copy(results = result.data, resultsQuery = normalized, recentQueries = recents, isSubmitting = false)
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _errors.send(result.error)
                }
            }
        }
    }

    /** 최근 검색어 탭. */
    fun searchRecent(recent: String, center: GeoPoint?) {
        _uiState.update { it.copy(query = recent) }
        submit(center)
    }

    /** 결과 목록에서 장소 탭. 고른 장소는 최근 장소에, 그 목록을 찾은 검색어는 최근 검색어에 남긴다. */
    fun remember(place: Place) {
        val keyword = _uiState.value.resultsQuery
        viewModelScope.launch {
            recentPlaces.add(place)
            if (keyword.isNotEmpty()) recentSearches.add(keyword)
        }
    }

    /** 최근 검색어 줄의 X. */
    fun removeRecent(recent: String) {
        viewModelScope.launch {
            val recents = recentSearches.remove(recent)
            _uiState.update { it.copy(recentQueries = recents) }
        }
    }

    companion object {
        const val SEARCH_DELAY_MILLIS = 300L
    }
}
