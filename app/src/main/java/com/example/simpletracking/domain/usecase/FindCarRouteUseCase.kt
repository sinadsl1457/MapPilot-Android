package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.repository.RouteRepository
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import javax.inject.Inject

class FindCarRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
) {
    suspend operator fun invoke(origin: GeoPoint, destination: GeoPoint): AppResult<Route> {
        if (isTooCloseForRoute(origin, destination)) return AppResult.Error(DomainError.DestinationTooClose)
        return routeRepository.findCarRoute(origin, destination)
    }
}
