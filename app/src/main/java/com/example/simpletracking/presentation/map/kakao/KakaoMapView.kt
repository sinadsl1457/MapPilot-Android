package com.example.simpletracking.presentation.map.kakao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView

/** 서울시청. 위치를 아직 모를 때의 시작점 */
val DEFAULT_MAP_CENTER = GeoPoint(37.5666, 126.9784)
private const val DEFAULT_ZOOM = 15

/**
 * 기존 View(MapView)를 Compose 안에 넣는다. Flutter PlatformView의 반대 방향.
 * 무엇을 그릴지는 전부 [state]로 보낸다. 이 함수는 MapView의 생성·생명주기만 맡는다.
 */
@Composable
fun KakaoMapView(
    state: KakaoMapState,
    modifier: Modifier = Modifier,
) {
    if (!KakaoMapSdk.isInitialized()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.map_missing_key), textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // remember: 리컴포지션마다 MapView를 새로 만들지 않도록 한 번만 생성해 보관한다.
    val mapView = remember { MapView(context) }
    // 지도가 준비되면 생기는 컨트롤러. 생명주기(나침반 켜고 끄기)와 정리에 쓴다(다시 그릴 필요가 없어 State가 아니다).
    val controller = remember { arrayOfNulls<KakaoMapController>(1) }

    // Activity 생명주기를 MapView에 그대로 전달한다(PlatformView에서 ActivityAware로 하던 일).
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.resume()
                    controller[0]?.setActive(true)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.pause()
                    controller[0]?.setActive(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.detach()
            controller[0]?.tearDown()
            controller[0] = null
            mapView.finish()
        }
    }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() = Unit
                            override fun onMapError(error: Exception) = state.fail(error.message.orEmpty())
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(kakaoMap: KakaoMap) {
                                val created = KakaoMapController(context, kakaoMap, state)
                                controller[0] = created
                                state.attach(created)
                            }
                            override fun getPosition(): LatLng = DEFAULT_MAP_CENTER.toLatLng()
                            override fun getZoomLevel(): Int = DEFAULT_ZOOM
                        },
                    )
                }
            },
        )

        state.loadFailure?.let { reason ->
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.error_map_load, reason),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}
