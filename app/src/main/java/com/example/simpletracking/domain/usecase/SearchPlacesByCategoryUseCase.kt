package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.PlaceCategory
import com.example.simpletracking.domain.repository.PlaceRepository
import com.example.simpletracking.domain.result.AppResult
import javax.inject.Inject

class SearchPlacesByCategoryUseCase @Inject constructor(
    private val placeRepository: PlaceRepository,
) {
    suspend operator fun invoke(category: PlaceCategory, center: GeoPoint): AppResult<List<Place>> =
        placeRepository.searchByCategory(category, center, RADIUS_METERS)

    companion object {
        const val RADIUS_METERS = 1_000
    }
}
