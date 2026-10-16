package com.example.simpletracking.testutil

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.UserLocation
import com.example.simpletracking.domain.repository.LocationRepository
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.domain.result.DomainException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

class FakeLocationRepository : LocationRepository {
    /** 테스트가 위치를 직접 흘려보내는 통로. subscriptionCount로 "GPS가 켜져 있는지" 확인할 수 있다. */
    val locations = MutableSharedFlow<UserLocation>()
    var lastLocation: UserLocation? = null
    var failWith: DomainError? = null
    var permissionGranted = true

    override fun hasPermission(): Boolean = permissionGranted
    override suspend fun getLastLocation(): UserLocation? = lastLocation
    override fun observeLocation(): Flow<UserLocation> =
        failWith?.let { error -> flow { throw DomainException(error) } } ?: locations
}

fun userLocation(latitude: Double, longitude: Double, accuracy: Float = 5f, time: Long = 0L) = UserLocation(
    point = GeoPoint(latitude, longitude),
    bearing = null,
    accuracyMeters = accuracy,
    elapsedRealtimeMillis = time,
)
