package com.example.simpletracking.data.mapper

import com.example.simpletracking.data.remote.dto.RouteDto
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Route

/**
 * 길찾기 결과는 "가장 가까운 도로"에서 시작·종료한다.
 * 실제 출발/도착 좌표를 양 끝에 이어 붙여야 지도에서 선이 내 위치와 목적지에 닿는다.
 * 요약 정보가 없으면(실패 응답) null.
 */
fun RouteDto.toDomainOrNull(origin: GeoPoint, destination: GeoPoint): Route? {
    val summary = summary ?: return null
    val roadPoints = sections
        .asSequence()
        .flatMap { it.roads }
        .flatMap { road -> road.vertexes.chunked(2) }
        .filter { it.size == 2 }
        .map { (longitude, latitude) -> GeoPoint(latitude, longitude) }
        .toList()

    return Route(
        points = listOf(origin) + roadPoints + destination,
        distanceMeters = summary.distance,
        durationSeconds = summary.duration,
        taxiFare = summary.fare?.taxi,
        tollFare = summary.fare?.toll,
    )
}
