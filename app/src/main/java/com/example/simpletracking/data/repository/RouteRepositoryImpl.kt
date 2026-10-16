package com.example.simpletracking.data.repository

import com.example.simpletracking.data.mapper.routingErrorForStatus
import com.example.simpletracking.data.mapper.toDomain
import com.example.simpletracking.data.mapper.toDomainOrNull
import com.example.simpletracking.data.remote.api.KakaoNaviApi
import com.example.simpletracking.data.remote.api.KakaoRoutingApi
import com.example.simpletracking.data.util.safeApiCall
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.model.TransitRoute
import com.example.simpletracking.domain.repository.RouteRepository
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val naviApi: KakaoNaviApi,
    private val routingApi: KakaoRoutingApi,
) : RouteRepository {

    override suspend fun findCarRoute(origin: GeoPoint, destination: GeoPoint): AppResult<Route> {
        val response = safeApiCall {
            naviApi.directions(
                origin = origin.toQueryParam(),
                destination = destination.toQueryParam(),
                // 최단 거리(DISTANCE)는 막히는 시내 길로 돌아 실제로 더 오래 걸릴 수 있어 카카오 기본값인 추천 경로를 쓴다.
                priority = "RECOMMEND",
            )
        }
        // HTTP는 200이어도 result_code로 실패를 알려주므로 한 번 더 확인한다.
        return when (response) {
            is AppResult.Error -> response
            is AppResult.Success -> {
                val route = response.data.routes.firstOrNull()
                    ?: return AppResult.Error(DomainError.RouteNotFound("경로 응답이 비어 있음"))
                val mapped = route.takeIf { it.resultCode == 0 }?.toDomainOrNull(origin, destination)
                    ?: return AppResult.Error(DomainError.RouteNotFound(route.resultMsg))
                AppResult.Success(mapped)
            }
        }
    }

    override suspend fun findWalkingRoute(origin: GeoPoint, destination: GeoPoint): AppResult<Route> {
        val response = safeApiCall {
            routingApi.walk(origin.longitude, origin.latitude, destination.longitude, destination.latitude)
        }
        return when (response) {
            is AppResult.Error -> response
            is AppResult.Success -> {
                val route = response.data.route
                if (response.data.status != STATUS_OK || route == null) {
                    AppResult.Error(routingErrorForStatus(response.data.status))
                } else {
                    AppResult.Success(route.toDomain(origin, destination))
                }
            }
        }
    }

    override suspend fun findTransitRoutes(origin: GeoPoint, destination: GeoPoint): AppResult<List<TransitRoute>> {
        val response = safeApiCall {
            routingApi.publicTraffic(origin.longitude, origin.latitude, destination.longitude, destination.latitude)
        }
        return when (response) {
            is AppResult.Error -> response
            is AppResult.Success ->
                if (response.data.status != STATUS_OK) AppResult.Error(routingErrorForStatus(response.data.status))
                else AppResult.Success(response.data.routes.map { it.toDomain() })
        }
    }

    /** 카카오모빌리티 API는 "경도,위도" 순서다(위도,경도가 아님에 주의). */
    private fun GeoPoint.toQueryParam() = "$longitude,$latitude"

    private companion object {
        const val STATUS_OK = "OK"
    }
}
