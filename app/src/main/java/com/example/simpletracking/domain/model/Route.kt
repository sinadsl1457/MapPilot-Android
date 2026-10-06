package com.example.simpletracking.domain.model

data class Route(
    /** 출발지 → 도로 경로 → 도착지 순서의 좌표 목록 */
    val points: List<GeoPoint>,
    val distanceMeters: Int,
    val durationSeconds: Int,
)
