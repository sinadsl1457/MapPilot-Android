package com.example.simpletracking.domain.repository

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.PlaceCategory
import com.example.simpletracking.domain.result.AppResult

/**
 * domain은 "무엇이 필요한지"만 선언하고, "어떻게 가져오는지"(카카오 API, Retrofit)는 data 계층이 구현한다.
 * 의존성 역전: data → domain 방향으로만 의존한다.
 */
interface PlaceRepository {
    /** [near]가 null이면 결과의 distanceMeters도 null이다. */
    suspend fun search(query: String, near: GeoPoint?): AppResult<List<Place>>

    /** [center] 반경 [radiusMeters] 안의 장소를 가까운 순으로 찾는다. */
    suspend fun searchByCategory(category: PlaceCategory, center: GeoPoint, radiusMeters: Int): AppResult<List<Place>>
}
