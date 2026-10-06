package com.example.simpletracking.domain.model

/**
 * 위경도 좌표. 카카오 SDK의 LatLng 대신 이 타입을 쓰는 이유:
 * domain이 지도 SDK에 의존하면 SDK를 교체할 때 domain까지 바꿔야 하고, JVM 단위 테스트도 어려워진다.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)
