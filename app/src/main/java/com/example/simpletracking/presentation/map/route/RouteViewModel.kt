package com.example.simpletracking.presentation.map.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.TravelMode
import com.example.simpletracking.domain.result.fold
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.domain.usecase.FindCarRouteUseCase
import com.example.simpletracking.domain.usecase.FindTransitRoutesUseCase
import com.example.simpletracking.domain.usecase.FindWalkingRouteUseCase
import com.example.simpletracking.domain.usecase.isTooCloseForRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 길찾기를 시작하면 세 이동 수단을 동시에 조회해 탭마다 소요 시간을 보여준다(네이버 지도 방식). */
@HiltViewModel
class RouteViewModel @Inject constructor(
    private val findCarRoute: FindCarRouteUseCase,
    private val findWalkingRoute: FindWalkingRouteUseCase,
    private val findTransitRoutes: FindTransitRoutesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RouteEffect>(Channel.BUFFERED)
    val effects: Flow<RouteEffect> = _effects.receiveAsFlow()

    /** 종료하거나 다시 찾으면 취소한다. 늦게 도착한 이전 결과가 화면을 바꾸지 않는다. */
    private var fetchJob: Job? = null

    /** @param origin 내 위치. 아직 모르면 길찾기를 시작하지 않는다. */
    fun start(destination: Place, origin: GeoPoint?) {
        if (origin == null) {
            _effects.trySend(RouteEffect.ShowMessage(R.string.message_locating))
            return
        }
        // 너무 가까우면 어떤 수단으로도 찾을 필요가 없으므로 화면을 열지 않는다.
        if (isTooCloseForRoute(origin, destination.location)) {
            _effects.trySend(RouteEffect.ShowError(DomainError.DestinationTooClose))
            return
        }

        fetchJob?.cancel()
        _uiState.update {
            it.copy(
                destination = destination,
                origin = origin,
                selectedTransitIndex = 0,
                results = TravelMode.entries.associateWith { ModeResult.Loading },
            )
        }
        fetchJob = viewModelScope.launch {
            // 자식 코루틴 세 개가 동시에 돈다. fetchJob을 취소하면 셋 다 취소된다.
            TravelMode.entries.forEach { mode ->
                launch {
                    val result = fetch(mode, origin, destination.location)
                    _uiState.update { it.copy(results = it.results + (mode to result)) }
                    if (mode == _uiState.value.selectedMode) fitSelected()
                }
            }
        }
    }

    fun selectMode(mode: TravelMode) {
        if (mode == _uiState.value.selectedMode) return
        _uiState.update { it.copy(selectedMode = mode, selectedTransitIndex = 0) }
        fitSelected()
    }

    fun selectTransitRoute(index: Int) {
        _uiState.update { it.copy(selectedTransitIndex = index) }
        fitSelected()
    }

    /** 이동 수단 선택은 남겨 두고 나머지를 지운다. */
    fun close() {
        fetchJob?.cancel()
        _uiState.update { RouteUiState(selectedMode = it.selectedMode) }
    }

    private fun fitSelected() {
        _uiState.value.overlay?.let { _effects.trySend(RouteEffect.FitCamera(it.allPoints)) }
    }

    private suspend fun fetch(mode: TravelMode, origin: GeoPoint, destination: GeoPoint): ModeResult = when (mode) {
        TravelMode.CAR -> findCarRoute(origin, destination).fold(ModeResult::Failed, ModeResult::Found)
        TravelMode.WALK -> findWalkingRoute(origin, destination).fold(ModeResult::Failed, ModeResult::Found)
        TravelMode.TRANSIT -> findTransitRoutes(origin, destination).fold(ModeResult::Failed, ModeResult::Transit)
    }
}
