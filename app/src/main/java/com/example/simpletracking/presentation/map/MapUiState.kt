package com.example.simpletracking.presentation.map

import com.example.simpletracking.domain.model.UserLocation

/**
 * 지도 화면의 내 위치 관련 상태. Cubit의 State(Freezed)에 해당한다.
 * 불변(val + data class)이라 copy()로만 바꿀 수 있고, Compose는 equals로 변경 여부를 판단한다.
 * 검색·길찾기 상태는 각자의 ViewModel이 따로 들고 있다.
 */
data class MapUiState(
    val hasLocationPermission: Boolean = false,
    val myLocation: UserLocation? = null,
    /** 권한이 "다시 묻지 않음"으로 거부돼 있으면 설정 앱으로 안내한다. */
    val isSettingsDialogVisible: Boolean = false,
)
