package com.example.simpletracking.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 서버 JSON을 그대로 옮긴 DTO. freezed + json_serializable의 fromJson 모델에 해당한다.
 * domain 모델(Place)과 분리해 두면 API 스펙이 바뀌어도 mapper만 고치면 된다.
 */
@Serializable
data class KeywordSearchResponse(
    val documents: List<PlaceDocument> = emptyList(),
)

@Serializable
data class PlaceDocument(
    val id: String,
    @SerialName("place_name") val placeName: String,
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("address_name") val addressName: String = "",
    @SerialName("road_address_name") val roadAddressName: String = "",
    val phone: String = "",
    /** 경도(문자열) */
    val x: String,
    /** 위도(문자열) */
    val y: String,
    /** 기준 좌표로부터 거리(m, 문자열). 기준 좌표 없이 검색하면 빈 문자열. */
    val distance: String = "",
)
