package com.example.simpletracking.presentation.map.kakao

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.UserLocation
import com.example.simpletracking.presentation.map.MapFitLayout
import com.example.simpletracking.presentation.map.PlaceMarker
import com.example.simpletracking.presentation.map.RouteLine
import com.example.simpletracking.presentation.map.RouteOverlay
import com.example.simpletracking.testutil.Fixtures
import com.example.simpletracking.testutil.userLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KakaoMapStateTest {

    private class FakeMapCommands : MapCommands {
        val calls = mutableListOf<String>()
        var lastLocation: UserLocation? = null
        var lastMarkers: List<PlaceMarker> = emptyList()
        var lastRoute: RouteOverlay? = null

        override fun moveCamera(target: GeoPoint, zoomLevel: Int) { calls += "move $zoomLevel" }
        override fun fitCamera(points: List<GeoPoint>, layout: MapFitLayout) { calls += "fit ${points.size} $layout" }
        override fun updateMyLocation(location: UserLocation?) { lastLocation = location }
        override fun showPlaceMarkers(markers: List<PlaceMarker>, selectedId: String?) { lastMarkers = markers }
        override fun showRoute(overlay: RouteOverlay?) { lastRoute = overlay }
    }

    private val state = KakaoMapState()
    private val map = FakeMapCommands()

    @Test
    fun `준비 전에 온 카메라 명령은 붙는 순간 한 번만 실행한다`() {
        state.moveCamera(Fixtures.cityHall, 17)

        state.attach(map)
        state.detach()
        state.attach(map)

        assertEquals(listOf("move 17"), map.calls)
    }

    @Test
    fun `준비된 뒤에는 바로 실행한다`() {
        state.attach(map)

        state.moveCamera(Fixtures.cityHall, 16)

        assertEquals(listOf("move 16"), map.calls)
    }

    @Test
    fun `준비 전에 여러 번 오면 마지막 명령만 실행한다`() {
        state.moveCamera(Fixtures.cityHall, 17)
        state.fitCamera(listOf(Fixtures.cityHall, Fixtures.gangnamStation), MapFitLayout.ROUTE)

        state.attach(map)

        assertEquals(listOf("fit 2 ROUTE"), map.calls)
    }

    @Test
    fun `맞출 지점이 없으면 무시한다`() {
        state.attach(map)

        state.fitCamera(emptyList(), MapFitLayout.SEARCH_RESULTS)

        assertEquals(emptyList<String>(), map.calls)
    }

    @Test
    fun `준비 전에 받은 내 위치·마커·경로를 붙을 때 다시 그린다`() {
        val location = userLocation(37.5, 127.0)
        val markers = listOf(PlaceMarker("1", Fixtures.cityHall, 1))
        val overlay = RouteOverlay(listOf(RouteLine(RouteLine.Kind.CAR, listOf(Fixtures.cityHall, Fixtures.gangnamStation))), Fixtures.cityHall, Fixtures.gangnamStation)
        state.updateMyLocation(location)
        state.showPlaceMarkers(markers, selectedId = null)
        state.showRoute(overlay)

        state.attach(map)

        assertEquals(location, map.lastLocation)
        assertEquals(markers, map.lastMarkers)
        assertEquals(overlay, map.lastRoute)
    }

    @Test
    fun `지운 경로는 다시 그리지 않는다`() {
        state.showRoute(RouteOverlay(emptyList(), Fixtures.cityHall, Fixtures.gangnamStation))
        state.showRoute(null)

        state.attach(map)

        assertNull(map.lastRoute)
    }

    @Test
    fun `마커 클릭을 전달하고, 붙으면 이전 실패를 지운다`() {
        val clicked = mutableListOf<String>()
        state.onPlaceClick = { clicked += it }
        state.fail("인증 실패")

        state.placeClicked("42")
        state.attach(map)

        assertEquals(listOf("42"), clicked)
        assertNull(state.loadFailure)
    }
}
