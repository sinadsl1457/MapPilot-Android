package com.example.simpletracking.presentation.map.search

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.presentation.common.formatDistance

/** 하단 시트의 머리글: 결과 제목과 개수. 장소 상세를 볼 때는 비운다. */
@Composable
fun PlaceResultsHeader(state: PlaceSearchUiState) {
    if (state.selectedPlace != null) return
    val title = when (val source = state.source) {
        is SearchSource.Keyword -> source.keyword
        is SearchSource.Category -> stringResource(source.category.titleRes)
        null -> ""
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val places = (state.phase as? SearchPhase.Loaded)?.places
        if (!places.isNullOrEmpty()) {
            Text(
                stringResource(R.string.search_results_count, places.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 하단 시트 본문: 검색 결과 목록 또는 선택한 장소 상세 */
@Composable
fun PlaceResultsContent(
    state: PlaceSearchUiState,
    myLocation: GeoPoint?,
    onSelect: (Place) -> Unit,
    onBackToList: () -> Unit,
    onRoute: (Place) -> Unit,
) {
    val selected = state.selectedPlace
    if (selected != null) {
        PlaceDetail(
            place = selected,
            distance = formatDistance(myLocation, selected.location),
            onBack = onBackToList,
            onRoute = { onRoute(selected) },
        )
        return
    }
    when (val phase = state.phase) {
        SearchPhase.Loading, null -> Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.TopCenter) {
            CircularProgressIndicator()
        }
        is SearchPhase.Loaded -> if (phase.places.isEmpty()) {
            Text(
                stringResource(R.string.search_results_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        } else {
            LazyColumn {
                itemsIndexed(phase.places, key = { _, place -> place.id }) { index, place ->
                    PlaceRow(
                        number = index + 1,
                        place = place,
                        distance = formatDistance(myLocation, place.location),
                        onClick = { onSelect(place) },
                    )
                    HorizontalDivider(Modifier.padding(start = 54.dp))
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(number: Int, place: Place, distance: String?, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(top = 1.dp).size(22.dp).border(1.5.dp, tint, CircleShape),
        ) {
            Text("$number", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tint)
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    place.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    place.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                text = listOfNotNull(distance, place.address).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaceDetail(place: Place, distance: String?, onBack: () -> Unit, onRoute: () -> Unit) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            // 시트가 접혀 공간이 모자라도 버튼이 눌려 찌그러지지 않고 스크롤되게 한다.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                place.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) { Text(stringResource(R.string.search_back_to_list)) }
        }
        Text(
            listOfNotNull(place.category, distance).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconLine(Icons.Filled.Place, place.address)
        if (place.phone.isNotEmpty()) {
            IconLine(
                Icons.Filled.Phone,
                place.phone,
                onClick = {
                    // ACTION_DIAL은 전화 앱에 번호만 채워 준다(CALL_PHONE 권한 불필요).
                    val digits = place.phone.filter { it.isDigit() || it == '+' }
                    context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$digits".toUri()))
                },
            )
        }
        Button(
            onClick = onRoute,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth().height(48.dp),
        ) {
            Icon(Icons.Filled.Directions, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.route_start), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun IconLine(icon: ImageVector, text: String, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        val color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
