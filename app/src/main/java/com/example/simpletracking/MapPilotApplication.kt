package com.example.simpletracking

import android.app.Application
import android.util.Log
import com.kakao.vectormap.KakaoMapSdk
import dagger.hilt.android.HiltAndroidApp

/**
 * @HiltAndroidApp: Hilt 의존성 그래프의 루트(SingletonComponent)를 앱 수명에 맞춰 생성한다.
 * Flutter에서 main()의 configureDependencies()(get_it + injectable)를 부르는 자리에 해당한다.
 */
@HiltAndroidApp
class MapPilotApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 키가 비어 있으면 SDK가 IllegalArgumentException을 던져 앱이 바로 죽는다.
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            Log.w(TAG, "local.properties에 KAKAO_NATIVE_APP_KEY가 없습니다. 지도가 표시되지 않습니다.")
            return
        }
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        // 카카오 개발자 콘솔 > 플랫폼 > Android에 등록해야 하는 키 해시
        Log.i(TAG, "Kakao key hash: ${KakaoMapSdk.INSTANCE.hashKey}")
    }

    private companion object {
        const val TAG = "MapPilot"
    }
}
