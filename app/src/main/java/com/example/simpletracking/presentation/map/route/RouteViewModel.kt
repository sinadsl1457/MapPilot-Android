package com.example.simpletracking.presentation.map.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.RouteEndpoint
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

/**
 * 길찾기 결과. 진입: start(장소 상세 "길찾기") · plan(길찾기 준비 화면) / 변경: changeOrigin · changeDestination · swap.
 * 시작하면 세 이동 수단을 동시에 조회해 탭마다 소요 시간을 보여준다(네이버 지도 방식).
 * 대중교통은 후보가 여럿이라 목록에서 고른 뒤 지도에 그린다(showRoute / backToList).
 * 자동차·도보는 경로가 하나라 그 탭을 고르면 바로 지도에 그린다(selectMode).
 * 화면: RouteListPage(목록)와 MapScreen(고른 경로)이 이 ViewModel 하나를 같이 본다.
 */
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

    /** 길찾기 준비 화면에서 정한 두 지점과 이동 수단으로 찾는다. */
    fun plan(plan: RoutePlan, myLocation: GeoPoint?) {
        _uiState.update { it.copy(selectedMode = plan.mode) }
        search(plan.origin, plan.destination, myLocation)
    }

    /** 장소 상세의 "길찾기". 출발지는 내 위치다. [myLocation]을 아직 모르면 시작하지 않는다. */
    fun start(destination: Place, myLocation: GeoPoint?) =
        search(RouteEndpoint.MyLocation, RouteEndpoint.PlaceEndpoint(destination), myLocation)

    /** 길찾기 중 출발지 줄 탭 → 선택 페이지에서 고른 값. */
    fun changeOrigin(origin: RouteEndpoint, myLocation: GeoPoint?) {
        val destination = _uiState.value.destination ?: return
        search(origin, destination, myLocation)
    }

    /** 길찾기 중 도착지 줄 탭 → 선택 페이지에서 고른 값. */
    fun changeDestination(destination: RouteEndpoint, myLocation: GeoPoint?) {
        val origin = _uiState.value.origin ?: return
        search(origin, destination, myLocation)
    }

    /** 출발지와 도착지를 맞바꾼다. */
    fun swap(myLocation: GeoPoint?) {
        val state = _uiState.value
        search(state.destination ?: return, state.origin ?: return, myLocation)
    }

    /**
     * 두 지점의 좌표를 정하고 세 이동 수단을 동시에 조회한다.
     * 좌표를 정할 수 없거나 너무 가까우면 상태를 바꾸지 않고 안내만 한다.
     */
    private fun search(origin: RouteEndpoint, destination: RouteEndpoint, myLocation: GeoPoint?) {
        val from = origin.point(myLocation)
        val to = destination.point(myLocation)
        if (from == null || to == null) {
            _effects.trySend(RouteEffect.ShowMessage(R.string.message_locating))
            return
        }
        // 너무 가까우면 어떤 수단으로도 찾을 필요가 없으므로 화면을 열지 않는다.
        if (isTooCloseForRoute(from, to)) {
            _effects.trySend(RouteEffect.ShowError(DomainError.DestinationTooClose))
            return
        }

        fetchJob?.cancel()
        _uiState.update {
            it.copy(
                origin = origin,
                destination = destination,
                originPoint = from,
                destinationPoint = to,
                selectedTransitIndex = 0,
                // 새로 찾으면 대중교통은 목록 단계부터, 자동차·도보는 바로 지도에 그린다(결과가 오면 카메라를 맞춘다).
                isShowingRoute = drawsDirectly(it.selectedMode),
                results = TravelMode.entries.associateWith { ModeResult.Loading },
            )
        }
        fetchJob = viewModelScope.launch {
            // 자식 코루틴 세 개가 동시에 돈다. fetchJob을 취소하면 셋 다 취소된다.
            TravelMode.entries.forEach { mode ->
                launch {
                    val result = fetch(mode, from, to)
                    _uiState.update { it.copy(results = it.results + (mode to result)) }
                    val state = _uiState.value
                    if (mode == state.selectedMode && state.isShowingRoute) fitSelected()
                }
            }
        }
    }

    /**
     * 이동 수단 탭(목록 화면, 지도 위 탭). 다시 조회하지 않는다.
     * 대중교통은 목록 단계로, 자동차·도보는 경로가 하나라 바로 지도에 그리고 카메라를 맞춘다.
     */
    fun selectMode(mode: TravelMode) {
        if (mode == _uiState.value.selectedMode) return
        _uiState.update { it.copy(selectedMode = mode, selectedTransitIndex = 0, isShowingRoute = drawsDirectly(mode)) }
        fitSelected()
    }

    /** 목록에서 경로 탭. 지금 탭의 [index]번째 경로를 지도에 그리고 카메라를 맞춘다. */
    fun showRoute(index: Int = 0) {
        _uiState.update { it.copy(selectedTransitIndex = index, isShowingRoute = true) }
        fitSelected()
    }

    /** 대중교통 경로를 보던 중 ‹(뒤로). 지도 선을 지우고 목록 단계로 돌아간다. */
    fun backToList() {
        _uiState.update { it.copy(selectedMode = TravelMode.TRANSIT, isShowingRoute = false) }
    }

    /** 길찾기 카드·목록의 ✕. 이동 수단 선택은 남겨 두고 나머지를 지운다. */
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

    private companion object {
        /** 고를 후보 없이 바로 지도에 그리는 수단(카카오 도보 API는 경로 하나만 준다). */
        fun drawsDirectly(mode: TravelMode) = mode != TravelMode.TRANSIT
    }
}
