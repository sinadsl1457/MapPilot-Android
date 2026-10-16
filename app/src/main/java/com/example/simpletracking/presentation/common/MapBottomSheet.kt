package com.example.simpletracking.presentation.common

import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filterNot
import kotlin.math.roundToInt

/** 하단 시트 높이 단계 */
enum class SheetDetent { COLLAPSED, MEDIUM, LARGE }

/**
 * 끌어서 높이를 바꾸는 하단 시트. Material의 ModalBottomSheet와 달리 지도 위에 직접 얹혀서 지도를 계속 만질 수 있고,
 * 현재 높이를 [onVisibleHeightChange]로 알려 내 위치 버튼 같은 다른 UI를 시트 위에 붙일 수 있다.
 * 손잡이와 [header]를 끌어서 높이를 바꾼다. [content]는 목록 스크롤에 쓴다.
 */
@Composable
fun MapBottomSheet(
    detent: SheetDetent,
    onDetentChange: (SheetDetent) -> Unit,
    onVisibleHeightChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    /** 펼쳤을 때 위에 남겨 둘 공간(검색창 자리) */
    topReservedHeight: Dp = 72.dp,
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val currentOnDetentChange by rememberUpdatedState(onDetentChange)
    val currentOnVisibleHeightChange by rememberUpdatedState(onVisibleHeightChange)
    val state = remember { AnchoredDraggableState(detent) }

    BoxWithConstraints(modifier) {
        val containerHeight = constraints.maxHeight.toFloat()
        // offset = 화면 위에서 시트 윗변까지의 거리. 작을수록 시트가 높다.
        val anchors = remember(containerHeight, density) {
            with(density) {
                DraggableAnchors {
                    SheetDetent.COLLAPSED at containerHeight - COLLAPSED_HEIGHT.toPx()
                    SheetDetent.MEDIUM at containerHeight * (1 - MEDIUM_FRACTION)
                    SheetDetent.LARGE at topReservedHeight.toPx()
                }
            }
        }
        LaunchedEffect(anchors) { state.updateAnchors(anchors, state.targetValue) }
        // 화면이 단계를 바꾸면(장소를 고르면 접기 등) 그 단계로 움직인다.
        LaunchedEffect(detent) { if (state.settledValue != detent) state.animateTo(detent) }
        LaunchedEffect(state) { snapshotFlow { state.settledValue }.collect { currentOnDetentChange(it) } }
        LaunchedEffect(state, containerHeight) {
            snapshotFlow { state.offset }
                .filterNot { it.isNaN() }
                .collect { currentOnVisibleHeightChange(with(density) { (containerHeight - it).toDp() }) }
        }

        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 8.dp,
            tonalElevation = 1.dp,
            modifier = Modifier
                // 드래그 중에는 리컴포지션 없이 배치(layout)만 다시 한다.
                .layout { measurable, constraints ->
                    val offset = state.offset.takeUnless { it.isNaN() } ?: anchors.positionOf(detent)
                    val height = (containerHeight - offset).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, offset.roundToInt()) }
                },
        ) {
            // Surface는 자기 영역의 터치를 막으므로 시트의 빈 곳을 눌러도 아래 지도가 움직이지 않는다.
            Column {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .anchoredDraggable(
                            state = state,
                            orientation = Orientation.Vertical,
                            flingBehavior = AnchoredDraggableDefaults.flingBehavior(state),
                        ),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp, bottom = 6.dp)
                            .size(width = 36.dp, height = 4.dp),
                    ) {}
                    header()
                }
                Column(Modifier.fillMaxWidth().weight(1f), content = content)
            }
        }
    }
}

/** 장소 상세(이름·주소·전화·길찾기 버튼)가 다 보이는 높이 */
private val COLLAPSED_HEIGHT = 280.dp
private const val MEDIUM_FRACTION = 0.45f
