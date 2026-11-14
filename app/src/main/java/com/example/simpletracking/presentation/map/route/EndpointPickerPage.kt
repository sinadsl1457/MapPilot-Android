package com.example.simpletracking.presentation.map.route

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.RouteEndpoint
import com.example.simpletracking.presentation.common.formatDistance

/**
 * 길찾기 출발지·도착지를 고르는 페이지. 맨 위 "내 위치", 입력 중이면 실시간 검색 결과, 비어 있으면 최근 검색어.
 * 고른 지점을 [onPick]으로 돌려준다(닫는 건 부르는 쪽이 한다).
 * 여는 곳: RoutePlannerPage, RouteListPage, MapScreen(자동차·도보 경로를 보는 중 카드 탭)
 */
@Composable
fun EndpointPickerPage(
    title: String,
    state: EndpointSearchUiState,
    myLocation: GeoPoint?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRecentClick: (String) -> Unit,
    onRecentRemove: (String) -> Unit,
    onPick: (RouteEndpoint) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
                TextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.route_picker_placeholder, title)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    trailingIcon = {
                        when {
                            state.isSubmitting -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            state.query.isNotEmpty() -> IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Cancel, contentDescription = stringResource(R.string.search_clear_text))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                )
            }
            HorizontalDivider()
            LazyColumn {
                item {
                    ListRow(
                        icon = { Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { onPick(RouteEndpoint.MyLocation) },
                    ) { Text(stringResource(R.string.route_origin_my_location), style = MaterialTheme.typography.bodyLarge) }
                    HorizontalDivider()
                }
                val results = state.results
                when {
                    results == null && state.recentQueries.isEmpty() -> item { EmptyText(stringResource(R.string.search_recent_empty)) }
                    results == null -> {
                        item {
                            Text(
                                stringResource(R.string.search_recent_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(state.recentQueries, key = { "recent:$it" }) { recent ->
                            ListRow(
                                icon = { Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { onRecentClick(recent) },
                                trailing = {
                                    IconButton(onClick = { onRecentRemove(recent) }) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.search_recent_remove, recent),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                },
                            ) { Text(recent, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                    results.isEmpty() -> item { EmptyText(stringResource(R.string.search_results_empty)) }
                    else -> items(results, key = { it.id }) { place ->
                        PlaceRow(place, formatDistance(myLocation, place.location)) { onPick(RouteEndpoint.PlaceEndpoint(place)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListRow(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) { content() }
        trailing?.invoke()
    }
}

/** 장소 한 줄: 이름 + 분류, 거리 · 주소 */
@Composable
private fun PlaceRow(place: Place, distance: String?, onClick: () -> Unit) {
    ListRow(
        icon = { Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        onClick = onClick,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(place.name) }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = MaterialTheme.typography.labelSmall.fontSize)) {
                    append("  ${place.category}")
                }
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOfNotNull(distance, place.address).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
