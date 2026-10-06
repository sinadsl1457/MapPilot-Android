package com.example.simpletracking.data.mapper

import com.example.simpletracking.data.remote.dto.PlaceDocument
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place

/** 좌표가 깨진 항목은 지도에 찍을 수 없으므로 null로 걸러낸다(mapNotNull과 함께 사용). */
fun PlaceDocument.toDomainOrNull(): Place? {
    val latitude = y.toDoubleOrNull() ?: return null
    val longitude = x.toDoubleOrNull() ?: return null
    return Place(
        id = id,
        name = placeName,
        // "음식점 > 카페 > 커피전문점" → "커피전문점"
        category = categoryName.substringAfterLast(" > "),
        address = roadAddressName.ifBlank { addressName },
        phone = phone,
        location = GeoPoint(latitude, longitude),
        distanceMeters = distance.toIntOrNull(),
    )
}
