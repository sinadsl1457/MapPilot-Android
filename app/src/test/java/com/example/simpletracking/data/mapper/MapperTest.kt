package com.example.simpletracking.data.mapper

import com.example.simpletracking.data.remote.dto.PlaceDocument
import com.example.simpletracking.data.remote.dto.RoadDto
import com.example.simpletracking.data.remote.dto.RouteDto
import com.example.simpletracking.data.remote.dto.RouteSectionDto
import com.example.simpletracking.data.remote.dto.RouteSummaryDto
import com.example.simpletracking.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapperTest {

    private fun document(x: String = "127.0276", y: String = "37.4979") = PlaceDocument(
        id = "1",
        placeName = "강남역",
        categoryName = "교통,수송 > 지하철,전철 > 수도권2호선",
        addressName = "서울 강남구 역삼동 858",
        roadAddressName = "",
        x = x,
        y = y,
        distance = "120",
    )

    @Test
    fun `x는 경도, y는 위도로 매핑하고 카테고리는 마지막 단계만 남긴다`() {
        val place = document().toDomainOrNull()!!

        assertEquals(GeoPoint(latitude = 37.4979, longitude = 127.0276), place.location)
        assertEquals("수도권2호선", place.category)
        assertEquals(120, place.distanceMeters)
    }

    @Test
    fun `도로명 주소가 비어 있으면 지번 주소를 쓴다`() {
        assertEquals("서울 강남구 역삼동 858", document().toDomainOrNull()!!.address)
    }

    @Test
    fun `좌표가 깨진 항목은 null`() {
        assertNull(document(x = "").toDomainOrNull())
    }

    @Test
    fun `평탄화된 vertexes를 (경도,위도) 쌍으로 풀고 양 끝에 출발·도착을 붙인다`() {
        val origin = GeoPoint(37.0, 127.0)
        val destination = GeoPoint(37.9, 127.9)
        val dto = RouteDto(
            resultCode = 0,
            summary = RouteSummaryDto(distance = 1500, duration = 300),
            sections = listOf(
                RouteSectionDto(roads = listOf(RoadDto(listOf(127.1, 37.1, 127.2, 37.2)))),
                RouteSectionDto(roads = listOf(RoadDto(listOf(127.3, 37.3)))),
            ),
        )

        val route = dto.toDomain(origin, destination)

        assertEquals(
            listOf(origin, GeoPoint(37.1, 127.1), GeoPoint(37.2, 127.2), GeoPoint(37.3, 127.3), destination),
            route.points,
        )
        assertEquals(1500, route.distanceMeters)
        assertEquals(300, route.durationSeconds)
    }
}
