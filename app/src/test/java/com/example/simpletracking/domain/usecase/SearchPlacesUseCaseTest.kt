package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.repository.PlaceRepository
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchPlacesUseCaseTest {

    /** mockito 대신 직접 만든 Fake. 인터페이스라서 가능하다(Repository를 인터페이스로 둔 이유 중 하나). */
    private class FakePlaceRepository : PlaceRepository {
        var lastQuery: String? = null
        override suspend fun search(query: String, near: GeoPoint?): AppResult<List<Place>> {
            lastQuery = query
            return AppResult.Success(emptyList())
        }
    }

    private val repository = FakePlaceRepository()
    private val searchPlaces = SearchPlacesUseCase(repository)

    @Test
    fun `공백뿐인 검색어는 API를 호출하지 않고 EmptyQuery 에러`() = runTest {
        val result = searchPlaces("   ", near = null)

        assertEquals(AppResult.Error(DomainError.EmptyQuery), result)
        assertNull(repository.lastQuery)
    }

    @Test
    fun `검색어 앞뒤 공백을 자르고 연속 공백은 하나로 합친다`() = runTest {
        searchPlaces("  강남역   맛집 ", near = null)

        assertEquals("강남역 맛집", repository.lastQuery)
    }
}
