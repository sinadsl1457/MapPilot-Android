package com.example.simpletracking.presentation.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/**
 * [text]에서 [query]의 단어들이 나오는 곳마다 [style]을 입힌다(대소문자 무시).
 * "현대 백화점"처럼 띄어 입력해도 "현대백화점 판교점"의 "현대"·"백화점"이 각각 강조된다.
 */
fun highlightMatches(text: String, query: String, style: SpanStyle): AnnotatedString = buildAnnotatedString {
    append(text)
    query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { word ->
        var start = text.indexOf(word, ignoreCase = true)
        while (start >= 0) {
            addStyle(style, start, start + word.length)
            start = text.indexOf(word, start + word.length, ignoreCase = true)
        }
    }
}
