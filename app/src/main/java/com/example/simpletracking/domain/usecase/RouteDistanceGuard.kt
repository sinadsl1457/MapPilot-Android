package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.util.GeoMath

/** 이보다 가까우면 어떤 이동 수단으로도 길을 찾지 않는다(호출 한도 절약 + 카카오 API도 5m 이내는 에러를 준다). */
const val MIN_ROUTE_DISTANCE_METERS = 20.0

fun isTooCloseForRoute(origin: GeoPoint, destination: GeoPoint): Boolean =
    GeoMath.distanceMeters(origin, destination) < MIN_ROUTE_DISTANCE_METERS
