package com.example.simpletracking.data.location

import android.location.Location
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.UserLocation

/** 이보다 느리면(사실상 정지) GPS bearing이 무작위로 튀므로 방향 정보를 버린다. */
private const val MIN_SPEED_FOR_BEARING_MPS = 0.5f

fun Location.toDomain(): UserLocation = UserLocation(
    point = GeoPoint(latitude, longitude),
    bearing = bearing.takeIf { hasBearing() && hasSpeed() && speed >= MIN_SPEED_FOR_BEARING_MPS },
    accuracyMeters = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
    elapsedRealtimeMillis = elapsedRealtimeNanos / 1_000_000,
)
