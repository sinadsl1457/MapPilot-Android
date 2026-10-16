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
        @Query("size") size: Int = PAGE_SIZE,
    ): KeywordSearchResponse

    /** 카테고리 검색. 응답 형식은 키워드 검색과 같다. */
    @GET("v2/local/search/category.json")
    suspend fun searchCategory(
        /** category_group_code (예: CE7 = 카페) */
        @Query("category_group_code") code: String,
        @Query("x") longitude: Double,
        @Query("y") latitude: Double,
        @Query("radius") radiusMeters: Int,
        @Query("sort") sort: String = "distance",
        @Query("size") size: Int = PAGE_SIZE,
    ): KeywordSearchResponse

    companion object {
        const val PAGE_SIZE = 15
    }
}
