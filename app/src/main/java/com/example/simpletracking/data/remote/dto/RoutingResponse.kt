package com.example.simpletracking.data.remote.dto

import kotlinx.serialization.Serializable

// GET /v2/routing/walk, GET /v2/routing/publictraffic 응답. 필드 이름이 이미 camelCase라 @SerialName이 필요 없다.

@Serializable
data class PathDto(
    /** [[경도, 위도], ...] */
    val points: List<List<Double>> = emptyList(),
)

// region 도보

@Serializable
data class WalkResponse(
    /** OK, SAME_POINT, TOO_FAR_AWAY, ROUTE_RESULT_NOT_FOUND 등 */
    val status: String,
    val route: WalkRouteDto? = null,
)

@Serializable
data class WalkRouteDto(
    val properties: WalkRoutePropertiesDto,
    val legs: List<WalkLegDto> = emptyList(),
)

@Serializable
data class WalkRoutePropertiesDto(
    val totalDistance: Int = 0,
    val totalTime: Int = 0,
)

@Serializable
data class WalkLegDto(
    val steps: List<WalkStepDto> = emptyList(),
)

@Serializable
data class WalkStepDto(
    val path: PathDto? = null,
)

// endregion

// region 대중교통

@Serializable
data class TransitResponse(
    /** OK, EQUAL_POINTS, NO_RESULTS, STARTNODES_NULL 등 */
    val status: String,
    val routes: List<TransitRouteDto> = emptyList(),
)

@Serializable
data class TransitRouteDto(
    val properties: TransitRoutePropertiesDto,
    val steps: List<TransitStepDto> = emptyList(),
)

@Serializable
data class TransitRoutePropertiesDto(
    /** BUS, SUBWAY, BUS_AND_SUBWAY */
    val type: String = "",
    val totalDistance: Int = 0,
    val totalTime: Int = 0,
    val transfers: Int = 0,
    val fare: FareDto? = null,
)

@Serializable
data class FareDto(
    val value: Int? = null,
)

@Serializable
data class TransitStepDto(
    val properties: TransitStepPropertiesDto,
    val path: PathDto? = null,
)

@Serializable
data class TransitStepPropertiesDto(
    val guidance: String? = null,
    /** BUS, SUBWAY, WALKING */
    val type: String = "",
    val distance: Int = 0,
    val time: Int = 0,
    val stops: List<NamedDto>? = null,
    val vehicles: List<NamedDto>? = null,
)

@Serializable
data class NamedDto(
    val name: String,
)

// endregion
