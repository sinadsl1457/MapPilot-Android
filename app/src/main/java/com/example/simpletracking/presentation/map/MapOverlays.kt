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
    /** 종류 기본색 대신 쓸 색(0xRRGGBB). 지하철 노선 색(예: 2호선 초록). */
    val rgb: Int? = null,
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

/**
 * 카메라를 여러 지점에 맞출 때 앱 UI가 지도를 가리는 높이(dp). 지점은 그 안쪽에 보이게 맞춘다.
 * 위: 상단 UI(검색창·칩 또는 길찾기 카드·탭), 아래: 시트가 멈출 높이 또는 경로 카드 높이.
 */
data class MapInsets(val topDp: Float, val bottomDp: Float)
