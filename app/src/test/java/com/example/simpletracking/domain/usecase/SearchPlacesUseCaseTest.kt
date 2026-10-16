package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.PlaceCategory
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.testutil.FakePlaceRepository
import com.example.simpletracking.testutil.Fixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchPlacesUseCaseTest {

    private val repository = FakePlaceRepository()
    private val searchPlaces = SearchPlacesUseCase(repository)

    @Test
    fun `공백뿐인 검색어는 API를 호출하지 않고 EmptyQuery 에러`() = runTest {
        listOf("", "   ", "\n\t").forEach { blank ->
            assertEquals(AppResult.Error(DomainError.EmptyQuery), searchPlaces(blank, near = null))
        }
        assertNull(repository.lastQuery)
    }

    @Test
    fun `검색어 앞뒤 공백을 자르고 연속 공백은 하나로 합친다`() = runTest {
        searchPlaces("  강남역   맛집 ", near = null)

        assertEquals("강남역 맛집", repository.lastQuery)
    }

    @Test
    fun `카테고리 검색은 중심 좌표 반경 1km 안을 찾는다`() = runTest {
        SearchPlacesByCategoryUseCase(repository)(PlaceCategory.CAFE, Fixtures.cityHall)

        assertEquals(PlaceCategory.CAFE, repository.lastCategory)
        assertEquals(Fixtures.cityHall, repository.lastCenter)
        assertEquals(1_000, repository.lastRadius)
    }
}
