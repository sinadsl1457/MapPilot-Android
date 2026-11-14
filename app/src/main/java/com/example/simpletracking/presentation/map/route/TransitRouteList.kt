package com.example.simpletracking.presentation.map.route

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.TransitLeg
import com.example.simpletracking.domain.model.TransitRoute
import com.example.simpletracking.domain.model.TravelMode
import com.example.simpletracking.presentation.common.arrivalTimeText
import com.example.simpletracking.presentation.common.durationText
import com.example.simpletracking.presentation.common.formatDistance
import com.example.simpletracking.presentation.common.formatWon

/** 대중교통 하단 시트 머리글 */
@Composable
fun TransitRouteListHeader(result: ModeResult?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
    ) {
        Icon(TravelMode.TRANSIT.icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.travel_mode_transit), style = MaterialTheme.typography.titleMedium)
        if (result is ModeResult.Transit) {
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.route_transit_count, result.routes.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 대중교통 경로 후보 목록(RouteListPage). 하나를 고르면 지도에 그 경로를 그린다. [selectedIndex]가 null이면 강조하지 않는다. */
@Composable
fun TransitRouteList(result: ModeResult?, selectedIndex: Int?, onSelect: (Int) -> Unit) {
    when (result) {
        is ModeResult.Transit -> LazyColumn {
            itemsIndexed(result.routes) { index, route ->
                TransitRouteRow(route, isSelected = index == selectedIndex, onClick = { onSelect(index) })
                HorizontalDivider(Modifier.padding(start = 20.dp))
            }
        }
        is ModeResult.Failed -> Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.TopCenter) {
            RouteFailureText(result.error)
        }
        ModeResult.Loading, is ModeResult.Found, null -> Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.TopCenter) {
            RouteLoadingText()
        }
    }
}

/** 대중교통 경로 한 줄. 목록과 지도 위 요약 카드(RouteSummaryCard, [onClick] 없음)가 같이 쓴다. */
@Composable
fun TransitRouteRow(route: TransitRoute, isSelected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(if (isSelected) accent.copy(alpha = 0.08f) else Color.Transparent)
            // 왼쪽 선택 표시 막대는 배경에 직접 그린다. IntrinsicSize로 높이를 맞추면
            // 안에 있는 BoxWithConstraints(구간 막대) 때문에 Compose가 크래시를 낸다.
            .drawBehind { if (isSelected) drawRect(accent, size = Size(3.dp.toPx(), size.height)) },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(durationText(route.durationSeconds), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.route_arrival, arrivalTimeText(route.durationSeconds)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Text(detailLine(route), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TransitSegmentBar(route.legs)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                route.legs.filter { it.kind != TransitLeg.Kind.WALK }.forEach { TransitLegLine(it) }
            }
        }
    }
}

/** "환승 1회 · 1,850원 · 25.9km" */
@Composable
private fun detailLine(route: TransitRoute): String = listOfNotNull(
    if (route.transfers == 0) stringResource(R.string.route_transfers_none) else stringResource(R.string.route_transfers, route.transfers),
    route.fare?.let { stringResource(R.string.route_fare, formatWon(it)) },
    formatDistance(route.distanceMeters.toDouble()),
).joinToString(" · ")

/** 구간별 소요 시간 비율 막대. 색은 지도 경로 선과 같다. */
@Composable
private fun TransitSegmentBar(legs: List<TransitLeg>) {
    if (legs.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp)) {
        val total = maxOf(1, legs.sumOf { it.durationSeconds })
        val spacing = 2.dp
        val available = maxWidth - spacing * (legs.size - 1)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            legs.forEach { leg ->
                val width = maxOf(14.dp, available * (leg.durationSeconds.toFloat() / total))
                val color = RouteColors.leg(leg.kind).let { if (leg.kind == TransitLeg.Kind.WALK) it.copy(alpha = 0.45f) else it }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.width(width).fillMaxHeight().background(color, RoundedCornerShape(50)),
                ) {
                    if (width > 36.dp) {
                        Text(
                            "${maxOf(1, leg.durationSeconds / 60)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

/** "🚇 수인분당선  태평 → 왕십리 · 14개 정류장" */
@Composable
private fun TransitLegLine(leg: TransitLeg) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp).background(RouteColors.leg(leg.kind), CircleShape)) {
            Icon(leg.kind.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Text(
            if (leg.lineNames.isEmpty()) leg.guidance else leg.lineNames.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        val from = leg.stops.firstOrNull()
        val to = leg.stops.lastOrNull()
        if (from != null && to != null) {
            Text(
                "$from → $to",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (leg.stops.size > 1) {
            Text(
                stringResource(R.string.route_stops, leg.stops.size - 1),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
            )
        }
    }
}
