package com.example.simpletracking.data.mapper

import com.example.simpletracking.data.remote.dto.CarFareDto
import com.example.simpletracking.data.remote.dto.PlaceDocument
import com.example.simpletracking.data.remote.dto.RoadDto
import com.example.simpletracking.data.remote.dto.RouteDto
import com.example.simpletracking.data.remote.dto.RouteSectionDto
import com.example.simpletracking.data.remote.dto.RouteSummaryDto
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.PlaceCategory
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

        val route = dto.toDomainOrNull(origin, destination)!!

        assertEquals(
            listOf(origin, GeoPoint(37.1, 127.1), GeoPoint(37.2, 127.2), GeoPoint(37.3, 127.3), destination),
            route.points,
        )
        assertEquals(1500, route.distanceMeters)
        assertEquals(300, route.durationSeconds)
    }

    @Test
    fun `짝이 맞지 않는 마지막 vertex 값은 버린다`() {
        val dto = RouteDto(
            resultCode = 0,
            summary = RouteSummaryDto(),
            sections = listOf(RouteSectionDto(roads = listOf(RoadDto(listOf(127.1, 37.1, 127.2))))),
        )

        val route = dto.toDomainOrNull(GeoPoint(37.0, 127.0), GeoPoint(37.9, 127.9))!!

        assertEquals(listOf(GeoPoint(37.0, 127.0), GeoPoint(37.1, 127.1), GeoPoint(37.9, 127.9)), route.points)
    }

    @Test
    fun `자동차 경로의 택시비와 통행료를 옮긴다`() {
        val dto = RouteDto(resultCode = 0, summary = RouteSummaryDto(1500, 300, CarFareDto(taxi = 26_800, toll = 1_200)))

        val route = dto.toDomainOrNull(GeoPoint(37.0, 127.0), GeoPoint(37.9, 127.9))!!

        assertEquals(26_800, route.taxiFare)
        assertEquals(1_200, route.tollFare)
    }

    @Test
    fun `요약 정보가 없으면 null`() {
        assertNull(RouteDto(resultCode = 0).toDomainOrNull(GeoPoint(37.0, 127.0), GeoPoint(37.9, 127.9)))
    }

    @Test
    fun `카테고리를 카카오 category_group_code로 바꾼다`() {
        assertEquals(
            listOf("FD6", "CE7", "CS2", "OL7", "PM9", "PK6"),
            PlaceCategory.entries.map { it.kakaoCode },
        )
    }
}
