package com.example.simpletracking.domain.repository

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.result.AppResult

interface RouteRepository {
    /** 최단거리 기준 경로 */
    suspend fun findShortestRoute(origin: GeoPoint, destination: GeoPoint): AppResult<Route>
}
