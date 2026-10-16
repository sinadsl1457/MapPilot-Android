package com.example.simpletracking.domain.repository

/** 최근 검색어 저장소. 순서·개수 규칙은 RecentSearchesUseCase가 정한다. */
interface RecentSearchRepository {
    suspend fun load(): List<String>

    suspend fun save(queries: List<String>)
}
