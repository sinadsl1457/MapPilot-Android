package com.example.simpletracking.presentation.map.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.simpletracking.domain.model.PlaceCategory

/** 검색창 아래 가로로 스크롤되는 카테고리 칩 */
@Composable
fun CategoryChips(
    selected: PlaceCategory?,
    onClick: (PlaceCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(PlaceCategory.entries) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onClick(category) },
                label = { Text(stringResource(category.titleRes)) },
                leadingIcon = { Icon(category.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                // 지도 위에서도 잘 보이도록 선택 전에도 바탕색과 그림자를 준다.
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = FilterChipDefaults.filterChipElevation(elevation = 2.dp),
            )
        }
    }
}
