package com.example.simpletracking.presentation.map

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.simpletracking.R
import com.example.simpletracking.presentation.common.LOCATION_PERMISSIONS
import com.example.simpletracking.presentation.common.hasLocationPermission
import com.example.simpletracking.presentation.common.messageRes
import com.example.simpletracking.presentation.map.kakao.KakaoMapState
import com.example.simpletracking.presentation.map.kakao.KakaoMapView
import com.example.simpletracking.presentation.map.kakao.rememberKakaoMapState
import kotlinx.coroutines.launch

/**
 * Route = BlocProvider + BlocListener 역할.
 * ViewModel을 꺼내고, 권한·이펙트처럼 Android 플랫폼(Activity)이 필요한 일을 처리한다.
 */
@Composable
fun MapRoute(viewModel: MapViewModel = hiltViewModel()) {
    // collectAsStateWithLifecycle: 앱이 백그라운드(STOPPED)면 구독을 멈춘다 → ViewModel의 WhileSubscribed 5초 타이머가 돈다.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val mapState = rememberKakaoMapState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        viewModel.onLocationPermissionResult(granted)
        if (!granted) {
            // "다시 묻지 않음" 상태면 다이얼로그 없이 바로 거부가 온다 → 설정 화면으로 안내
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.error_location_permission_denied),
                    actionLabel = context.getString(R.string.action_open_settings),
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                }
            }
        }
    }

    // rememberSaveable: 화면 회전 후에도 유지된다 → 회전할 때마다 권한 다이얼로그가 다시 뜨지 않게 한다.
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    // 화면에 돌아올 때마다(설정 앱에서 권한을 켜고 돌아온 경우 포함) 권한 상태를 다시 확인한다.
    LifecycleResumeEffect(Unit) {
        val granted = context.hasLocationPermission()
        viewModel.onLocationPermissionResult(granted)
        if (!granted && !permissionAsked) {
            permissionAsked = true
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
        onPauseOrDispose { }
    }

    // 일회성 이펙트 처리(BlocListener). STARTED 이상일 때만 수집한다.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is MapEffect.MoveCamera -> mapState.moveCamera(effect.target, effect.zoomLevel)
                    // showSnackbar는 스낵바가 사라질 때까지 suspend된다.
                    // 여기서 바로 기다리면 그동안 다음 이펙트(카메라 이동 등)가 막히므로 별도 코루틴에서 띄운다.
                    is MapEffect.ShowError -> launch { snackbarHostState.showSnackbar(context.getString(effect.error.messageRes())) }
                    is MapEffect.ShowMessage -> launch { snackbarHostState.showSnackbar(context.getString(effect.messageRes)) }
                    MapEffect.RequestLocationPermission -> permissionLauncher.launch(LOCATION_PERMISSIONS)
                }
            }
        }
    }

    MapScreen(
        uiState = uiState,
        mapState = mapState,
        snackbarHostState = snackbarHostState,
        onMyLocationClick = viewModel::onMyLocationClick,
        onMapError = { error ->
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_map_load, error.message ?: "")) }
        },
    )
}

/**
 * Screen = BlocBuilder의 builder. 상태를 받아 그리기만 하고, 사용자 입력은 콜백으로 올려보낸다.
 * ViewModel을 모르므로 가짜 상태만으로 UI 테스트를 할 수 있다.
 */
@Composable
fun MapScreen(
    uiState: MapUiState,
    mapState: KakaoMapState,
    snackbarHostState: SnackbarHostState,
    onMyLocationClick: () -> Unit,
    onMapError: (Exception) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onMyLocationClick) {
                Icon(
                    imageVector = if (uiState.hasLocationPermission) Icons.Default.MyLocation else Icons.Default.LocationDisabled,
                    contentDescription = "내 위치",
                )
            }
        },
    ) { _ ->
        // 지도는 상태바·내비게이션바 아래까지 꽉 채운다(innerPadding 무시). FAB와 스낵바는 Scaffold가 inset을 처리한다.
        KakaoMapView(
            state = mapState,
            myLocation = uiState.myLocation,
            onMapError = onMapError,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
