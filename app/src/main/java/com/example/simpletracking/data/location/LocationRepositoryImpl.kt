package com.example.simpletracking.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.simpletracking.domain.model.UserLocation
import com.example.simpletracking.domain.repository.LocationRepository
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.domain.result.DomainException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    // Activity Context를 싱글턴이 붙잡으면 화면이 닫혀도 GC되지 않는다(메모리 누수). 그래서 Application Context만 받는다.
    @param:ApplicationContext private val context: Context,
    private val fusedClient: FusedLocationProviderClient,
) : LocationRepository {

    override fun hasPermission(): Boolean = LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // hasPermission()으로 먼저 확인한다
    override suspend fun getLastLocation(): UserLocation? {
        if (!hasPermission()) return null
        return try {
            // Task<Location> → suspend 변환 (kotlinx-coroutines-play-services). Dart의 Future를 await하는 것과 같다.
            fusedClient.lastLocation.await()?.toDomain()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 마지막 위치는 "있으면 좋은" 값이라 실패해도 null로 넘긴다.
            null
        }
    }

    @SuppressLint("MissingPermission")
    override fun observeLocation(): Flow<UserLocation> = callbackFlow {
        if (!hasPermission()) {
            close(DomainException(DomainError.LocationPermissionDenied))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MILLIS)
            .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MILLIS / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // trySend: 수신 측이 바쁘면 기다리지 않는다. 콜백은 suspend가 아니라 send()를 쓸 수 없다.
                result.lastLocation?.let { trySend(it.toDomain()) }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // 확인 직후 사용자가 설정에서 권한을 끄는 경쟁 상황
            close(DomainException(DomainError.LocationPermissionDenied))
        }

        // collect가 취소될 때까지 여기서 대기하다가, 취소되면 GPS 업데이트를 해제한다.
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    private companion object {
        const val UPDATE_INTERVAL_MILLIS = 1_000L
        val LOCATION_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
