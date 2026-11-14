package com.example.simpletracking.presentation.map.route

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.RouteEndpoint
import com.example.simpletracking.domain.result.DomainError

/** 출발지·도착지 선택 페이지를 여는 대상 */
enum class EndpointRole { ORIGIN, DESTINATION }

/** 길찾기 흐름에서 화면만 아는 상태: 준비 화면이 열려 있는지, 선택 페이지가 무엇을 고르는 중인지. */
@Stable
class RouteFlowState {
    var isPlannerOpen by mutableStateOf(false)
    var picking by mutableStateOf<EndpointRole?>(null)
}

@Composable
fun rememberRouteFlowState(): RouteFlowState = remember { RouteFlowState() }

/**
 * 길찾기 흐름의 전체 화면들. MapRoute가 지도 화면 위에 덮는다(아래에서 위 순서).
 * - 경로 목록(RouteListPage): 길찾기 중이고 지도에 경로를 보이지 않는 단계(대중교통). 경로를 고르면 사라진다.
 * - 준비 화면(RoutePlannerPage): 검색창 옆 길찾기 버튼. 두 지점이 정해지면 닫고 [RouteViewModel.plan].
 * - 출발지·도착지 선택(EndpointPickerPage): 위 두 화면 또는 지도 위 출발·도착 카드에서 연다.
 *   준비 화면이 열려 있으면 그 화면에, 아니면 진행 중인 길찾기에 고른 지점을 넣는다.
 * 뒤로 가기는 맨 위 화면부터 닫는다(나중에 등록된 BackHandler가 먼저 받는다).
 */
@Composable
fun RouteFlowOverlays(
    flow: RouteFlowState,
    route: RouteUiState,
    routeViewModel: RouteViewModel,
    plannerViewModel: RoutePlannerViewModel,
    endpointViewModel: EndpointSearchViewModel,
    myLocation: GeoPoint?,
    onError: (DomainError) -> Unit,
) {
    val planner by plannerViewModel.uiState.collectAsStateWithLifecycle()
    val endpoint by endpointViewModel.uiState.collectAsStateWithLifecycle()

    // 효과 수집은 한 번만 시작한다. 그 안에서 처음 값(위치를 아직 모를 때 null)이 아니라 지금 내 위치를 읽게 한다.
    val currentMyLocation by rememberUpdatedState(myLocation)
    LaunchedEffect(plannerViewModel) {
        plannerViewModel.effects.collect { effect ->
            when (effect) {
                is RoutePlannerEffect.Completed -> {
                    flow.isPlannerOpen = false
                    routeViewModel.plan(effect.plan, currentMyLocation)
                }
            }
        }
    }
    LaunchedEffect(endpointViewModel) { endpointViewModel.errors.collect(onError) }

    fun openPicker(role: EndpointRole) {
        endpointViewModel.open()
        flow.picking = role
    }

    fun pick(role: EndpointRole, endpoint: RouteEndpoint) {
        flow.picking = null
        if (endpoint is RouteEndpoint.PlaceEndpoint) endpointViewModel.remember(endpoint.place)
        when {
            flow.isPlannerOpen && role == EndpointRole.ORIGIN -> plannerViewModel.setOrigin(endpoint)
            flow.isPlannerOpen -> plannerViewModel.setDestination(endpoint)
            role == EndpointRole.ORIGIN -> routeViewModel.changeOrigin(endpoint, myLocation)
            else -> routeViewModel.changeDestination(endpoint, myLocation)
        }
    }

    BackHandler(enabled = route.isShowingList && !flow.isPlannerOpen, onBack = routeViewModel::close)
    AnimatedVisibility(visible = route.isShowingList && !flow.isPlannerOpen, enter = fadeIn(), exit = fadeOut()) {
        RouteListPage(
            route = route,
            onTapOrigin = { openPicker(EndpointRole.ORIGIN) },
            onTapDestination = { openPicker(EndpointRole.DESTINATION) },
            onSwap = { routeViewModel.swap(myLocation) },
            onClose = routeViewModel::close,
            onSelectMode = routeViewModel::selectMode,
            onSelectRoute = routeViewModel::showRoute,
        )
    }

    BackHandler(enabled = flow.isPlannerOpen) { flow.isPlannerOpen = false }
    AnimatedVisibility(visible = flow.isPlannerOpen, enter = fadeIn(), exit = fadeOut()) {
        RoutePlannerPage(
            state = planner,
            onTapOrigin = { openPicker(EndpointRole.ORIGIN) },
            onTapDestination = { openPicker(EndpointRole.DESTINATION) },
            onSwap = plannerViewModel::swap,
            onClose = { flow.isPlannerOpen = false },
            onSelectMode = plannerViewModel::selectMode,
            onRecentClick = plannerViewModel::chooseRecent,
            onRecentRemove = plannerViewModel::removeRecent,
        )
    }

    BackHandler(enabled = flow.picking != null) { flow.picking = null }
    AnimatedVisibility(
        visible = flow.picking != null,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it },
    ) {
        // 닫히는 애니메이션 동안에도 제목을 유지하도록 마지막 대상을 기억한다.
        val role = remember { flow.picking ?: EndpointRole.DESTINATION }
        EndpointPickerPage(
            title = stringResource(if (role == EndpointRole.ORIGIN) R.string.route_picker_origin else R.string.route_picker_destination),
            state = endpoint,
            myLocation = myLocation,
            onQueryChange = { endpointViewModel.onQueryChange(it, myLocation) },
            onSubmit = { endpointViewModel.submit(myLocation) },
            onRecentClick = { endpointViewModel.searchRecent(it, myLocation) },
            onRecentRemove = endpointViewModel::removeRecent,
            onPick = { pick(role, it) },
            onBack = { flow.picking = null },
        )
    }
}
