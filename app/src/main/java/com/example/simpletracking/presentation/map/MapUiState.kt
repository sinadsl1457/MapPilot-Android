package com.example.simpletracking.presentation.map

import com.example.simpletracking.domain.model.UserLocation

/**
 * 화면이 그리는 데 필요한 모든 것. Cubit의 State(Freezed)에 해당한다.
 * 불변(val + data class)이라 copy()로만 바꿀 수 있고, Compose는 equals로 변경 여부를 판단한다.
 *
 * 이후 단계에서 검색 결과, 선택 장소, 추적 모드, 길안내 상태가 추가된다.
 */
data class MapUiState(
    val hasLocationPermission: Boolean = false,
    val myLocation: UserLocation? = null,
)
