package com.example.simpletracking.presentation.common

import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberCoroutineScope
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
 *
 * 손잡이·[header]는 물론 [content]의 목록을 끌어도 시트가 움직인다(중첩 스크롤 연결). 목록을 위로 끌면 시트가 먼저
 * 맨 위([topReservedHeight])까지 올라가고, 다 올라가면 같은 손동작이 그대로 목록 스크롤로 이어진다(네이버 지도 방식).
 * 목록이 맨 위일 때 아래로 끌면 반대로 시트가 내려온다. [content] 안에 스크롤되는 목록이 있어야 한다.
 */
@Composable
fun MapBottomSheet(
    detent: SheetDetent,
    onDetentChange: (SheetDetent) -> Unit,
    onVisibleHeightChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    /** 끝까지 올렸을 때 화면 맨 위부터 시트 윗변까지(보통 검색창 아래 끝). 그 위의 UI는 가려지지 않는다. */
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
                    SheetDetent.MEDIUM at containerHeight * (1 - SHEET_MEDIUM_FRACTION)
                    SheetDetent.LARGE at topReservedHeight.toPx()
                }
            }
        }
        LaunchedEffect(anchors) { state.updateAnchors(anchors, state.targetValue) }
        // 화면이 단계를 바꾸면(장소를 고르면 접기 등) 그 단계로 움직인다.
        LaunchedEffect(detent) { if (state.settledValue != detent) state.animateTo(detent) }
        LaunchedEffect(state) { snapshotFlow { state.settledValue }.collect { currentOnDetentChange(it) } }
        val scope = rememberCoroutineScope()
        val nestedScroll = remember(state, scope) { SheetNestedScrollConnection(state, scope) }
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
                Column(Modifier.fillMaxWidth().weight(1f).nestedScroll(nestedScroll), content = content)
            }
        }
    }
}

/** 장소 상세(이름·주소·전화·길찾기 버튼)가 다 보이는 높이 */
private val COLLAPSED_HEIGHT = 280.dp
/** 중간 높이 = 화면 높이의 이 비율. 카메라 여백 계산(MapScreen)도 같은 값을 쓴다. */
const val SHEET_MEDIUM_FRACTION = 0.45f

/**
 * 목록 스크롤과 시트 끌기를 하나의 손동작으로 잇는다(Material BottomSheetScaffold와 같은 방식).
 * - 손가락을 위로: 시트가 맨 위가 아니면 시트가 먼저 받아 올라간다. 다 올라가면 남은 만큼 목록이 스크롤된다.
 * - 손가락을 아래로: 목록이 먼저 스크롤되고, 목록이 맨 위에 닿아 남은 만큼은 시트가 받아 내려간다.
 * - 손을 떼면: 시트가 단계 사이에 있으면 속도를 반영해 가까운 단계로 붙인다.
 */
private class SheetNestedScrollConnection(
    private val state: AnchoredDraggableState<SheetDetent>,
    private val scope: CoroutineScope,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y >= 0) return Offset.Zero
        return Offset(0f, state.dispatchRawDelta(available.y))
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y <= 0) return Offset.Zero
        return Offset(0f, state.dispatchRawDelta(available.y))
    }

    /** 맨 위가 아닌데 위로 튕기면 목록 대신 시트가 받는다(목록은 다음 손동작부터 스크롤). */
    override suspend fun onPreFling(available: Velocity): Velocity {
        val offset = state.offset
        if (offset.isNaN() || available.y >= 0 || offset <= state.anchors.minPosition()) return Velocity.Zero
        settle(available.y)
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (!state.offset.isNaN() && state.anchors.closestAnchor(state.offset)?.let { state.anchors.positionOf(it) } != state.offset) {
            settle(available.y)
        }
        return available
    }

    /** 놓은 위치에 속도를 조금 더해 가장 가까운 단계로 붙인다. */
    private suspend fun settle(velocity: Float) {
        val projected = state.offset + velocity * FLING_PROJECTION_SECONDS
        val target = SheetDetent.entries.minBy { abs(state.anchors.positionOf(it) - projected) }
        scope.launch { state.animateTo(target) }.join()
    }

    private companion object {
        const val FLING_PROJECTION_SECONDS = 0.15f
    }
}
