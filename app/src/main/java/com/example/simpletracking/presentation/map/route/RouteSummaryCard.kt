package com.example.simpletracking.presentation.map.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.Route
import com.example.simpletracking.domain.model.TravelMode
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.presentation.common.arrivalTimeText
import com.example.simpletracking.presentation.common.durationText
import com.example.simpletracking.presentation.common.formatDistance
import com.example.simpletracking.presentation.common.formatWon
import com.example.simpletracking.presentation.common.messageRes

/** 자동차·도보 길찾기 하단 카드: 예상 시간, 거리, 도착 예정 시각, 요금 */
@Composable
fun RouteSummaryCard(mode: TravelMode, result: ModeResult?, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Icon(mode.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(mode.titleRes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            when (result) {
                is ModeResult.Found -> RouteSummary(result.route)
                is ModeResult.Failed -> RouteFailureText(result.error, Modifier.height(60.dp))
                ModeResult.Loading, is ModeResult.Transit, null -> RouteLoadingText(Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun RouteSummary(route: Route) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(durationText(route.durationSeconds), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            formatDistance(route.distanceMeters.toDouble()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
    SecondaryText(stringResource(R.string.route_arrival, arrivalTimeText(route.durationSeconds)))
    // "택시 약 26,800원 · 통행료 1,200원". 값이 없거나 0이면 생략한다.
    val fares = listOfNotNull(
        route.taxiFare?.takeIf { it > 0 }?.let { stringResource(R.string.route_fare_taxi, formatWon(it)) },
        route.tollFare?.takeIf { it > 0 }?.let { stringResource(R.string.route_fare_toll, formatWon(it)) },
    )
    if (fares.isNotEmpty()) SecondaryText(fares.joinToString(" · "))
}

@Composable
private fun SecondaryText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun RouteFailureText(error: DomainError, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        SecondaryText(stringResource(error.messageRes()))
    }
}

@Composable
fun RouteLoadingText(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        SecondaryText(stringResource(R.string.route_loading))
    }
}
