package com.example.simpletracking.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.UserLocation
import com.example.simpletracking.domain.result.DomainException
import com.example.simpletracking.domain.usecase.HasLocationPermissionUseCase
import com.example.simpletracking.domain.usecase.ObserveLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @HiltViewModel + @Inject constructor: 화면에서 hiltViewModel()로 꺼내면 Hilt가 UseCase를 채워서 만든다.
 * ViewModel은 UseCase에만 의존한다(Repository를 직접 알지 않는다).
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val observeLocation: ObserveLocationUseCase,
    private val hasLocationPermission: HasLocationPermissionUseCase,
) : ViewModel() {

    private val locationPermissionGranted = MutableStateFlow(false)
    private val settingsDialogVisible = MutableStateFlow(false)
    private val followMode = MutableStateFlow(FollowMode.NONE)
    /** 카메라가 내 위치에 맞춰져 있다(첫 위치 이동 · 내 위치 버튼 이후 지도를 옮기지 않음). */
    private val isCameraOnMyLocation = MutableStateFlow(false)
    /** 앱을 켠 뒤 권한 팝업을 자동으로 한 번만 띄운다. ViewModel이라 화면 회전에도 유지된다. */
    private var permissionRequestedOnStart = false

    // Channel: 값 하나를 정확히 한 번, 한 수신자에게 전달한다. BUFFERED라 화면이 잠깐 구독을 끊어도 쌓아 둔다.
    private val _effects = Channel<MapEffect>(Channel.BUFFERED)
    val effects: Flow<MapEffect> = _effects.receiveAsFlow()

    /**
     * 권한이 바뀔 때마다 위치 스트림을 갈아끼운다(flatMapLatest = RxDart의 switchMap).
     * stateIn: 구독자가 여럿이어도 GPS 등록은 하나로 공유한다(BehaviorSubject).
     *
     * 여기서는 대기 시간 없이(WhileSubscribed()) 구독자가 0이 되는 즉시 끊는다.
     * 5초 유예는 화면과 맞닿은 uiState 한 곳에만 둔다. 두 겹으로 5초를 두면 GPS가 10초간 켜져 있게 된다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val myLocation: StateFlow<UserLocation?> = locationPermissionGranted
        .flatMapLatest { granted ->
            if (!granted) {
                flowOf(null)
            } else {
                observeLocation().catch { e ->
                    if (e !is DomainException) throw e
                    locationPermissionGranted.value = false
                    stopFollowing()
                    _effects.send(MapEffect.ShowError(e.error))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /**
     * 화면에 내보내는 단일 상태. 여러 소스를 combine으로 합친다(RxDart의 combineLatest).
     * WhileSubscribed(5초): 아무도 보지 않은 지 5초가 지나면(앱이 백그라운드로 감) 위쪽 스트림(=GPS)을 끊는다.
     * 화면 회전(1초 미만)에는 끊기지 않는다.
     */
    val uiState: StateFlow<MapUiState> =
        combine(locationPermissionGranted, myLocation, settingsDialogVisible, followMode, isCameraOnMyLocation) {
                granted, location, dialog, mode, cameraOnMe ->
            MapUiState(
                hasLocationPermission = granted,
                myLocation = location,
                isSettingsDialogVisible = dialog,
                followMode = mode,
                isCameraOnMyLocation = cameraOnMe,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MapUiState())

    init {
        // 첫 위치를 받으면 한 번만 카메라를 내 위치로 옮긴다.
        viewModelScope.launch {
            val first = myLocation.filterNotNull().first()
            isCameraOnMyLocation.value = true
            _effects.send(MapEffect.MoveCamera(first.point, ZOOM_MY_LOCATION))
        }
    }

    /**
     * 화면에 돌아올 때마다 부른다(설정 앱에서 권한을 켜고 돌아온 경우 포함).
     * 권한이 없으면 앱을 켠 뒤 처음 한 번만 권한을 요청한다.
     */
    fun onResume() {
        val granted = hasLocationPermission()
        locationPermissionGranted.value = granted
        if (!granted) stopFollowing()
        if (!granted && !permissionRequestedOnStart) {
            permissionRequestedOnStart = true
            _effects.trySend(MapEffect.RequestLocationPermission)
        }
    }

    /**
     * 권한 요청 결과는 Activity만 받을 수 있으므로 화면이 전달해 준다.
     * [canAskAgain]이 false면("다시 묻지 않음") 팝업이 더는 뜨지 않으므로 설정 앱으로 안내한다.
     */
    fun onLocationPermissionResult(granted: Boolean, canAskAgain: Boolean = true) {
        locationPermissionGranted.value = granted
        if (!granted) stopFollowing()
        if (!granted && !canAskAgain) settingsDialogVisible.value = true
    }

    fun onSettingsDialogDismiss() {
        settingsDialogVisible.value = false
    }

    /**
     * 권한 없음 → 권한 요청 / 위치 모름 → 안내 /
     * 지도를 옮겨 둠 → 내 위치로 카메라만 옮긴다(따라가기는 아직 끔) /
     * 카메라가 내 위치 → 따라가기 모드를 다음 단계로(NONE → FOLLOW → FOLLOW_WITH_HEADING → NONE).
     * NONE에서 누를 때는 항상 내 위치로 카메라를 옮긴다.
     */
    fun onMyLocationClick() {
        val state = uiState.value
        when {
            !state.hasLocationPermission -> _effects.trySend(MapEffect.RequestLocationPermission)
            state.myLocation == null -> _effects.trySend(MapEffect.ShowMessage(R.string.message_locating))
            else -> {
                if (followMode.value == FollowMode.NONE) {
                    _effects.trySend(MapEffect.MoveCamera(state.myLocation.point, ZOOM_MY_LOCATION))
                    if (!isCameraOnMyLocation.value) {
                        isCameraOnMyLocation.value = true
                        return
                    }
                }
                followMode.value = followMode.value.next
            }
        }
    }

    /** 사용자가 지도를 드래그했다. 내 위치로 카메라를 끌고 가지 않게 따라가기를 끈다. 다음 내 위치 버튼은 카메라만 옮긴다. */
    fun onMapPanned() = stopFollowing()

    /** 사용자가 지도를 직접 돌렸다. 회전 모드만 풀고 따라가기는 유지한다. */
    fun onMapRotated() {
        if (followMode.value == FollowMode.FOLLOW_WITH_HEADING) followMode.value = FollowMode.FOLLOW
    }

    /** 다른 기능이 카메라를 옮길 때(검색 결과, 길찾기) 화면이 부른다. 끄지 않으면 트래킹이 카메라를 바로 내 위치로 되돌린다. */
    fun stopFollowing() {
        followMode.value = FollowMode.NONE
        isCameraOnMyLocation.value = false
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        /** 카카오맵 줌 레벨(숫자가 클수록 확대). 17 ≈ 건물 하나하나가 보이는 수준 */
        const val ZOOM_MY_LOCATION = 17
    }
}
