package com.example.simpletracking.data.mapper

import com.example.simpletracking.data.remote.dto.NamedDto
import com.example.simpletracking.data.remote.dto.PathDto
import com.example.simpletracking.data.remote.dto.TransitRouteDto
import com.example.simpletracking.data.remote.dto.TransitStepDto
import com.example.simpletracking.data.remote.dto.WalkRouteDto
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.model.TransitLeg
import com.example.simpletracking.domain.model.TransitRoute
import com.example.simpletracking.domain.result.DomainError

/** [[경도, 위도]] → GeoPoint. 값이 2개 미만인 항목은 버린다. */
fun PathDto?.toGeoPoints(): List<GeoPoint> =
    this?.points.orEmpty().mapNotNull { pair ->
        if (pair.size >= 2) GeoPoint(latitude = pair[1], longitude = pair[0]) else null
    }

fun WalkRouteDto.toDomain(origin: GeoPoint, destination: GeoPoint): Route {
    val roadPoints = legs.flatMap { it.steps }.flatMap { it.path.toGeoPoints() }
    return Route(
        points = listOf(origin) + roadPoints + destination,
        distanceMeters = properties.totalDistance,
        durationSeconds = properties.totalTime,
    )
}

fun TransitRouteDto.toDomain(): TransitRoute = TransitRoute(
    kind = when (properties.type) {
        "BUS" -> TransitRoute.Kind.BUS
        "SUBWAY" -> TransitRoute.Kind.SUBWAY
        else -> TransitRoute.Kind.BUS_AND_SUBWAY
    },
    durationSeconds = properties.totalTime,
    distanceMeters = properties.totalDistance,
    transfers = properties.transfers,
    fare = properties.fare?.value,
    legs = steps.map { it.toDomain() },
)

fun TransitStepDto.toDomain(): TransitLeg = TransitLeg(
    kind = when (properties.type) {
        "BUS" -> TransitLeg.Kind.BUS
        "SUBWAY" -> TransitLeg.Kind.SUBWAY
        // 모르는 종류는 도보로 본다.
        else -> TransitLeg.Kind.WALK
    },
    durationSeconds = properties.time,
    distanceMeters = properties.distance,
    guidance = properties.guidance.orEmpty(),
    // 같은 노선이 일반·급행으로 두 번 오기도 해서 이름 기준으로 중복을 지운다.
    lineNames = properties.vehicles.orEmpty().map(NamedDto::name).distinct(),
    stops = properties.stops.orEmpty().map(NamedDto::name),
    points = path.toGeoPoints(),
)

/** 도보·대중교통 실패 status → DomainError */
fun routingErrorForStatus(status: String): DomainError = when (status) {
    "SAME_POINT", "EQUAL_POINTS" -> DomainError.DestinationTooClose
    else -> DomainError.RouteNotFound(status)
}
