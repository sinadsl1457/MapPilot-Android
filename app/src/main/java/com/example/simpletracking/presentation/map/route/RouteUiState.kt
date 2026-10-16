package com.example.simpletracking.presentation.map.route

import androidx.annotation.StringRes
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.model.TransitLeg
import com.example.simpletracking.domain.model.TransitRoute
import com.example.simpletracking.domain.model.TravelMode
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.presentation.map.RouteLine
import com.example.simpletracking.presentation.map.RouteOverlay

/** 이동 수단 하나의 조회 결과 */
sealed interface ModeResult {
    data object Loading : ModeResult
    data class Found(val route: Route) : ModeResult
    data class Transit(val routes: List<TransitRoute>) : ModeResult
    data class Failed(val error: DomainError) : ModeResult
}

/** 길찾기 모드 상태. 출발지는 항상 내 위치다. */
data class RouteUiState(
    val destination: Place? = null,
    val origin: GeoPoint? = null,
    /** 마지막으로 고른 이동 수단을 다음 길찾기에서도 유지한다. */
    val selectedMode: TravelMode = TravelMode.TRANSIT,
    val results: Map<TravelMode, ModeResult> = emptyMap(),
    val selectedTransitIndex: Int = 0,
) {
    val isActive: Boolean get() = destination != null

    val selectedResult: ModeResult? get() = results[selectedMode]

    val selectedTransitRoute: TransitRoute?
        get() = (selectedResult as? ModeResult.Transit)?.routes?.getOrNull(selectedTransitIndex)

    /** 지도에 그릴 경로. 아직 결과가 없거나 실패했으면 null. */
    val overlay: RouteOverlay?
        get() {
            val origin = origin ?: return null
            val end = destination?.location ?: return null
            return when (val result = selectedResult) {
                is ModeResult.Found -> {
                    val kind = if (selectedMode == TravelMode.WALK) RouteLine.Kind.WALK else RouteLine.Kind.CAR
                    RouteOverlay(listOf(RouteLine(kind, result.route.points)), origin, end)
                }
                is ModeResult.Transit -> {
                    val route = selectedTransitRoute ?: return null
                    val lines = route.legs.map { RouteLine(it.kind.toLineKind(), it.points) }.toMutableList()
                    // 대중교통 응답은 첫 정류장부터 시작하므로, 출발지·도착지와 이어지는 도보 구간을 직선으로 덧붙인다.
                    lines.firstOrNull()?.points?.firstOrNull()?.takeIf { it != origin }?.let {
                        lines.add(0, RouteLine(RouteLine.Kind.WALK, listOf(origin, it)))
                    }
                    lines.lastOrNull()?.points?.lastOrNull()?.takeIf { it != end }?.let {
                        lines.add(RouteLine(RouteLine.Kind.WALK, listOf(it, end)))
                    }
                    RouteOverlay(lines, origin, end)
                }
                ModeResult.Loading, is ModeResult.Failed, null -> null
            }
        }
}

private fun TransitLeg.Kind.toLineKind() = when (this) {
    TransitLeg.Kind.WALK -> RouteLine.Kind.WALK
    TransitLeg.Kind.BUS -> RouteLine.Kind.BUS
    TransitLeg.Kind.SUBWAY -> RouteLine.Kind.SUBWAY
}

sealed interface RouteEffect {
    /** 경로 전체가 보이도록 카메라를 맞춘다. */
    data class FitCamera(val points: List<GeoPoint>) : RouteEffect
    data class ShowError(val error: DomainError) : RouteEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : RouteEffect
}
