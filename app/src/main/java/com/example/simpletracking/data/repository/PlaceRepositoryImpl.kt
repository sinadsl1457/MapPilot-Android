package com.example.simpletracking.data.repository

import com.example.simpletracking.data.mapper.kakaoCode
import com.example.simpletracking.data.mapper.toDomainOrNull
import com.example.simpletracking.data.remote.api.KakaoLocalApi
import com.example.simpletracking.data.util.safeApiCall
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.PlaceCategory
import com.example.simpletracking.domain.repository.PlaceRepository
import com.example.simpletracking.domain.result.AppResult
import javax.inject.Inject

class PlaceRepositoryImpl @Inject constructor(
    private val api: KakaoLocalApi,
) : PlaceRepository {

    override suspend fun search(query: String, near: GeoPoint?): AppResult<List<Place>> = safeApiCall {
        api.searchKeyword(query, longitude = near?.longitude, latitude = near?.latitude)
            .documents
            .mapNotNull { it.toDomainOrNull() }
    }

    override suspend fun searchByCategory(
        category: PlaceCategory,
        center: GeoPoint,
        radiusMeters: Int,
    ): AppResult<List<Place>> = safeApiCall {
        api.searchCategory(category.kakaoCode, longitude = center.longitude, latitude = center.latitude, radiusMeters = radiusMeters)
            .documents
            .mapNotNull { it.toDomainOrNull() }
    }
}
