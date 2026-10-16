package com.example.simpletracking.domain.repository

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.model.TransitRoute
import com.example.simpletracking.domain.result.AppResult

interface RouteRepository {
    /** 자동차 추천 경로 */
    suspend fun findCarRoute(origin: GeoPoint, destination: GeoPoint): AppResult<Route>

    suspend fun findWalkingRoute(origin: GeoPoint, destination: GeoPoint): AppResult<Route>

    /** 대중교통 경로 후보들. 빠른 순서는 보장하지 않는다. */
    suspend fun findTransitRoutes(origin: GeoPoint, destination: GeoPoint): AppResult<List<TransitRoute>>
}
