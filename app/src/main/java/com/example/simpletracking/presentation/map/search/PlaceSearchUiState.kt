package com.example.simpletracking.presentation.map.search

import com.example.simpletracking.domain.usecase.normalizeQuery
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.PlaceCategory
import com.example.simpletracking.domain.result.DomainError

/** 지금 지도에 보이는 검색 결과가 어디서 왔는지 */
sealed interface SearchSource {
    data class Keyword(val keyword: String) : SearchSource
    data class Category(val category: PlaceCategory) : SearchSource
}

sealed interface SearchPhase {
    data object Loading : SearchPhase
    data class Loaded(val places: List<Place>) : SearchPhase
}

data class PlaceSearchUiState(
    /** 검색창에 입력 중인 글자 */
    val query: String = "",
    val isSearchPageVisible: Boolean = false,
    /** 검색 페이지에서 키워드 검색이 진행 중인지. 성공하면 페이지가 닫힌다. */
    val isSubmitting: Boolean = false,
    /** null이면 결과 없음(하단 시트 숨김) */
    val source: SearchSource? = null,
    val phase: SearchPhase? = null,
    val selectedPlace: Place? = null,
    val recentQueries: List<String> = emptyList(),
    /** 검색 페이지에서 입력하는 동안 보여주는 추천 장소(자동완성) */
    val suggestions: List<Place> = emptyList(),
    /** [suggestions]를 찾은 검색어(정리된 형태). 입력 중에는 지금 입력과 다를 수 있다. */
    val suggestionsQuery: String = "",
) {
    /** 지금 입력의 추천 결과가 도착했는데 비어 있을 때만 "결과 없음"을 보인다(받는 중에는 이전 목록을 그대로 둔다). */
    val isSuggestionEmpty: Boolean get() = suggestions.isEmpty() && suggestionsQuery.isNotEmpty() && suggestionsQuery == normalizeQuery(query)

    val places: List<Place> get() = (phase as? SearchPhase.Loaded)?.places.orEmpty()
    val selectedCategory: PlaceCategory? get() = (source as? SearchSource.Category)?.category
    val keyword: String? get() = (source as? SearchSource.Keyword)?.keyword
}

sealed interface PlaceSearchEffect {
    /** 검색 결과가 모두 보이도록 카메라를 맞춘다. */
    data class FitCamera(val points: List<GeoPoint>) : PlaceSearchEffect
    /** 선택한 장소로 카메라를 옮긴다. */
    data class Focus(val point: GeoPoint) : PlaceSearchEffect
    data class ShowError(val error: DomainError) : PlaceSearchEffect
}
