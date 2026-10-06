package com.example.simpletracking.presentation.map.kakao

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.simpletracking.domain.model.UserLocation
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView

/** 서울시청. 위치를 아직 모를 때의 시작점 */
private val DEFAULT_POSITION: LatLng = LatLng.from(37.5666, 126.9784)
private const val DEFAULT_ZOOM = 15

/**
 * 기존 View(MapView)를 Compose 안에 넣는다. Flutter PlatformView의 반대 방향.
 * 이 함수는 "상태 → 지도" 반영만 담당하고, 무엇을 보여줄지는 전부 파라미터로 받는다.
 */
@Composable
fun KakaoMapView(
    state: KakaoMapState,
    myLocation: UserLocation?,
    onMapError: (Exception) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!KakaoMapSdk.isInitialized()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("local.properties에 KAKAO_NATIVE_APP_KEY를 설정해주세요")
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // remember: 리컴포지션마다 MapView를 새로 만들지 않도록 한 번만 생성해 보관한다.
    val mapView = remember { MapView(context) }
    // 콜백은 한 번 등록되지만 onMapError는 리컴포지션마다 바뀔 수 있다 → 항상 최신 람다를 가리키게 한다.
    val currentOnMapError by rememberUpdatedState(onMapError)

    // Activity 생명주기를 MapView에 그대로 전달한다(PlatformView에서 ActivityAware로 하던 일).
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.detach()
            mapView.finish()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit
                        override fun onMapError(error: Exception) = currentOnMapError(error)
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(kakaoMap: KakaoMap) {
                            state.attach(KakaoMapController(context, kakaoMap))
                        }
                        override fun getPosition(): LatLng = myLocation?.point?.toLatLng() ?: DEFAULT_POSITION
                        override fun getZoomLevel(): Int = DEFAULT_ZOOM
                    },
                )
            }
        },
    )

    // 선언형 → 명령형: controller가 준비됐거나 myLocation이 바뀔 때마다 지도에 반영한다.
    val controller = state.controller
    LaunchedEffect(controller, myLocation) {
        controller?.updateMyLocation(myLocation)
    }
}
