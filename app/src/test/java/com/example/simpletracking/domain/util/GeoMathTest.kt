package com.example.simpletracking.domain.util

import com.example.simpletracking.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {

    @Test
    fun `서울시청에서 강남역까지 약 8,785m`() {
        val cityHall = GeoPoint(37.5666, 126.9784)
        val gangnam = GeoPoint(37.4979, 127.0276)

        assertEquals(8_785.0, GeoMath.distanceMeters(cityHall, gangnam), 1.0)
    }

    @Test
    fun `위도 0,0001도 차이는 약 11m`() {
        val a = GeoPoint(37.5, 127.0)
        val b = GeoPoint(37.5001, 127.0)

        assertEquals(11.12, GeoMath.distanceMeters(a, b), 0.01)
    }

    @Test
    fun `같은 좌표는 0m`() {
        val p = GeoPoint(37.5, 127.0)
        assertEquals(0.0, GeoMath.distanceMeters(p, p), 0.0)
    }
}
