package com.example.simpletracking.di

import com.example.simpletracking.BuildConfig
import com.example.simpletracking.data.remote.api.KakaoLocalApi
import com.example.simpletracking.data.remote.api.KakaoNaviApi
import com.example.simpletracking.data.remote.api.KakaoRoutingApi
import com.example.simpletracking.data.remote.interceptor.KakaoAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import javax.inject.Singleton

/**
 * @Provides: 생성자에 @Inject를 붙일 수 없는 외부 라이브러리 객체를 만드는 방법.
 * @InstallIn(SingletonComponent): 앱 전체 수명 동안 유지 (get_it의 registerLazySingleton과 비슷).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val KAKAO_LOCAL_BASE_URL = "https://dapi.kakao.com/"
    private const val KAKAO_NAVI_BASE_URL = "https://apis-navi.kakaomobility.com/"

    @Provides
    @KakaoRestApiKey
    fun provideKakaoRestApiKey(): String = BuildConfig.KAKAO_REST_API_KEY

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // 서버가 필드를 추가해도 앱이 죽지 않게 한다.
        ignoreUnknownKeys = true
        // null이 오면 기본값으로 대체한다.
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: KakaoAuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

    @Provides
    @Singleton
    @KakaoLocalRetrofit
    fun provideLocalRetrofit(client: OkHttpClient, json: Json): Retrofit =
        buildRetrofit(KAKAO_LOCAL_BASE_URL, client, json)

    @Provides
    @Singleton
    @KakaoNaviRetrofit
    fun provideNaviRetrofit(client: OkHttpClient, json: Json): Retrofit =
        buildRetrofit(KAKAO_NAVI_BASE_URL, client, json)

    @Provides
    @Singleton
    fun provideKakaoLocalApi(@KakaoLocalRetrofit retrofit: Retrofit): KakaoLocalApi = retrofit.create()

    @Provides
    @Singleton
    fun provideKakaoNaviApi(@KakaoNaviRetrofit retrofit: Retrofit): KakaoNaviApi = retrofit.create()

    /** 도보·대중교통 경로는 로컬 API와 같은 dapi.kakao.com에 있다. */
    @Provides
    @Singleton
    fun provideKakaoRoutingApi(@KakaoLocalRetrofit retrofit: Retrofit): KakaoRoutingApi = retrofit.create()

    // OkHttpClient 하나를 두 Retrofit이 공유한다 → 커넥션 풀·스레드 풀도 공유된다.
    private fun buildRetrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
