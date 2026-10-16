package com.example.simpletracking.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DirectionsResponse(
    val routes: List<RouteDto> = emptyList(),
)

@Serializable
data class RouteDto(
    /** 0이면 성공. 그 외(예: 104 출발지-도착지 5m 이내)는 실패 */
    @SerialName("result_code") val resultCode: Int,
    @SerialName("result_msg") val resultMsg: String = "",
    /** 길찾기에 실패하면(resultCode != 0) 내려오지 않는다. */
    val summary: RouteSummaryDto? = null,
    val sections: List<RouteSectionDto> = emptyList(),
)

@Serializable
data class RouteSummaryDto(
    val distance: Int = 0,
    val duration: Int = 0,
    val fare: CarFareDto? = null,
)

@Serializable
data class CarFareDto(
    val taxi: Int? = null,
    val toll: Int? = null,
)

@Serializable
data class RouteSectionDto(
    val roads: List<RoadDto> = emptyList(),
)

@Serializable
data class RoadDto(
    /** [경도, 위도, 경도, 위도, ...] 로 평탄화된 좌표 배열 */
    val vertexes: List<Double> = emptyList(),
)
