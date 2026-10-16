package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.repository.LocationRepository
import javax.inject.Inject

/**
 * 권한 "요청"은 Activity만 할 수 있어 화면이 맡고, 여기서는 현재 상태만 확인한다.
 * 고정 위치를 쓰면 저장소가 항상 true를 돌려주므로 권한 팝업도 뜨지 않는다.
 */
class HasLocationPermissionUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(): Boolean = locationRepository.hasPermission()
}
