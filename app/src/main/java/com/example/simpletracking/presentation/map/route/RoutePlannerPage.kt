package com.example.simpletracking.presentation.map.route

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.RecentPlace
import com.example.simpletracking.domain.model.TravelMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 지도 첫 화면의 길찾기 버튼으로 여는 준비 화면. 출발지(기본: 내 위치)·도착지·이동 수단을 정한다.
 * 두 지점이 정해지면 RoutePlannerViewModel이 Completed를 내고 MapRoute가 이 화면을 닫는다.
 */
@Composable
fun RoutePlannerPage(
    state: RoutePlannerUiState,
    onTapOrigin: () -> Unit,
    onTapDestination: () -> Unit,
    onSwap: () -> Unit,
    onClose: () -> Unit,
    onSelectMode: (TravelMode) -> Unit,
    onRecentClick: (RecentPlace) -> Unit,
    onRecentRemove: (RecentPlace) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            RouteHeader(
                originName = state.origin?.title(),
                destinationName = state.destination?.title(),
                onTapOrigin = onTapOrigin,
                onTapDestination = onTapDestination,
                onSwap = onSwap,
                onClose = onClose,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            )
            ModeIconTabs(state.mode, onSelectMode)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            if (state.recentPlaces.isEmpty()) {
                Text(
                    stringResource(R.string.route_recent_places_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 40.dp),
                )
            } else {
                LazyColumn {
                    items(state.recentPlaces, key = { it.place.id }) { recent ->
                        RecentPlaceRow(recent, onClick = { onRecentClick(recent) }, onRemove = { onRecentRemove(recent) })
                        HorizontalDivider(Modifier.padding(start = 56.dp))
                    }
                }
            }
        }
    }
}

/** 이동 수단 아이콘 탭. 아직 결과가 없어 시간 대신 아이콘만 보여준다. */
@Composable
private fun ModeIconTabs(selected: TravelMode, onSelect: (TravelMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
        TravelMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val title = stringResource(mode.titleRes)
            Surface(
                onClick = { onSelect(mode) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(width = 64.dp, height = 40.dp).semantics {
                    contentDescription = title
                    this.selected = isSelected
                },
            ) {
                Icon(mode.icon, contentDescription = null, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
private fun RecentPlaceRow(recent: RecentPlace, onClick: () -> Unit, onRemove: () -> Unit) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 20.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Icon(Icons.Filled.Place, contentDescription = null, tint = secondary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(recent.place.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(recent.place.address, style = MaterialTheme.typography.bodySmall, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(monthDay(recent.usedAtMillis), style = MaterialTheme.typography.bodySmall, color = secondary)
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.search_recent_remove, recent.place.name),
                tint = secondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** "11.14." */
private fun monthDay(millis: Long): String = SimpleDateFormat("MM.dd.", Locale.KOREA).format(Date(millis))
