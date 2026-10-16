package com.example.simpletracking.presentation.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.example.simpletracking.R

/** 지도 우하단에 떠 있는 내 위치 버튼. 권한이 없으면 아이콘이 바뀌고 누르면 권한을 요청한다. */
@Composable
fun MyLocationButton(hasPermission: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hint = stringResource(if (hasPermission) R.string.my_location_hint_authorized else R.string.my_location_hint_unauthorized)
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        // TalkBack이 "두 번 탭하여 (힌트)"로 읽는다.
        modifier = modifier.semantics { onClick(label = hint) { onClick(); true } },
    ) {
        Icon(
            imageVector = if (hasPermission) Icons.Filled.MyLocation else Icons.Filled.LocationDisabled,
            contentDescription = stringResource(R.string.my_location),
        )
    }
}
