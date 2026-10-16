package com.example.simpletracking.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentSearchCodecTest {

    @Test
    fun `저장한 순서 그대로 다시 읽는다`() {
        val queries = listOf("서울역", "강남역 맛집", "\"따옴표\", 쉼표")

        assertEquals(queries, decodeQueries(encodeQueries(queries)))
    }

    @Test
    fun `값이 없거나 깨져 있으면 빈 목록`() {
        assertEquals(emptyList<String>(), decodeQueries(null))
        assertEquals(emptyList<String>(), decodeQueries(""))
        assertEquals(emptyList<String>(), decodeQueries("not json"))
    }
}
