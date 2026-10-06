package com.example.simpletracking.data.repository

import com.example.simpletracking.data.mapper.toDomain
import com.example.simpletracking.data.remote.api.KakaoNaviApi
import com.example.simpletracking.data.util.safeApiCall
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.repository.RouteRepository
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val api: KakaoNaviApi,
) : RouteRepository {

    override suspend fun findShortestRoute(origin: GeoPoint, destination: GeoPoint): AppResult<Route> {
        val response = safeApiCall {
            api.directions(
                origin = origin.toQueryParam(),
                destination = destination.toQueryParam(),
                priority = "DISTANCE",
            )
        }
        // HTTP는 200이어도 result_code로 실패를 알려주므로 한 번 더 확인한다.
        return when (response) {
            is AppResult.Error -> response
            is AppResult.Success -> {
                val route = response.data.routes.firstOrNull()
                when {
                    route == null -> AppResult.Error(DomainError.RouteNotFound("경로 응답이 비어 있음"))
                    route.resultCode != 0 -> AppResult.Error(DomainError.RouteNotFound(route.resultMsg))
                    else -> AppResult.Success(route.toDomain(origin, destination))
                }
            }
        }
    }

    /** 카카오 API는 "경도,위도" 순서다(위도,경도가 아님에 주의). */
    private fun GeoPoint.toQueryParam() = "$longitude,$latitude"
}
