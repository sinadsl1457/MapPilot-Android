package com.example.simpletracking.presentation.map

import com.example.simpletracking.domain.model.GeoPoint

/** 지도에 표시할 검색 결과 하나. 지도 SDK 타입을 쓰지 않는다. */
data class PlaceMarker(
    val id: String,
    val point: GeoPoint,
    /** 목록 순번(1부터) */
    val number: Int,
)

/** 지도에 그릴 경로 선 한 구간. 종류마다 색이 다르다. */
data class RouteLine(
    val kind: Kind,
    val points: List<GeoPoint>,
) {
    enum class Kind { CAR, WALK, BUS, SUBWAY }
}

/** 지도에 그릴 경로 전체: 선들과 출발·도착 마커 위치 */
data class RouteOverlay(
    val lines: List<RouteLine>,
    val start: GeoPoint,
    val end: GeoPoint,
) {
    val allPoints: List<GeoPoint> get() = listOf(start) + lines.flatMap { it.points } + end
}

/** 카메라를 여러 지점에 맞출 때 화면의 어떤 부분이 UI에 가려지는지 */
enum class MapFitLayout {
    /** 위: 검색창 + 칩, 아래: 큰 하단 시트 */
    SEARCH_RESULTS,
    /** 위: 출발·도착 카드와 탭, 아래: 작은 요약 카드 */
    ROUTE,
    /** 위: 출발·도착 카드와 탭, 아래: 큰 하단 시트(대중교통 목록) */
    ROUTE_WITH_SHEET,
}
