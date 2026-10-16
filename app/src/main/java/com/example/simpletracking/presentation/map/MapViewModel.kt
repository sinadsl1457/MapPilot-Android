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
        combine(locationPermissionGranted, myLocation, settingsDialogVisible) { granted, location, dialog ->
            MapUiState(hasLocationPermission = granted, myLocation = location, isSettingsDialogVisible = dialog)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MapUiState())

    init {
        // 첫 위치를 받으면 한 번만 카메라를 내 위치로 옮긴다.
        viewModelScope.launch {
            val first = myLocation.filterNotNull().first()
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
        if (!granted && !canAskAgain) settingsDialogVisible.value = true
    }

    fun onSettingsDialogDismiss() {
        settingsDialogVisible.value = false
    }

    fun onMyLocationClick() {
        viewModelScope.launch {
            val state = uiState.value
            when {
                !state.hasLocationPermission -> _effects.send(MapEffect.RequestLocationPermission)
                state.myLocation == null -> _effects.send(MapEffect.ShowMessage(R.string.message_locating))
                else -> _effects.send(MapEffect.MoveCamera(state.myLocation.point, ZOOM_MY_LOCATION))
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        /** 카카오맵 줌 레벨(숫자가 클수록 확대). 17 ≈ 건물 하나하나가 보이는 수준 */
        const val ZOOM_MY_LOCATION = 17
    }
}
