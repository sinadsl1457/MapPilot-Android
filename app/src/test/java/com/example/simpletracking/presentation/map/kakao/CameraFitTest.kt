package com.example.simpletracking.presentation.map.kakao

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.presentation.map.MapFitLayout
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraFitTest {

    private val points = listOf(GeoPoint(37.5509, 126.9210), GeoPoint(37.5742, 126.9258))

    /** 화면 높이에서 위쪽부터 잰 위치(0=맨 위, 1=맨 아래) */
    private fun screenY(latitude: Double, corners: Pair<GeoPoint, GeoPoint>): Double {
        val (sw, ne) = corners
        return (ne.latitude - latitude) / (ne.latitude - sw.latitude)
    }

    @Test
    fun `모든 지점이 위 UI와 아래 시트 사이 보이는 띠 안에 들어간다`() {
        MapFitLayout.entries.forEach { layout ->
            val insets = layout.insets
            val corners = fitCorners(points, insets)

            points.forEach { point ->
                val y = screenY(point.latitude, corners)
                assertTrue("$layout: $y 가 위 UI(${insets.top})에 가림", y > insets.top)
                assertTrue("$layout: $y 가 아래 UI(${1 - insets.bottom})에 가림", y < 1 - insets.bottom)
            }
        }
    }

    @Test
    fun `가로로도 지점이 화면 안에 들어간다`() {
        val (sw, ne) = fitCorners(points, MapFitLayout.SEARCH_RESULTS.insets)

        points.forEach { assertTrue(it.longitude > sw.longitude && it.longitude < ne.longitude) }
    }

    @Test
    fun `한 점이어도 범위가 0이 되지 않는다`() {
        val (sw, ne) = fitCorners(listOf(GeoPoint(37.5, 127.0)), MapFitLayout.ROUTE.insets)

        assertTrue(ne.latitude - sw.latitude > 0.002)
        assertTrue(ne.longitude - sw.longitude > 0)
    }
}
