package com.example.simpletracking.domain.model

/** 대중교통 경로 하나. 여러 구간(도보·버스·지하철)으로 이루어진다. */
data class TransitRoute(
    val kind: Kind,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val transfers: Int,
    /** 원 단위. 알 수 없으면 null. */
    val fare: Int?,
    val legs: List<TransitLeg>,
) {
    enum class Kind { BUS, SUBWAY, BUS_AND_SUBWAY }
}

data class TransitLeg(
    val kind: Kind,
    val durationSeconds: Int,
    val distanceMeters: Int,
    /** 예: "수인분당선 (태평 > 왕십리)", "왕십리역 환승" */
    val guidance: String,
    /** 탈 수 있는 노선 이름. 버스는 여러 개일 수 있고, 도보면 비어 있다. */
    val lineNames: List<String>,
    /** 지나는 정류장·역 이름. 첫 번째가 타는 곳, 마지막이 내리는 곳이다. */
    val stops: List<String>,
    val points: List<GeoPoint>,
) {
    enum class Kind { WALK, BUS, SUBWAY }
}
