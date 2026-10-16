package com.example.simpletracking.data.mapper

import com.example.simpletracking.domain.model.PlaceCategory

/** 카카오 로컬 API의 category_group_code */
val PlaceCategory.kakaoCode: String
    get() = when (this) {
        PlaceCategory.RESTAURANT -> "FD6"
        PlaceCategory.CAFE -> "CE7"
        PlaceCategory.CONVENIENCE_STORE -> "CS2"
        PlaceCategory.GAS_STATION -> "OL7"
        PlaceCategory.PHARMACY -> "PM9"
        PlaceCategory.PARKING -> "PK6"
    }
