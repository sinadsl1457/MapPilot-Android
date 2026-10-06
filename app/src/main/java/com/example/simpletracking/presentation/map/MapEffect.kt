package com.example.simpletracking.presentation.map

import androidx.annotation.StringRes
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.result.DomainError

/**
 * 한 번만 실행되어야 하는 명령. BlocListener로 처리하던 부수효과에 해당한다.
 * 상태에 넣으면 화면 회전 후 재구독할 때 다시 실행되므로(스낵바 두 번 등) 분리한다.
 */
sealed interface MapEffect {
    data class MoveCamera(val target: GeoPoint, val zoomLevel: Int) : MapEffect
    data class ShowError(val error: DomainError) : MapEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : MapEffect
    data object RequestLocationPermission : MapEffect
}
