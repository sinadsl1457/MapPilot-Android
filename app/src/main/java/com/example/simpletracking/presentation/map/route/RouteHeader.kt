package com.example.simpletracking.presentation.map.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.TravelMode
import com.example.simpletracking.presentation.common.durationText

/** 길찾기 중 상단에 뜨는 출발지·도착지 카드 */
@Composable
fun RouteHeader(destinationName: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), shadowElevation = 4.dp, modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
            Column(Modifier.weight(1f)) {
                EndpointRow(RouteColors.Start, stringResource(R.string.route_origin_my_location))
                HorizontalDivider(Modifier.padding(start = 24.dp))
                EndpointRow(RouteColors.End, destinationName)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.route_close))
            }
        }
    }
}

@Composable
private fun EndpointRow(color: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(44.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 길찾기 상단의 이동 수단 탭. 탭마다 소요 시간을 미리 보여준다. */
@Composable
fun TravelModeTabs(
    selected: TravelMode,
    results: Map<TravelMode, ModeResult>,
    onSelect: (TravelMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        TravelMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val title = stringResource(mode.titleRes)
            val summary = when (val result = results[mode]) {
                is ModeResult.Found -> durationText(result.route.durationSeconds)
                is ModeResult.Transit -> durationText(result.routes.firstOrNull()?.durationSeconds ?: 0)
                is ModeResult.Failed -> stringResource(R.string.route_unavailable)
                ModeResult.Loading, null -> null
            }
            Surface(
                selected = isSelected,
                onClick = { onSelect(mode) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .semantics { contentDescription = listOfNotNull(title, summary).joinToString(" ") },
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(mode.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    when (val result = results[mode]) {
                        ModeResult.Loading, null -> CircularProgressIndicator(
                            Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                        )
                        is ModeResult.Failed -> Text("–", style = MaterialTheme.typography.labelLarge)
                        else -> Text(summary.orEmpty(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
    }
}
