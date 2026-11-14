package com.example.simpletracking.presentation.map.route

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.simpletracking.R

/** 검색창 오른쪽의 파란 길찾기 버튼. 누르면 길찾기 준비 화면(RoutePlannerPage)이 열린다. */
@Composable
fun DirectionsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Icon(
            Icons.Filled.Directions,
            contentDescription = stringResource(R.string.route_start),
            modifier = Modifier.padding(12.dp),
        )
    }
}


/** 대중교통 경로를 지도에서 보는 중 상단: ‹(목록으로) + "출발 → 도착". 줄 어디를 눌러도 목록으로 간다. */
@Composable
fun RouteShownBar(originName: String, destinationName: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onBack,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = stringResource(R.string.route_back_to_list))
            }
            Text(
                "$originName → $destinationName",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(20.dp))
        }
    }
}
