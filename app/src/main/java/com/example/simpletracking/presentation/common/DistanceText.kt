package com.example.simpletracking.presentation.common

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.util.GeoMath
import java.util.Locale
import kotlin.math.roundToInt

/** 1km 미만은 m 단위 정수, 이상은 km 소수 첫째 자리. 예: "320m", "1.2km" */
fun formatDistance(meters: Double): String {
    if (meters < 1_000) return "${meters.roundToInt()}m"
    // 22850m를 바로 /1000 하면 22.8499…가 되어 22.8로 내려가므로 100m 단위로 먼저 반올림한다.
    val kilometers = (meters / 100).roundToInt() / 10.0
    return String.format(Locale.US, "%.1fkm", kilometers)
}

/** 내 위치를 알 때만 거리를 보여준다. */
fun formatDistance(from: GeoPoint?, to: GeoPoint): String? =
    from?.let { formatDistance(GeoMath.distanceMeters(it, to)) }
