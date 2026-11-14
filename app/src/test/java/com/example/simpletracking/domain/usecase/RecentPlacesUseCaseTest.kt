package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.RecentPlace
import com.example.simpletracking.testutil.FakeRecentPlaceRepository
import com.example.simpletracking.testutil.place
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecentPlacesUseCaseTest {

    private val repository = FakeRecentPlaceRepository()
    private val useCase = RecentPlacesUseCase(repository)

    @Test
    fun `최신순이고 같은 장소는 한 번만 남는다`() = runTest {
        useCase.add(place("a"), nowMillis = 1)
        useCase.add(place("b"), nowMillis = 2)

        val places = useCase.add(place("a"), nowMillis = 3)

        assertEquals(listOf("a", "b"), places.map { it.place.id })
        assertEquals(3L, places.first().usedAtMillis)
        assertEquals(places, repository.saved)
    }

    @Test
    fun `최대 개수까지만 둔다`() = runTest {
        var places = emptyList<RecentPlace>()
        repeat(RecentPlacesUseCase.MAX_COUNT + 1) { places = useCase.add(place("$it"), nowMillis = it.toLong()) }

        assertEquals(RecentPlacesUseCase.MAX_COUNT, places.size)
        assertEquals("${RecentPlacesUseCase.MAX_COUNT}", places.first().place.id)
        assertFalse(places.any { it.place.id == "0" })
    }

    @Test
    fun `지우면 그 장소만 빠진다`() = runTest {
        repository.saved = listOf(RecentPlace(place("a"), 1), RecentPlace(place("b"), 2))

        assertEquals(listOf("b"), useCase.remove("a").map { it.place.id })
    }
}
