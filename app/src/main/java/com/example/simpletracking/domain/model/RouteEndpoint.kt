package com.example.simpletracking.domain.model

/** 길찾기 출발지·도착지. 내 위치이거나 검색한 장소다. */
sealed interface RouteEndpoint {
    /** 좌표는 길찾기를 시작하는 순간의 내 위치로 정한다(RouteViewModel). */
    data object MyLocation : RouteEndpoint
    data class PlaceEndpoint(val place: Place) : RouteEndpoint

    /** 이 지점의 좌표. 내 위치는 [myLocation]을 쓰고, 아직 모르면 null. */
    fun point(myLocation: GeoPoint?): GeoPoint? = when (this) {
        MyLocation -> myLocation
        is PlaceEndpoint -> place.location
    }
}
