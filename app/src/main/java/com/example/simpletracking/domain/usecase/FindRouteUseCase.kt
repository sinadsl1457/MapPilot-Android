package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.repository.RouteRepository
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.domain.util.GeoMath
import javax.inject.Inject

class FindRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
) {
    suspend operator fun invoke(origin: GeoPoint, destination: GeoPoint): AppResult<Route> {
        // 이미 도착한 거리라면 API를 호출하지 않는다(호출 한도 절약 + 카카오 API도 5m 이내는 에러를 준다).
        if (GeoMath.distanceMeters(origin, destination) < MIN_ROUTE_DISTANCE_METERS) {
            return AppResult.Error(DomainError.DestinationTooClose)
        }
        return routeRepository.findShortestRoute(origin, destination)
    }

    companion object {
        const val MIN_ROUTE_DISTANCE_METERS = 20.0
    }
}
