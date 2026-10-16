package com.example.simpletracking.presentation.common

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class HighlightTextTest {

    private val bold = SpanStyle(fontWeight = FontWeight.Bold)

    private fun highlighted(text: String, query: String): List<String> =
        highlightMatches(text, query, bold).spanStyles.map { text.substring(it.start, it.end) }

    @Test
    fun `입력한 글자가 나오는 곳을 강조한다`() {
        assertEquals(listOf("현대백화"), highlighted("현대백화점 판교점", "현대백화"))
    }

    @Test
    fun `띄어 입력한 단어는 각각 강조한다`() {
        assertEquals(listOf("현대", "백화점"), highlighted("현대백화점", "현대 백화점"))
    }

    @Test
    fun `영문은 대소문자를 가리지 않고, 여러 번 나오면 모두 강조한다`() {
        assertEquals(listOf("KT", "kt"), highlighted("KT&G kt", "kt"))
    }

    @Test
    fun `빈 검색어는 아무것도 강조하지 않는다`() {
        assertEquals(emptyList<String>(), highlighted("현대백화점", "  "))
    }
}
