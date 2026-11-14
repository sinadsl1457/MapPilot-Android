package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.RecentPlace
import com.example.simpletracking.domain.repository.RecentPlaceRepository
import javax.inject.Inject

/**
 * 최근에 고른 장소. 가장 최근 것이 앞에 오고, 같은 장소(id)는 한 번만, 최대 [MAX_COUNT]개까지 둔다.
 * 기록하는 곳: PlaceSearchViewModel.select(장소 상세를 봄), EndpointSearchViewModel.remember(출발지·도착지로 고름),
 * RoutePlannerViewModel.chooseRecent(최근 장소를 다시 고름)
 */
class RecentPlacesUseCase @Inject constructor(
    private val repository: RecentPlaceRepository,
) {
    suspend fun list(): List<RecentPlace> = repository.load()

    suspend fun add(place: Place, nowMillis: Long = System.currentTimeMillis()): List<RecentPlace> {
        val places = (listOf(RecentPlace(place, nowMillis)) + repository.load().filter { it.place.id != place.id })
            .take(MAX_COUNT)
        repository.save(places)
        return places
    }

    suspend fun remove(placeId: String): List<RecentPlace> {
        val places = repository.load().filter { it.place.id != placeId }
        repository.save(places)
        return places
    }

    companion object {
        const val MAX_COUNT = 20
    }
}
