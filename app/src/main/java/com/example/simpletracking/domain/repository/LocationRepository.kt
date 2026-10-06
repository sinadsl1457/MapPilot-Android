package com.example.simpletracking.domain.repository

import com.example.simpletracking.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun hasPermission(): Boolean

    suspend fun getLastLocation(): UserLocation?

    /**
     * 위치 스트림. RxDart의 Observable/Dart Stream과 달리 Flow는 "cold"다:
     * 누군가 collect하는 동안에만 GPS를 켜고, 수집이 취소되면 자동으로 끈다.
     */
    fun observeLocation(): Flow<UserLocation>
}
