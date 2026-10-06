package com.example.simpletracking.data.remote.interceptor

import com.example.simpletracking.di.KakaoRestApiKey
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Dio의 InterceptorsWrapper(onRequest에서 헤더 추가)에 해당한다.
 * 키를 BuildConfig에서 직접 읽지 않고 주입받으므로 테스트에서 다른 값을 넣을 수 있다.
 */
class KakaoAuthInterceptor @Inject constructor(
    @param:KakaoRestApiKey private val apiKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .header("Authorization", "KakaoAK $apiKey")
                .build()
        )
}
