package com.example.simpletracking.domain.usecase

import com.example.simpletracking.domain.repository.RecentSearchRepository
import javax.inject.Inject

/** 최근 검색어. 가장 최근 것이 앞에 오고, 같은 검색어는 한 번만, 최대 [MAX_COUNT]개까지 둔다. */
class RecentSearchesUseCase @Inject constructor(
    private val repository: RecentSearchRepository,
) {
    suspend fun list(): List<String> = repository.load()

    suspend fun add(query: String): List<String> {
        val normalized = normalizeQuery(query)
        if (normalized.isEmpty()) return repository.load()
        val queries = (listOf(normalized) + repository.load().filter { it != normalized }).take(MAX_COUNT)
        repository.save(queries)
        return queries
    }

    suspend fun remove(query: String): List<String> {
        val queries = repository.load().filter { it != query }
        repository.save(queries)
        return queries
    }

    suspend fun clear() = repository.save(emptyList())

    companion object {
        const val MAX_COUNT = 10
    }
}
