package com.example.simpletracking.presentation.map.route

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.TransitLeg
import com.example.simpletracking.domain.model.TravelMode
import com.example.simpletracking.domain.result.AppResult
import com.example.simpletracking.domain.usecase.FindCarRouteUseCase
import com.example.simpletracking.domain.usecase.FindTransitRoutesUseCase
import com.example.simpletracking.domain.usecase.FindWalkingRouteUseCase
import com.example.simpletracking.presentation.map.RouteLine
import com.example.simpletracking.testutil.FakeRouteRepository
import com.example.simpletracking.testutil.Fixtures
import com.example.simpletracking.testutil.MainDispatcherRule
import com.example.simpletracking.testutil.place
import com.example.simpletracking.testutil.transitLeg
import com.example.simpletracking.testutil.transitRoute
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Flutter subway_line_colors_test · iOS SubwayLineColorsTests와 같은 경우. */
class SubwayLineColorsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun leg(kind: TransitLeg.Kind, vararg lines: String) =
        transitLeg(kind, GeoPoint(37.5, 127.0), GeoPoint(37.51, 127.01)).copy(lineNames = lines.toList())

    @Test
    fun `노선 이름으로 공식 색을 찾는다`() {
        listOf(
            "1호선" to 0x0052A4,
            "2호선" to 0x00A84D,
            "수도권 1호선" to 0x0052A4,
            "9호선" to 0xBDB092,
            "수인분당선" to 0xF5A200,
            "신분당선" to 0xD4003B,
            "경의중앙선" to 0x77C4A3,
            "인천 1호선" to 0x7CA8D5, // 1호선 색이 아니어야 한다
        ).forEach { (name, rgb) -> assertEquals(name, rgb, SubwayLineColors.rgb(name)) }
    }

    @Test
    fun `모르는 노선은 null`() {
        assertNull(SubwayLineColors.rgb("부산 1호선 아님 노선"))
        assertNull(SubwayLineColors.rgb("동해선"))
    }

    @Test
    fun `지하철 구간에만 노선 색을 쓴다`() {
        assertEquals(0xBDB092, SubwayLineColors.rgb(leg(TransitLeg.Kind.SUBWAY, "모르는선", "9호선")))
        assertNull(SubwayLineColors.rgb(leg(TransitLeg.Kind.SUBWAY, "모르는선")))
        assertNull(SubwayLineColors.rgb(leg(TransitLeg.Kind.BUS, "2")))
    }

    @Test
    fun `지도 경로선은 지하철 구간에만 노선 색을 싣는다`() = runTest {
        val repository = FakeRouteRepository()
        repository.transitResult = AppResult.Success(
            listOf(transitRoute(30, legs = listOf(leg(TransitLeg.Kind.BUS, "146"), leg(TransitLeg.Kind.SUBWAY, "9호선"))))
        )
        val viewModel = RouteViewModel(FindCarRouteUseCase(repository), FindWalkingRouteUseCase(repository), FindTransitRoutesUseCase(repository))
        viewModel.start(place("d", 37.6, 127.1), Fixtures.cityHall)
        viewModel.showRoute(0)

        val lines = viewModel.uiState.value.overlay!!.lines
        assertEquals(TravelMode.TRANSIT, viewModel.uiState.value.selectedMode)
        assertEquals(0xBDB092, lines.first { it.kind == RouteLine.Kind.SUBWAY }.rgb)
        assertNull(lines.first { it.kind == RouteLine.Kind.BUS }.rgb)
    }
}
