package com.example.simpletracking.di

import android.content.Context
import com.example.simpletracking.BuildConfig
import com.example.simpletracking.data.location.FixedLocationRepository
import com.example.simpletracking.data.location.LocationRepositoryImpl
import com.example.simpletracking.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    /** Google Play 서비스 라이브러리 객체라 생성자에 @Inject를 붙일 수 없다 → @Provides */
    @Provides
    @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * local.properties에 FIXED_LOCATION("위도,경도")이 있으면 GPS 대신 그 좌표를 내 위치로 쓴다.
     * Provider: 고정 위치를 쓰면 GPS 구현체(와 FusedLocationProviderClient)를 아예 만들지 않는다.
     */
    @Provides
    @Singleton
    fun provideLocationRepository(gps: Provider<LocationRepositoryImpl>): LocationRepository =
        FixedLocationRepository.parse(BuildConfig.FIXED_LOCATION)
            ?.let { FixedLocationRepository(it) }
            ?: gps.get()
}
