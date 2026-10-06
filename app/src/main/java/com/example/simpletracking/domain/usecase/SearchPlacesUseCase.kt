package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.repository.PlaceRepository
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import javax.inject.Inject

/**
 * @Inject constructor: Hilt에 "이 클래스는 이 생성자로 만들어라"라고 알려준다. injectable의 @injectable과 같다.
 * javax.inject는 Android와 무관한 순수 Java 표준 어노테이션이라 domain에 둬도 순수성이 깨지지 않는다.
 */
class SearchPlacesUseCase @Inject constructor(
    private val placeRepository: PlaceRepository,
) {
    /** operator fun invoke: searchPlaces("강남역")처럼 호출할 수 있다. Dart의 call()과 같다. */
    suspend operator fun invoke(query: String, near: GeoPoint?): AppResult<List<Place>> {
        val normalized = query.trim().replace(WHITESPACES, " ")
        if (normalized.isEmpty()) return AppResult.Error(DomainError.EmptyQuery)
        return placeRepository.search(normalized, near)
    }

    private companion object {
        val WHITESPACES = Regex("\\s+")
    }
}
