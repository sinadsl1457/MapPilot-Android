package com.example.simpletracking.testutil

import com.example.simpletracking.domain.repository.RecentPlaceRepository
import com.example.simpletracking.domain.model.RecentPlace
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.PlaceCategory
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.model.TransitLeg
import com.example.simpletracking.domain.model.TransitRoute
import com.example.simpletracking.domain.repository.PlaceRepository
import com.example.simpletracking.domain.repository.RecentSearchRepository
import com.example.simpletracking.domain.repository.RouteRepository
import com.example.simpletracking.domain.result.AppResult

/** mockito 대신 직접 만든 Fake. 인터페이스라서 가능하다(Repository를 인터페이스로 둔 이유 중 하나). */
class FakePlaceRepository : PlaceRepository {
    var keywordResult: AppResult<List<Place>> = AppResult.Success(emptyList())
    var categoryResult: AppResult<List<Place>> = AppResult.Success(emptyList())
    var lastQuery: String? = null
    val keywordQueries = mutableListOf<String>()
    var lastNear: GeoPoint? = null
    var lastCategory: PlaceCategory? = null
    var lastCenter: GeoPoint? = null
    var lastRadius: Int? = null

    override suspend fun search(query: String, near: GeoPoint?): AppResult<List<Place>> {
        lastQuery = query
        keywordQueries += query
        lastNear = near
        return keywordResult
    }

    override suspend fun searchByCategory(category: PlaceCategory, center: GeoPoint, radiusMeters: Int): AppResult<List<Place>> {
        lastCategory = category
        lastCenter = center
        lastRadius = radiusMeters
        return categoryResult
    }
}

class FakeRouteRepository : RouteRepository {
    var carResult: AppResult<Route> = AppResult.Success(route(minutes = 30))
    var walkResult: AppResult<Route> = AppResult.Success(route(minutes = 90))
    var transitResult: AppResult<List<TransitRoute>> = AppResult.Success(listOf(transitRoute(minutes = 40)))
    val calls = mutableListOf<String>()
    /** 자동차 경로 요청의 (출발, 도착). 출발지·도착지를 바꾼 뒤 무엇으로 찾았는지 확인할 때 쓴다. */
    val carRequests = mutableListOf<Pair<GeoPoint, GeoPoint>>()

    override suspend fun findCarRoute(origin: GeoPoint, destination: GeoPoint): AppResult<Route> {
        calls += "car"
        carRequests += origin to destination
        return carResult
    }

    override suspend fun findWalkingRoute(origin: GeoPoint, destination: GeoPoint): AppResult<Route> {
        calls += "walk"
        return walkResult
    }

    override suspend fun findTransitRoutes(origin: GeoPoint, destination: GeoPoint): AppResult<List<TransitRoute>> {
        calls += "transit"
        return transitResult
    }
}

class FakeRecentSearchRepository(initial: List<String> = emptyList()) : RecentSearchRepository {
    var saved: List<String> = initial
    override suspend fun load(): List<String> = saved
    override suspend fun save(queries: List<String>) {
        saved = queries
    }
}

class FakeRecentPlaceRepository(initial: List<RecentPlace> = emptyList()) : RecentPlaceRepository {
    var saved: List<RecentPlace> = initial
    override suspend fun load(): List<RecentPlace> = saved
    override suspend fun save(places: List<RecentPlace>) {
        saved = places
    }
}

object Fixtures {
    val cityHall = GeoPoint(latitude = 37.5666, longitude = 126.9784)
    val gangnamStation = GeoPoint(latitude = 37.49794, longitude = 127.02764)
}

fun place(id: String, latitude: Double = 37.5, longitude: Double = 127.0) = Place(
    id = id,
    name = "장소 $id",
    category = "카페",
    address = "주소 $id",
    phone = "",
    location = GeoPoint(latitude, longitude),
    distanceMeters = null,
)

fun route(minutes: Int, points: List<GeoPoint> = listOf(Fixtures.cityHall, Fixtures.gangnamStation)) =
    Route(points = points, distanceMeters = 10_000, durationSeconds = minutes * 60)

fun transitRoute(minutes: Int, legs: List<TransitLeg> = emptyList()) = TransitRoute(
    kind = TransitRoute.Kind.SUBWAY,
    durationSeconds = minutes * 60,
    distanceMeters = 10_000,
    transfers = 0,
    fare = 1_500,
    legs = legs,
)

fun transitLeg(kind: TransitLeg.Kind, vararg points: GeoPoint) = TransitLeg(
    kind = kind,
    durationSeconds = 600,
    distanceMeters = 1_000,
    guidance = "",
    lineNames = emptyList(),
    stops = emptyList(),
    points = points.toList(),
)
