package com.example.simpletracking.presentation.map.kakao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.simpletracking.domain.model.GeoPoint

/**
 * 선언형(Compose) ↔ 명령형(카카오맵) 사이의 다리. maps-compose의 CameraPositionState와 같은 역할.
 *
 * 카카오맵은 네이티브 엔진 초기화에 1~2초가 걸린다. 그 전에 온 카메라 명령은 버리지 않고 보관했다가
 * 지도가 준비되는 순간 실행한다(앱 시작 직후 "첫 위치로 이동"이 바로 이 경우).
 */
@Stable
class KakaoMapState internal constructor() {

    /** mutableStateOf: 값이 바뀌면 이 값을 읽는 Composable이 다시 그려진다(Flutter의 setState 대상 필드). */
    internal var controller: KakaoMapController? by mutableStateOf(null)
        private set

    private var pendingCamera: Pair<GeoPoint, Int>? = null

    fun moveCamera(target: GeoPoint, zoomLevel: Int) {
        controller?.moveCamera(target, zoomLevel) ?: run { pendingCamera = target to zoomLevel }
    }

    internal fun attach(controller: KakaoMapController) {
        this.controller = controller
        pendingCamera?.let { (target, zoom) -> controller.moveCamera(target, zoom) }
        pendingCamera = null
    }

    internal fun detach() {
        controller = null
    }
}

@Composable
fun rememberKakaoMapState(): KakaoMapState = remember { KakaoMapState() }
