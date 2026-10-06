package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.UserLocation
import com.example.simpletracking.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObserveLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
) {
    /**
     * 1) 마지막으로 알려진 위치를 먼저 내보내 지도가 즉시 내 주변을 보여주게 하고
     * 2) 이어서 실시간 GPS 스트림을 이어 붙인다.
     * GPS 첫 fix는 수 초~수십 초 걸리기 때문에 1)이 체감 속도를 크게 바꾼다.
     */
    operator fun invoke(): Flow<UserLocation> =
        flow {
            locationRepository.getLastLocation()?.let { emit(it) }
            emitAll(locationRepository.observeLocation())
        }
            // 정확도가 너무 낮은 위치(실내 GPS 튐 등)는 거리 계산을 흔들기 때문에 걸러낸다.
            .filter { it.accuracyMeters <= MAX_ACCURACY_METERS }

    companion object {
        const val MAX_ACCURACY_METERS = 50f
    }
}
