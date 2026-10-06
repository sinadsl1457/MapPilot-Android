package com.example.simpletracking.di

import com.example.simpletracking.data.location.LocationRepositoryImpl
import com.example.simpletracking.data.repository.PlaceRepositoryImpl
import com.example.simpletracking.data.repository.RouteRepositoryImpl
import com.example.simpletracking.domain.repository.LocationRepository
import com.example.simpletracking.domain.repository.PlaceRepository
import com.example.simpletracking.domain.repository.RouteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @Binds: "인터페이스를 요청하면 이 구현체를 줘라"라는 연결만 한다(injectable의 @Injectable(as: ...)).
 * 객체 생성은 Impl의 @Inject constructor가 맡으므로 추상 메서드만 있으면 된다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlaceRepository(impl: PlaceRepositoryImpl): PlaceRepository

    @Binds
    @Singleton
    abstract fun bindRouteRepository(impl: RouteRepositoryImpl): RouteRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
}
