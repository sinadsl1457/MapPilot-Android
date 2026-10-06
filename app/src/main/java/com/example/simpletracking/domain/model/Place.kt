package com.example.simpletracking.domain.model

data class Place(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
    val phone: String,
    val location: GeoPoint,
    /** 검색 기준 좌표로부터의 거리(m). 기준 좌표 없이 검색했다면 null. */
    val distanceMeters: Int?,
)
