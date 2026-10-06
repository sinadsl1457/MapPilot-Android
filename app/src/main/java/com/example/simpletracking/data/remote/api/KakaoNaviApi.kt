package com.example.simpletracking.data.remote.api

import com.example.simpletracking.data.remote.dto.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** 카카오모빌리티 길찾기 API (https://apis-navi.kakaomobility.com) - 자동차 경로만 제공 */
interface KakaoNaviApi {
    @GET("v1/directions")
    suspend fun directions(
        /** "경도,위도" */
        @Query("origin") origin: String,
        /** "경도,위도" */
        @Query("destination") destination: String,
        /** RECOMMEND(추천) | TIME(최단시간) | DISTANCE(최단거리) */
        @Query("priority") priority: String,
    ): DirectionsResponse
}
