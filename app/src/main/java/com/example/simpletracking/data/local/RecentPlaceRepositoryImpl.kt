package com.example.simpletracking.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.RecentPlace
import com.example.simpletracking.domain.repository.RecentPlaceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** 최근 장소를 기기 안(SharedPreferences)에 JSON으로 저장한다. 저장 형식(DTO)은 이 파일만 안다. */
class RecentPlaceRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : RecentPlaceRepository {

    // 첫 접근 때 디스크에서 파일을 읽으므로 메인 스레드에서 만들지 않는다.
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    override suspend fun load(): List<RecentPlace> = withContext(Dispatchers.IO) {
        decodeRecentPlaces(prefs.getString(KEY, null))
    }

    override suspend fun save(places: List<RecentPlace>) = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY, encodeRecentPlaces(places)) }
    }

    private companion object {
        const val PREFS_NAME = "recent_place"
        const val KEY = "places"
    }
}

/** 저장 형식. 도메인 모델(Place)에 직렬화 어노테이션을 붙이지 않으려고 따로 둔다. */
@Serializable
internal data class RecentPlaceDto(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val usedAtMillis: Long,
)

internal fun encodeRecentPlaces(places: List<RecentPlace>): String = Json.encodeToString(
    places.map {
        RecentPlaceDto(
            it.place.id, it.place.name, it.place.category, it.place.address, it.place.phone,
            it.place.location.latitude, it.place.location.longitude, it.usedAtMillis,
        )
    }
)

/** 저장된 값이 없거나 깨져 있으면(형식 변경 등) 빈 목록으로 본다. 거리는 저장하지 않는다(고른 때의 내 위치 기준이라). */
internal fun decodeRecentPlaces(raw: String?): List<RecentPlace> {
    if (raw.isNullOrEmpty()) return emptyList()
    return try {
        Json.decodeFromString<List<RecentPlaceDto>>(raw).map {
            RecentPlace(
                Place(it.id, it.name, it.category, it.address, it.phone, GeoPoint(it.latitude, it.longitude), distanceMeters = null),
                it.usedAtMillis,
            )
        }
    } catch (e: SerializationException) {
        emptyList()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }
}
