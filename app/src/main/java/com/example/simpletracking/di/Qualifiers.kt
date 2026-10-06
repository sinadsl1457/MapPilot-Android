package com.example.simpletracking.di

import javax.inject.Qualifier

/**
 * 같은 타입(Retrofit, String)이 여러 개일 때 구분하는 표식.
 * get_it의 instanceName / injectable의 @Named("...")와 같지만, 문자열이 아닌 타입이라 오타가 컴파일 에러가 된다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KakaoLocalRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KakaoNaviRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KakaoRestApiKey
