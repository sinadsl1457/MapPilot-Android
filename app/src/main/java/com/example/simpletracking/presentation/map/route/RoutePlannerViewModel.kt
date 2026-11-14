package com.example.simpletracking.presentation.map.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpletracking.domain.model.RecentPlace
import com.example.simpletracking.domain.model.RouteEndpoint
import com.example.simpletracking.domain.model.TravelMode
import com.example.simpletracking.domain.usecase.RecentPlacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 길찾기 준비 화면에서 정한 것. 지도 화면이 받아 길찾기를 실행한다(RouteViewModel.plan). */
data class RoutePlan(
    val origin: RouteEndpoint,
    val destination: RouteEndpoint,
    val mode: TravelMode,
)

data class RoutePlannerUiState(
    /** 처음에는 내 위치. 맞바꾸면 비어 있을 수 있다. */
    val origin: RouteEndpoint? = RouteEndpoint.MyLocation,
    val destination: RouteEndpoint? = null,
    val mode: TravelMode = TravelMode.TRANSIT,
    val recentPlaces: List<RecentPlace> = emptyList(),
)

sealed interface RoutePlannerEffect {
    /** 출발지·도착지가 모두 정해졌다. 화면은 닫고 [plan]으로 길찾기를 한다. */
    data class Completed(val plan: RoutePlan) : RoutePlannerEffect
}

/**
 * 지도 첫 화면의 길찾기 버튼으로 여는 준비 화면(RoutePlannerPage) 로직.
 * 출발지(기본: 내 위치)와 도착지를 정하고, 최근 장소를 바로 도착지로 고를 수 있다.
 * 두 지점이 모두 정해지면 [RoutePlannerEffect.Completed] → 화면이 닫히며 RouteViewModel.plan을 부른다.
 * Activity와 같이 사는 ViewModel이라 화면을 열 때마다 [open]으로 처음 상태로 되돌린다.
 */
@HiltViewModel
class RoutePlannerViewModel @Inject constructor(
    private val recentPlaces: RecentPlacesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutePlannerUiState())
    val uiState: StateFlow<RoutePlannerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RoutePlannerEffect>(Channel.BUFFERED)
    val effects: Flow<RoutePlannerEffect> = _effects.receiveAsFlow()

    /** 준비 화면을 열 때. [mode]는 지난 길찾기에서 고른 이동 수단. */
    fun open(mode: TravelMode) {
        _uiState.value = RoutePlannerUiState(mode = mode)
        viewModelScope.launch {
            val recents = recentPlaces.list()
            _uiState.update { it.copy(recentPlaces = recents) }
        }
    }

    /** 출발지 줄 탭 → 선택 페이지에서 고른 값. 도착지도 있으면 바로 Completed. */
    fun setOrigin(origin: RouteEndpoint) {
        _uiState.update { it.copy(origin = origin) }
        completeIfReady()
    }

    /** 도착지 줄 탭 → 선택 페이지에서 고른 값(또는 chooseRecent). 출발지도 있으면 바로 Completed. */
    fun setDestination(destination: RouteEndpoint) {
        _uiState.update { it.copy(destination = destination) }
        completeIfReady()
    }

    /** 출발지와 도착지를 맞바꾼다. 둘 다 정해져 있으면 바로 길찾기로 넘어간다. */
    fun swap() {
        _uiState.update { it.copy(origin = it.destination, destination = it.origin) }
        completeIfReady()
    }

    /** 이동 수단 아이콘 탭. 결과 화면의 첫 탭이 된다. */
    fun selectMode(mode: TravelMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    /** 최근 장소를 누르면 도착지로 정한다. 목록에서도 맨 앞으로 올린다. */
    fun chooseRecent(recent: RecentPlace) {
        viewModelScope.launch { recentPlaces.add(recent.place) }
        setDestination(RouteEndpoint.PlaceEndpoint(recent.place))
    }

    /** 최근 장소 줄의 X. */
    fun removeRecent(recent: RecentPlace) {
        viewModelScope.launch {
            val recents = recentPlaces.remove(recent.place.id)
            _uiState.update { it.copy(recentPlaces = recents) }
        }
    }

    private fun completeIfReady() {
        val state = _uiState.value
        val origin = state.origin ?: return
        val destination = state.destination ?: return
        _effects.trySend(RoutePlannerEffect.Completed(RoutePlan(origin, destination, state.mode)))
    }
}
