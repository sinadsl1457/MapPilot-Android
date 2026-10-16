package com.example.simpletracking.domain.model

data class Route(
    /** 출발지 → 도로 경로 → 도착지 순서의 좌표 목록 */
    val points: List<GeoPoint>,
    val distanceMeters: Int,
    val durationSeconds: Int,
    /** 자동차 경로의 예상 택시비(원). 도보 경로에는 없다. */
    val taxiFare: Int? = null,
    /** 자동차 경로의 통행료(원) */
    val tollFare: Int? = null,
)
