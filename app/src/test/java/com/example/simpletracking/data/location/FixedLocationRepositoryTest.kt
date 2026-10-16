package com.example.simpletracking.data.location

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.usecase.ObserveLocationUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FixedLocationRepositoryTest {

    private val point = GeoPoint(37.4419, 127.1290)
    private val repository = FixedLocationRepository(point, clock = { 0L })

    @Test
    fun `위도,경도 문자열을 읽는다`() {
        assertEquals(GeoPoint(37.5666, 126.9784), FixedLocationRepository.parse("37.5666,126.9784"))
        assertEquals(GeoPoint(37.5666, 126.9784), FixedLocationRepository.parse(" 37.5666 , 126.9784 "))
    }

    @Test
    fun `형식이 틀리거나 범위를 벗어나면 null`() {
        listOf("", "37.5", "abc,127", "37.5,127.0,1", "91,127", "37,181").forEach {
            assertNull(it, FixedLocationRepository.parse(it))
        }
    }

    @Test
    fun `권한은 항상 허용이고 고정 위치를 바로 내보낸다`() = runTest {
        assertTrue(repository.hasPermission())
        assertEquals(point, repository.observeLocation().first().point)
    }

    @Test
    fun `정확도 필터를 통과해 내 위치로 쓰인다`() = runTest {
        assertEquals(point, ObserveLocationUseCase(repository)().first().point)
    }
}
