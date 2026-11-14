package com.example.simpletracking.domain.repository

import com.example.simpletracking.domain.model.RecentPlace

/** 최근 장소 저장소. 순서·개수 규칙은 RecentPlacesUseCase가 정한다. */
interface RecentPlaceRepository {
    suspend fun load(): List<RecentPlace>
    suspend fun save(places: List<RecentPlace>)
}
