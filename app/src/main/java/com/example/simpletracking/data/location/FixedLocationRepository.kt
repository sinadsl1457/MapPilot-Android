package com.example.simpletracking.data.location

import android.os.SystemClock
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.UserLocation
import com.example.simpletracking.domain.repository.LocationRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * GPS 대신 정해진 좌표를 내 위치로 돌려준다. 권한은 항상 허용된 것으로 본다.
 * 에뮬레이터처럼 실제 위치를 받기 어려운 환경에서 연습용으로 쓴다(local.properties의 FIXED_LOCATION).
 */
class FixedLocationRepository(
    private val point: GeoPoint,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : LocationRepository {

    override fun hasPermission(): Boolean = true

    override suspend fun getLastLocation(): UserLocation = location()

    /** 고정 위치를 한 번 내보내고, 구독이 끝날 때까지 열어 둔다. */
    override fun observeLocation(): Flow<UserLocation> = flow {
        emit(location())
        awaitCancellation()
    }

    private fun location() = UserLocation(
        point = point,
        bearing = null,
        accuracyMeters = 5f,
        elapsedRealtimeMillis = clock(),
    )

    companion object {
        /** "위도,경도" → GeoPoint. 형식이 틀리거나 범위를 벗어나면 null. */
        fun parse(text: String): GeoPoint? {
            val parts = text.split(",").map { it.trim() }
            if (parts.size != 2) return null
            val latitude = parts[0].toDoubleOrNull() ?: return null
            val longitude = parts[1].toDoubleOrNull() ?: return null
            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
            return GeoPoint(latitude, longitude)
        }
    }
}
