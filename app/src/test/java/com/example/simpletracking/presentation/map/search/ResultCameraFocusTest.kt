package com.example.simpletracking.presentation.map.search

import com.example.simpletracking.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultCameraFocusTest {

    private val daechi = GeoPoint(37.5067, 127.0651)
    private val hongdae1 = GeoPoint(37.5510, 126.9210)
    private val hongdae2 = GeoPoint(37.5509, 126.9211)
    private val hongdaeFlower = GeoPoint(37.5742, 126.9258) // 홍대에서 약 2.6km
    private val chuncheon = GeoPoint(37.8728, 127.7024)

    @Test
    fun `흩어진 결과는 가장 많이 모인 곳 주변만 맞춘다`() {
        val result = densestCluster(listOf(daechi, hongdae1, hongdae2, hongdaeFlower, chuncheon))

        assertEquals(listOf(hongdae1, hongdae2, hongdaeFlower), result)
    }

    @Test
    fun `모두 가까우면 전부 맞춘다`() {
        val points = listOf(hongdae1, hongdae2, hongdaeFlower)

        assertEquals(points, densestCluster(points))
    }

    @Test
    fun `모인 정도가 같으면 검색 순위가 높은 쪽을 고른다`() {
        assertEquals(listOf(daechi), densestCluster(listOf(daechi, chuncheon)))
    }

    @Test
    fun `결과가 0개나 1개면 그대로 둔다`() {
        assertEquals(emptyList<GeoPoint>(), densestCluster(emptyList()))
        assertEquals(listOf(chuncheon), densestCluster(listOf(chuncheon)))
    }
}
