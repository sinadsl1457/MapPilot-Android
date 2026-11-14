package com.example.simpletracking.presentation.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.example.simpletracking.R

/**
 * 지도 우하단에 떠 있는 내 위치 버튼. 아이콘으로 따라가기 모드를 보여 준다.
 *
 * | 상태 | 아이콘 | 누르면 |
 * |---|---|---|
 * | 권한 없음 | 위치 꺼짐(회색) | 권한 요청 |
 * | NONE (지도를 옮겨 둠) | 조준점(검정) | 내 위치로 카메라만 이동 |
 * | NONE (카메라가 내 위치) | 조준점(검정) | 따라가기 |
 * | FOLLOW | 조준점(파랑) | 지도 회전까지 |
 * | FOLLOW_WITH_HEADING | 나침반 화살표(파랑) | 따라가기 해제 |
 */
@Composable
fun MyLocationButton(
    hasPermission: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    followMode: FollowMode = FollowMode.NONE,
    isCameraOnMyLocation: Boolean = false,
) {
    val hint = stringResource(
        when {
            !hasPermission -> R.string.my_location_hint_unauthorized
            followMode == FollowMode.FOLLOW -> R.string.my_location_hint_rotate
            followMode == FollowMode.FOLLOW_WITH_HEADING -> R.string.my_location_hint_stop
            isCameraOnMyLocation -> R.string.my_location_hint_follow
            else -> R.string.my_location_hint_move
        }
    )
    val colors = MaterialTheme.colorScheme
    FloatingActionButton(
        onClick = onClick,
        containerColor = colors.surface,
        contentColor = when {
            !hasPermission -> colors.onSurfaceVariant
            followMode == FollowMode.NONE -> colors.onSurface
            else -> colors.primary
        },
        // TalkBack이 "두 번 탭하여 (힌트)"로 읽는다.
        modifier = modifier.semantics { onClick(label = hint) { onClick(); true } },
    ) {
        Icon(
            imageVector = when {
                !hasPermission -> Icons.Filled.LocationDisabled
                followMode == FollowMode.FOLLOW_WITH_HEADING -> Icons.Filled.Navigation
                else -> Icons.Filled.MyLocation
            },
            contentDescription = stringResource(R.string.my_location),
        )
    }
}
