package com.example.simpletracking.presentation.map.kakao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.UserLocation
import com.example.simpletracking.presentation.map.MapFitLayout
import com.example.simpletracking.presentation.map.PlaceMarker
import com.example.simpletracking.presentation.map.RouteOverlay

/** 지도에 내리는 명령. KakaoMapController가 구현하고, 테스트에서는 fake로 바꾼다. */
interface MapCommands {
    fun moveCamera(target: GeoPoint, zoomLevel: Int)
    fun fitCamera(points: List<GeoPoint>, layout: MapFitLayout)
    fun updateMyLocation(location: UserLocation?)
    fun showPlaceMarkers(markers: List<PlaceMarker>, selectedId: String?)
    /** 경로 선과 출발·도착 마커. null이면 지운다. */
    fun showRoute(overlay: RouteOverlay?)
}

/**
 * 선언형(Compose) ↔ 명령형(카카오맵) 사이의 다리. maps-compose의 CameraPositionState와 같은 역할.
 *
 * 카카오맵은 네이티브 엔진 초기화에 1~2초가 걸린다. 그 전에 온 명령은 버리지 않고 보관했다가
 * 지도가 준비되는 순간 실행한다(앱 시작 직후 "첫 위치로 이동"이 바로 이 경우).
 * 마커·경로는 마지막 값을 기억해 두므로, 지도가 다시 만들어져도(화면 회전 등) 그대로 다시 그린다.
 */
@Stable
class KakaoMapState {

    /** 인증·지도 로드 실패 사유. null이면 정상. */
    var loadFailure: String? by mutableStateOf(null)
        private set

    /** 지도 화면 중심 좌표. 카메라가 멈출 때마다 갱신된다(다시 그릴 필요가 없어 State가 아니다). */
    var cameraCenter: GeoPoint? = null
        private set

    /** 사용자가 검색 결과 마커를 눌렀을 때(장소 id) */
    var onPlaceClick: ((String) -> Unit)? = null

    private var map: MapCommands? = null
    private var pendingCamera: PendingCamera? = null
    private var myLocation: UserLocation? = null
    private var placeMarkers: List<PlaceMarker> = emptyList()
    private var selectedPlaceId: String? = null
    private var routeOverlay: RouteOverlay? = null

    private sealed interface PendingCamera {
        data class Move(val target: GeoPoint, val zoomLevel: Int) : PendingCamera
        data class Fit(val points: List<GeoPoint>, val layout: MapFitLayout) : PendingCamera
    }

    fun moveCamera(target: GeoPoint, zoomLevel: Int) {
        // 준비 전에 여러 번 들어오면 마지막 명령만 의미가 있다.
        map?.moveCamera(target, zoomLevel) ?: run { pendingCamera = PendingCamera.Move(target, zoomLevel) }
    }

    fun fitCamera(points: List<GeoPoint>, layout: MapFitLayout) {
        if (points.isEmpty()) return
        map?.fitCamera(points, layout) ?: run { pendingCamera = PendingCamera.Fit(points, layout) }
    }

    fun updateMyLocation(location: UserLocation?) {
        myLocation = location
        map?.updateMyLocation(location)
    }

    fun showPlaceMarkers(markers: List<PlaceMarker>, selectedId: String?) {
        placeMarkers = markers
        selectedPlaceId = selectedId
        map?.showPlaceMarkers(markers, selectedId)
    }

    fun showRoute(overlay: RouteOverlay?) {
        routeOverlay = overlay
        map?.showRoute(overlay)
    }

    // region KakaoMapView / KakaoMapController 전용

    internal fun attach(map: MapCommands) {
        this.map = map
        loadFailure = null
        map.updateMyLocation(myLocation)
        map.showPlaceMarkers(placeMarkers, selectedPlaceId)
        map.showRoute(routeOverlay)
        when (val pending = pendingCamera) {
            is PendingCamera.Move -> map.moveCamera(pending.target, pending.zoomLevel)
            is PendingCamera.Fit -> map.fitCamera(pending.points, pending.layout)
            null -> Unit
        }
        pendingCamera = null
    }

    internal fun detach() {
        map = null
    }

    internal fun fail(reason: String) {
        loadFailure = reason
    }

    internal fun cameraDidStop(center: GeoPoint) {
        cameraCenter = center
    }

    internal fun placeClicked(id: String) {
        onPlaceClick?.invoke(id)
    }

    // endregion
}

@Composable
fun rememberKakaoMapState(): KakaoMapState = remember { KakaoMapState() }
