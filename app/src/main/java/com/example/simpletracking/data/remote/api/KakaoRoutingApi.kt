package com.example.simpletracking.data.remote.api

import com.example.simpletracking.data.remote.dto.TransitResponse
import com.example.simpletracking.data.remote.dto.WalkResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 카카오맵 도보·대중교통 경로 API (https://dapi.kakao.com).
 * 자동차 길찾기(카카오모빌리티)와 달리 좌표를 "경도,위도" 문자열이 아니라 x(경도)·y(위도) 파라미터로 따로 받는다.
 */
interface KakaoRoutingApi {
    @GET("v2/routing/walk")
    suspend fun walk(
        @Query("start_x") startLongitude: Double,
        @Query("start_y") startLatitude: Double,
        @Query("end_x") endLongitude: Double,
        @Query("end_y") endLatitude: Double,
    ): WalkResponse

    @GET("v2/routing/publictraffic")
    suspend fun publicTraffic(
        @Query("start_x") startLongitude: Double,
        @Query("start_y") startLatitude: Double,
        @Query("end_x") endLongitude: Double,
        @Query("end_y") endLatitude: Double,
    ): TransitResponse
}
