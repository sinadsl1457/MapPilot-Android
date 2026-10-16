package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.TransitRoute
import com.example.simpletracking.domain.repository.RouteRepository
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import javax.inject.Inject

class FindTransitRoutesUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
) {
    /** 빠른 순으로 정렬해 돌려준다. 결과가 없으면 RouteNotFound. */
    suspend operator fun invoke(origin: GeoPoint, destination: GeoPoint): AppResult<List<TransitRoute>> {
        if (isTooCloseForRoute(origin, destination)) return AppResult.Error(DomainError.DestinationTooClose)
        return when (val result = routeRepository.findTransitRoutes(origin, destination)) {
            is AppResult.Error -> result
            is AppResult.Success ->
                if (result.data.isEmpty()) AppResult.Error(DomainError.RouteNotFound("대중교통 경로 없음"))
                else AppResult.Success(result.data.sortedBy { it.durationSeconds })
        }
    }
}
