package com.example.simpletracking.data.mapper

import com.example.simpletracking.data.remote.dto.FareDto
import com.example.simpletracking.data.remote.dto.NamedDto
import com.example.simpletracking.data.remote.dto.PathDto
import com.example.simpletracking.data.remote.dto.TransitRouteDto
import com.example.simpletracking.data.remote.dto.TransitRoutePropertiesDto
import com.example.simpletracking.data.remote.dto.TransitStepDto
import com.example.simpletracking.data.remote.dto.TransitStepPropertiesDto
import com.example.simpletracking.data.remote.dto.WalkLegDto
import com.example.simpletracking.data.remote.dto.WalkRouteDto
import com.example.simpletracking.data.remote.dto.WalkRoutePropertiesDto
import com.example.simpletracking.data.remote.dto.WalkStepDto
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.TransitLeg
import com.example.simpletracking.domain.model.TransitRoute
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutingMapperTest {

    @Test
    fun `path는 (경도, 위도) 쌍이고 값이 모자란 항목은 버린다`() {
        val points = PathDto(listOf(listOf(127.1, 37.1), listOf(127.2), listOf(127.3, 37.3))).toGeoPoints()

        assertEquals(listOf(GeoPoint(37.1, 127.1), GeoPoint(37.3, 127.3)), points)
    }

    @Test
    fun `도보 경로는 모든 step의 좌표를 이어 붙이고 양 끝에 출발·도착을 붙인다`() {
        val dto = WalkRouteDto(
            properties = WalkRoutePropertiesDto(totalDistance = 1_200, totalTime = 900),
            legs = listOf(
                WalkLegDto(
                    steps = listOf(
                        WalkStepDto(PathDto(listOf(listOf(127.0, 37.0), listOf(127.01, 37.01)))),
                        WalkStepDto(path = null),
                    )
                )
            ),
        )

        val route = dto.toDomain(Fixtures.cityHall, Fixtures.gangnamStation)

        assertEquals(
            listOf(Fixtures.cityHall, GeoPoint(37.0, 127.0), GeoPoint(37.01, 127.01), Fixtures.gangnamStation),
            route.points,
        )
        assertEquals(1_200, route.distanceMeters)
        assertEquals(900, route.durationSeconds)
    }

    @Test
    fun `대중교통 경로의 종류·요금·구간을 옮기고 같은 노선 이름은 한 번만 남긴다`() {
        val dto = TransitRouteDto(
            properties = TransitRoutePropertiesDto("BUS_AND_SUBWAY", totalDistance = 25_000, totalTime = 3_600, transfers = 1, fare = FareDto(1_850)),
            steps = listOf(
                TransitStepDto(
                    TransitStepPropertiesDto(
                        guidance = "수인분당선 (태평 > 왕십리)", type = "SUBWAY", distance = 20_000, time = 1_980,
                        stops = listOf(NamedDto("태평"), NamedDto("모란"), NamedDto("왕십리")),
                        vehicles = listOf(NamedDto("수인분당선"), NamedDto("수인분당선")),
                    ),
                    PathDto(listOf(listOf(127.1, 37.4), listOf(127.0, 37.5))),
                ),
                TransitStepDto(TransitStepPropertiesDto(guidance = null, type = "WALKING", distance = 88, time = 144)),
            ),
        )

        val route = dto.toDomain()

        assertEquals(TransitRoute.Kind.BUS_AND_SUBWAY, route.kind)
        assertEquals(1_850, route.fare)
        assertEquals(1, route.transfers)
        assertEquals(listOf(TransitLeg.Kind.SUBWAY, TransitLeg.Kind.WALK), route.legs.map { it.kind })
        assertEquals(listOf("수인분당선"), route.legs[0].lineNames)
        assertEquals(listOf("태평", "모란", "왕십리"), route.legs[0].stops)
        assertEquals(GeoPoint(37.4, 127.1), route.legs[0].points.first())
        assertTrue(route.legs[1].guidance.isEmpty())
        assertTrue(route.legs[1].lineNames.isEmpty())
    }

    @Test
    fun `실패 status를 DomainError로 바꾼다`() {
        assertEquals(DomainError.DestinationTooClose, routingErrorForStatus("SAME_POINT"))
        assertEquals(DomainError.DestinationTooClose, routingErrorForStatus("EQUAL_POINTS"))
        assertEquals(DomainError.RouteNotFound("TOO_FAR_AWAY"), routingErrorForStatus("TOO_FAR_AWAY"))
        assertEquals(DomainError.RouteNotFound("NO_RESULTS"), routingErrorForStatus("NO_RESULTS"))
    }
}
