package com.example.simpletracking.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.simpletracking.domain.repository.RecentSearchRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** 최근 검색어를 기기 안(SharedPreferences)에만 저장한다. 순서를 지키려고 JSON 배열 문자열로 넣는다. */
class RecentSearchRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : RecentSearchRepository {

    // 첫 접근 때 디스크에서 파일을 읽으므로 메인 스레드에서 만들지 않는다.
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    override suspend fun load(): List<String> = withContext(Dispatchers.IO) {
        decodeQueries(prefs.getString(KEY, null))
    }

    override suspend fun save(queries: List<String>) = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY, encodeQueries(queries)) }
    }

    private companion object {
        const val PREFS_NAME = "recent_search"
        const val KEY = "queries"
    }
}

internal fun encodeQueries(queries: List<String>): String = Json.encodeToString(queries)

/** 저장된 값이 없거나 깨져 있으면 빈 목록으로 본다. */
internal fun decodeQueries(raw: String?): List<String> {
    if (raw.isNullOrEmpty()) return emptyList()
    return try {
        Json.decodeFromString<List<String>>(raw)
    } catch (e: SerializationException) {
        emptyList()
    } catch (e: IllegalArgumentException) {
        emptyList()
    }
}
