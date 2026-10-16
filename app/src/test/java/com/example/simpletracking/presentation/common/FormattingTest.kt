package com.example.simpletracking.presentation.common

import com.example.simpletracking.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattingTest {

    @Test
    fun `1km 미만은 m 정수, 이상은 km 소수 첫째 자리`() {
        assertEquals("0m", formatDistance(0.0))
        assertEquals("320m", formatDistance(320.4))
        assertEquals("999m", formatDistance(999.0))
        assertEquals("1.0km", formatDistance(1_000.0))
        assertEquals("1.2km", formatDistance(1_234.0))
    }

    @Test
    fun `km는 100m 단위로 먼저 반올림한다`() {
        // 22850 / 1000 = 22.849999… 라서 그냥 반올림하면 22.8이 된다.
        assertEquals("22.9km", formatDistance(22_850.0))
    }

    @Test
    fun `내 위치를 모르면 거리를 보여주지 않는다`() {
        assertNull(formatDistance(null, Fixtures.gangnamStation))
        assertEquals("8.8km", formatDistance(Fixtures.cityHall, Fixtures.gangnamStation)) // 8,783m
    }

    @Test
    fun `소요 시간은 분 단위로 올린다`() {
        assertEquals(0 to 1, durationParts(0))
        assertEquals(0 to 1, durationParts(45))
        assertEquals(0 to 25, durationParts(1_500))
        assertEquals(1 to 6, durationParts(3_906))
        assertEquals(2 to 0, durationParts(7_200))
    }
}
