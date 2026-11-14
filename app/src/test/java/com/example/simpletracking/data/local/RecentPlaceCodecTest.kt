package com.example.simpletracking.data.local

import com.example.simpletracking.domain.model.RecentPlace
import com.example.simpletracking.testutil.place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentPlaceCodecTest {

    @Test
    fun `저장한 장소를 다시 읽는다 - 거리는 저장하지 않는다`() {
        val original = place("a").copy(distanceMeters = 120)

        val loaded = decodeRecentPlaces(encodeRecentPlaces(listOf(RecentPlace(original, 1_800_000_000_000))))

        assertEquals(1, loaded.size)
        assertEquals(original.copy(distanceMeters = null), loaded.first().place)
        assertEquals(1_800_000_000_000, loaded.first().usedAtMillis)
        assertNull(loaded.first().place.distanceMeters)
    }

    @Test
    fun `값이 없거나 깨져 있으면 빈 목록`() {
        assertEquals(emptyList<RecentPlace>(), decodeRecentPlaces(null))
        assertEquals(emptyList<RecentPlace>(), decodeRecentPlaces("not json"))
    }
}
