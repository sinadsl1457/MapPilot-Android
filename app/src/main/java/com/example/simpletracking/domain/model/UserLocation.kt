package com.example.simpletracking.domain.model

/** android.location.Location을 domain으로 옮긴 타입. 필요한 값만 남긴다. */
data class UserLocation(
    val point: GeoPoint,
    /** 이동 방향(도, 북쪽 기준 시계방향). 정지 중이거나 알 수 없으면 null. */
    val bearing: Float?,
    val accuracyMeters: Float,
    /** 단조 증가 시각(ms). 기기 시계 변경에 영향받지 않아 속도·추세 계산에 쓴다. */
    val elapsedRealtimeMillis: Long,
)
