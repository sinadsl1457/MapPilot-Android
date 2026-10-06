package com.example.simpletracking.data.remote.api

import com.example.simpletracking.data.remote.dto.KeywordSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** 카카오 로컬 API (https://dapi.kakao.com) */
interface KakaoLocalApi {
    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Query("query") query: String,
        @Query("x") longitude: Double? = null,
        @Query("y") latitude: Double? = null,
        @Query("size") size: Int = 15,
    ): KeywordSearchResponse
}
