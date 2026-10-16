package com.example.simpletracking.domain.usecase

import com.example.simpletracking.testutil.FakeRecentSearchRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentSearchesUseCaseTest {

    private val repository = FakeRecentSearchRepository()
    private val recents = RecentSearchesUseCase(repository)

    @Test
    fun `가장 최근 검색어가 앞에 온다`() = runTest {
        recents.add("강남역")
        recents.add("서울역")

        assertEquals(listOf("서울역", "강남역"), recents.list())
    }

    @Test
    fun `같은 검색어를 다시 넣으면 맨 앞으로 옮긴다`() = runTest {
        recents.add("강남역")
        recents.add("서울역")
        recents.add("강남역")

        assertEquals(listOf("강남역", "서울역"), repository.saved)
    }

    @Test
    fun `공백을 정규화하고 빈 검색어는 저장하지 않는다`() = runTest {
        recents.add("  강남역   맛집 ")
        recents.add("   ")

        assertEquals(listOf("강남역 맛집"), repository.saved)
    }

    @Test
    fun `최대 10개까지만 남긴다`() = runTest {
        (1..12).forEach { recents.add("검색 $it") }

        assertEquals((12 downTo 3).map { "검색 $it" }, repository.saved)
    }

    @Test
    fun `하나를 지우거나 전체를 지울 수 있다`() = runTest {
        recents.add("강남역")
        recents.add("서울역")

        assertEquals(listOf("강남역"), recents.remove("서울역"))
        recents.clear()
        assertEquals(emptyList<String>(), recents.list())
    }
}
