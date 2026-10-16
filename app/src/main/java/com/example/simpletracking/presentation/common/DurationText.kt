package com.example.simpletracking.presentation.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.simpletracking.R
import java.text.NumberFormat
import java.util.Date
import kotlin.math.ceil

/** 분 단위로 올린 (시간, 분). 예: 45초 → (0, 1), 3906초 → (1, 6), 7200초 → (2, 0) */
fun durationParts(seconds: Int): Pair<Int, Int> {
    val minutes = maxOf(1, ceil(seconds / 60.0).toInt())
    return minutes / 60 to minutes % 60
}

/** "25분", "1시간 6분", "2시간" */
@Composable
fun durationText(seconds: Int): String {
    val (hours, minutes) = durationParts(seconds)
    return when {
        hours == 0 -> stringResource(R.string.duration_minutes, minutes)
        minutes == 0 -> stringResource(R.string.duration_hours, hours)
        else -> stringResource(R.string.duration_hours_minutes, hours, minutes)
    }
}

/** 지금부터 [seconds] 뒤의 시각. 기기의 12/24시간 설정을 따른다. */
@Composable
fun arrivalTimeText(seconds: Int): String {
    val format = DateFormat.getTimeFormat(LocalContext.current)
    return format.format(Date(System.currentTimeMillis() + seconds * 1_000L))
}

/** 26800 → "26,800" */
fun formatWon(amount: Int): String = NumberFormat.getIntegerInstance().format(amount)
