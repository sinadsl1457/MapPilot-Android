package com.example.simpletracking.presentation.map

import com.example.simpletracking.presentation.map.search.titleRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import com.example.simpletracking.presentation.map.route.title
import com.example.simpletracking.presentation.map.route.rememberRouteFlowState
import com.example.simpletracking.presentation.map.route.RouteShownBar
import com.example.simpletracking.presentation.map.route.RoutePlannerViewModel
import com.example.simpletracking.presentation.map.route.RouteFlowOverlays
import com.example.simpletracking.presentation.map.route.EndpointSearchViewModel
import com.example.simpletracking.presentation.map.route.EndpointRole
import com.example.simpletracking.presentation.map.route.DirectionsButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.Place
import com.example.simpletracking.domain.model.PlaceCategory
import com.example.simpletracking.domain.model.TravelMode
import com.example.simpletracking.presentation.common.LOCATION_PERMISSIONS
import com.example.simpletracking.presentation.common.MapBottomSheet
import com.example.simpletracking.presentation.common.SHEET_MEDIUM_FRACTION
import com.example.simpletracking.presentation.common.SheetDetent
import com.example.simpletracking.presentation.common.messageRes
import com.example.simpletracking.presentation.map.kakao.DEFAULT_MAP_CENTER
import com.example.simpletracking.presentation.map.kakao.KakaoMapState
import com.example.simpletracking.presentation.map.kakao.KakaoMapView
import com.example.simpletracking.presentation.map.kakao.rememberKakaoMapState
import com.example.simpletracking.presentation.map.route.RouteEffect
import com.example.simpletracking.presentation.map.route.RouteHeader
import com.example.simpletracking.presentation.map.route.RouteSummaryCard
import com.example.simpletracking.presentation.map.route.RouteUiState
import com.example.simpletracking.presentation.map.route.RouteViewModel
import com.example.simpletracking.presentation.map.route.TransitRouteList
import com.example.simpletracking.presentation.map.route.TransitRouteListHeader
import com.example.simpletracking.presentation.map.route.TravelModeTabs
import com.example.simpletracking.presentation.map.search.CategoryChips
import com.example.simpletracking.presentation.map.search.MapSearchBar
import com.example.simpletracking.presentation.map.search.PlaceResultsContent
import com.example.simpletracking.presentation.map.search.PlaceResultsHeader
import com.example.simpletracking.presentation.map.search.PlaceSearchEffect
import com.example.simpletracking.presentation.map.search.PlaceSearchUiState
import com.example.simpletracking.presentation.map.search.PlaceSearchViewModel
import com.example.simpletracking.presentation.map.search.SearchPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 검색 결과·내 위치로 카메라를 옮길 때의 줌 레벨 */
private const val ZOOM_PLACE = 17

/** 경로 카드가 새 경로로 다시 그려질 때까지 기다리는 시간 */
private const val ROUTE_CARD_SETTLE_MILLIS = 150L

/**
 * 지도를 가리는 UI의 지금 크기(dp). MapScreen이 그릴 때 재고, 카메라를 맞출 때 MapRoute가 읽는다.
 * 다시 그릴 필요가 없는 값이라 State가 아니다.
 */
class MapChromeMetrics {
    /** 화면 위에서 상단 UI(검색창·칩 또는 길찾기 카드·탭) 아래 끝까지 */
    var topCoveredDp = 0f
    var screenHeightDp = 0f
    /** 화면 아래에서 경로 카드 윗변까지 */
    var routeCardHeightDp = 0f

    /** 지점이 위 UI와 아래 UI([bottomDp]) 사이에 들어오게 하는 여백 */
    fun insets(bottomDp: Float) = MapInsets(topDp = topCoveredDp, bottomDp = bottomDp)

    /** 중간 높이로 멈춘 하단 시트가 가리는 높이. 시트는 펼쳐지는 중일 수 있어 지금 높이가 아니라 멈출 높이를 쓴다. */
    val mediumSheetDp: Float get() = screenHeightDp * SHEET_MEDIUM_FRACTION
}

/** 길찾기 시작 전 카메라(중심·줌)와 직전 길찾기 여부. 다시 그릴 필요가 없는 값이라 State가 아니다. */
private class RouteCameraMemory {
    var wasActive = false
    var before: Pair<GeoPoint, Int>? = null
}

/**
 * Route = BlocProvider + BlocListener 역할.
 * ViewModel을 꺼내 서로 잇고, 권한·이펙트처럼 Android 플랫폼(Activity)이 필요한 일을 처리한다.
 */
@Composable
fun MapRoute(
    mapViewModel: MapViewModel = hiltViewModel(),
    searchViewModel: PlaceSearchViewModel = hiltViewModel(),
    routeViewModel: RouteViewModel = hiltViewModel(),
    plannerViewModel: RoutePlannerViewModel = hiltViewModel(),
    endpointViewModel: EndpointSearchViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle: 앱이 백그라운드(STOPPED)면 구독을 멈춘다 → ViewModel의 WhileSubscribed 5초 타이머가 돈다.
    val mapUi by mapViewModel.uiState.collectAsStateWithLifecycle()
    val search by searchViewModel.uiState.collectAsStateWithLifecycle()
    val route by routeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 설정 변경(언어 등) 후에도 최신 문자열을 읽도록 Context 대신 Resources를 쓴다.
    val resources = LocalResources.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val mapState = rememberKakaoMapState()
    val chrome = remember { MapChromeMetrics() }
    val routeFlow = rememberRouteFlowState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        // 거부 직후 이 값이 false면 "다시 묻지 않음" 상태다. 이때는 팝업이 다시 뜨지 않으므로 설정 앱으로 안내한다.
        val canAskAgain = activity == null ||
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        mapViewModel.onLocationPermissionResult(granted, canAskAgain)
    }

    // 화면에 돌아올 때마다(설정 앱에서 권한을 켜고 돌아온 경우 포함) 권한 상태를 다시 확인한다.
    LifecycleResumeEffect(Unit) {
        mapViewModel.onResume()
        onPauseOrDispose { }
    }

    // 상태 → 지도(명령형) 반영
    LaunchedEffect(mapUi.myLocation) { mapState.updateMyLocation(mapUi.myLocation) }
    // 따라가기 모드를 지도에 알린다. 카메라 추적은 SDK 트래킹이 한다.
    LaunchedEffect(mapUi.followMode) { mapState.setFollowMode(mapUi.followMode) }
    val markers = remember(search.places) {
        search.places.mapIndexed { index, place -> PlaceMarker(place.id, place.location, number = index + 1) }
    }
    LaunchedEffect(markers, search.selectedPlace?.id, route.isActive) {
        // 길찾기 중에는 검색 결과 마커를 숨기고 출발·도착 마커만 보인다.
        if (route.isActive) mapState.showPlaceMarkers(emptyList(), null)
        else mapState.showPlaceMarkers(markers, search.selectedPlace?.id)
    }
    val overlay = route.overlay
    LaunchedEffect(overlay) { mapState.showRoute(overlay) }
    // 길찾기를 시작하면(경로에 맞춰 줌 아웃하기 전) 지금 카메라를 기억해 두고, 끝나면 그 위치·줌으로 되돌린다.
    val routeCamera = remember { RouteCameraMemory() }
    LaunchedEffect(route.isActive) {
        if (route.isActive == routeCamera.wasActive) return@LaunchedEffect // 처음 그릴 때·화면 회전
        routeCamera.wasActive = route.isActive
        if (route.isActive) {
            routeCamera.before = mapState.cameraCenter?.let { center -> mapState.cameraZoom?.let { center to it } }
        } else {
            // 길찾기 중에 따라가기를 켰더라도 끈다. 켜 두면 복원한 카메라를 트래킹이 다시 끌고 간다.
            mapViewModel.stopFollowing()
            val before = routeCamera.before
            routeCamera.before = null
            when {
                before != null -> mapState.moveCamera(before.first, before.second)
                search.selectedPlace != null -> mapState.moveCamera(search.selectedPlace!!.location, ZOOM_PLACE)
            }
        }
    }
    DisposableEffect(mapState, searchViewModel) {
        mapState.onPlaceClick = searchViewModel::selectPlace
        onDispose { mapState.onPlaceClick = null }
    }
    DisposableEffect(mapState, mapViewModel) {
        mapState.onUserGesture = { gesture ->
            when (gesture) {
                MapGesture.PAN -> mapViewModel.onMapPanned()
                MapGesture.ROTATE -> mapViewModel.onMapRotated()
            }
        }
        onDispose { mapState.onUserGesture = null }
    }

    /** 내 위치가 아닌 곳(검색 결과, 경로)으로 카메라를 옮긴다. 따라가기를 먼저 꺼야 트래킹이 카메라를 내 위치로 되돌리지 않는다. */
    fun cameraAwayFromMe(move: () -> Unit) {
        mapViewModel.stopFollowing()
        move()
    }

    // 일회성 이펙트 처리(BlocListener). STARTED 이상일 때만 수집한다.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // showSnackbar는 스낵바가 사라질 때까지 suspend된다.
            // 여기서 바로 기다리면 그동안 다음 이펙트(카메라 이동 등)가 막히므로 별도 코루틴에서 띄운다.
            fun showMessage(text: String) {
                launch { snackbarHostState.showSnackbar(text) }
            }
            launch {
                mapViewModel.effects.collect { effect ->
                    when (effect) {
                        // 내 위치(첫 위치, 내 위치 버튼)는 애니메이션 없이 바로 놓는다.
                        // 멀리서 날아오면 어색하고, 긴 이동 애니메이션은 도중에 끊겨 엉뚱한 곳에 멈출 수 있다.
                        is MapEffect.MoveCamera -> mapState.moveCamera(effect.target, effect.zoomLevel, animated = false)
                        is MapEffect.ShowError -> showMessage(resources.getString(effect.error.messageRes()))
                        is MapEffect.ShowMessage -> showMessage(resources.getString(effect.messageRes))
                        MapEffect.RequestLocationPermission -> permissionLauncher.launch(LOCATION_PERMISSIONS)
                    }
                }
            }
            launch {
                searchViewModel.effects.collect { effect ->
                    when (effect) {
                        is PlaceSearchEffect.FitCamera -> cameraAwayFromMe {
                            mapState.fitCamera(effect.points, chrome.insets(bottomDp = chrome.mediumSheetDp))
                        }
                        is PlaceSearchEffect.Focus -> cameraAwayFromMe { mapState.moveCamera(effect.point, ZOOM_PLACE) }
                        is PlaceSearchEffect.ShowError -> showMessage(resources.getString(effect.error.messageRes()))
                    }
                }
            }
            launch {
                routeViewModel.effects.collect { effect ->
                    when (effect) {
                        is RouteEffect.FitCamera -> {
                            // 아래 경로 카드는 이 effect와 같은 상태 변경으로 이제 막 바뀐다(목록이 닫히는 중일 수도 있다).
                            // 다시 그려진 뒤의 실제 높이로 맞춘다.
                            launch {
                                delay(ROUTE_CARD_SETTLE_MILLIS)
                                cameraAwayFromMe { mapState.fitCamera(effect.points, chrome.insets(bottomDp = chrome.routeCardHeightDp)) }
                            }
                        }
                        is RouteEffect.ShowError -> showMessage(resources.getString(effect.error.messageRes()))
                        is RouteEffect.ShowMessage -> showMessage(resources.getString(effect.messageRes))
                    }
                }
            }
        }
    }

    if (mapUi.isSettingsDialogVisible) {
        AlertDialog(
            onDismissRequest = mapViewModel::onSettingsDialogDismiss,
            title = { Text(stringResource(R.string.permission_dialog_title)) },
            text = { Text(stringResource(R.string.permission_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    mapViewModel.onSettingsDialogDismiss()
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                    )
                }) { Text(stringResource(R.string.action_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = mapViewModel::onSettingsDialogDismiss) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    val myPoint = mapUi.myLocation?.point
    MapScreen(
        mapUi = mapUi,
        search = search,
        route = route,
        mapState = mapState,
        chrome = chrome,
        snackbarHostState = snackbarHostState,
        actions = MapScreenActions(
            onMyLocationClick = mapViewModel::onMyLocationClick,
            onSearchBarClick = { searchViewModel.openSearchPage(center = myPoint) },
            onClearResults = searchViewModel::clearResults,
            onCategoryClick = { category ->
                // 카테고리 검색 기준: 지금 보고 있는 지도 중심 → 내 위치 → 기본 위치
                searchViewModel.toggleCategory(category, mapState.cameraCenter ?: myPoint ?: DEFAULT_MAP_CENTER)
            },
            onQueryChange = searchViewModel::onQueryChange,
            onSuggestionClick = searchViewModel::selectSuggestion,
            onSubmit = { searchViewModel.submit(myPoint) },
            onRecentClick = { searchViewModel.searchRecent(it, myPoint) },
            onRecentRemove = searchViewModel::removeRecent,
            onClearRecents = searchViewModel::clearRecents,
            onCloseSearchPage = searchViewModel::closeSearchPage,
            onSelectPlace = searchViewModel::select,
            onBackToList = searchViewModel::deselectPlace,
            onStartRoute = { place ->
                // 권한이 없으면 길찾기 대신 권한부터 요청한다.
                if (!mapUi.hasLocationPermission) mapViewModel.onMyLocationClick()
                else routeViewModel.start(place, myPoint)
            },
            onCloseRoute = routeViewModel::close,
            onSelectMode = routeViewModel::selectMode,
            onOpenPlanner = {
                plannerViewModel.open(route.selectedMode)
                routeFlow.isPlannerOpen = true
            },
            onTapRouteOrigin = {
                endpointViewModel.open()
                routeFlow.picking = EndpointRole.ORIGIN
            },
            onTapRouteDestination = {
                endpointViewModel.open()
                routeFlow.picking = EndpointRole.DESTINATION
            },
            onSwapRoute = { routeViewModel.swap(myPoint) },
            onBackToRouteList = routeViewModel::backToList,
        ),
        overlays = {
            RouteFlowOverlays(
                flow = routeFlow,
                route = route,
                routeViewModel = routeViewModel,
                plannerViewModel = plannerViewModel,
                endpointViewModel = endpointViewModel,
                myLocation = myPoint,
                onError = { error -> scope.launch { snackbarHostState.showSnackbar(resources.getString(error.messageRes())) } },
            )
        },
    )
}

/** 화면에서 올라오는 사용자 입력. MapScreen이 ViewModel을 모르게 하려고 콜백으로 묶는다. */
class MapScreenActions(
    val onMyLocationClick: () -> Unit,
    val onSearchBarClick: () -> Unit,
    val onClearResults: () -> Unit,
    val onCategoryClick: (PlaceCategory) -> Unit,
    val onQueryChange: (String) -> Unit,
    val onSuggestionClick: (Place) -> Unit,
    val onSubmit: () -> Unit,
    val onRecentClick: (String) -> Unit,
    val onRecentRemove: (String) -> Unit,
    val onClearRecents: () -> Unit,
    val onCloseSearchPage: () -> Unit,
    val onSelectPlace: (Place) -> Unit,
    val onBackToList: () -> Unit,
    val onStartRoute: (Place) -> Unit,
    val onCloseRoute: () -> Unit,
    val onSelectMode: (TravelMode) -> Unit,
    /** 검색창 옆 길찾기 버튼 */
    val onOpenPlanner: () -> Unit,
    /** 자동차·도보 경로를 보는 중 지도 위 출발·도착 카드의 줄 탭 */
    val onTapRouteOrigin: () -> Unit,
    val onTapRouteDestination: () -> Unit,
    val onSwapRoute: () -> Unit,
    /** 대중교통 경로를 보는 중 ‹ */
    val onBackToRouteList: () -> Unit,
)

/**
 * Screen = BlocBuilder의 builder. 상태를 받아 그리기만 하고, 사용자 입력은 [actions]로 올려보낸다.
 * ViewModel을 모르므로 가짜 상태만으로 UI 테스트를 할 수 있다.
 */
@Composable
fun MapScreen(
    mapUi: MapUiState,
    search: PlaceSearchUiState,
    route: RouteUiState,
    mapState: KakaoMapState,
    snackbarHostState: SnackbarHostState,
    actions: MapScreenActions,
    chrome: MapChromeMetrics = remember { MapChromeMetrics() },
    /** 지도 화면 위에 덮는 전체 화면들(길찾기 준비·경로 목록·출발지 선택). 맨 위에 그린다. */
    overlays: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    var sheetDetent by rememberSaveable { mutableStateOf(SheetDetent.MEDIUM) }
    var sheetHeight by remember { mutableStateOf(0.dp) }
    var routeCardHeight by remember { mutableStateOf(0.dp) }
    var screenHeight by remember { mutableStateOf(0.dp) }
    /** 화면 맨 위부터 검색창 줄 아래 끝까지. 장소 시트를 끝까지 올리면 여기 바로 아래에서 멈춘다. */
    var searchRowBottom by remember { mutableStateOf(0.dp) }

    val isResultsSheetVisible = search.source != null && !route.isActive
    val myPoint: GeoPoint? = mapUi.myLocation?.point

    // 장소를 고르면 지도가 잘 보이도록 시트를 접고, 목록으로 돌아오거나 새로 검색하면 중간 높이로 연다.
    LaunchedEffect(search.selectedPlace == null, search.source) {
        sheetDetent = if (search.selectedPlace == null) SheetDetent.MEDIUM else SheetDetent.COLLAPSED
    }

    // 뒤로 가기: 가장 위에 열린 것부터 하나씩 닫는다.
    BackHandler(enabled = search.isSearchPageVisible, onBack = actions.onCloseSearchPage)
    // 경로를 보던 중: 대중교통은 경로 목록으로, 자동차·도보는(목록이 없으니) 길찾기를 끝낸다. 목록 단계는 RouteFlowOverlays가 받는다.
    BackHandler(enabled = !search.isSearchPageVisible && route.isShowingRoute) {
        if (route.selectedMode == TravelMode.TRANSIT) actions.onBackToRouteList() else actions.onCloseRoute()
    }
    BackHandler(enabled = !search.isSearchPageVisible && !route.isActive && search.selectedPlace != null, onBack = actions.onBackToList)
    BackHandler(
        enabled = !search.isSearchPageVisible && !route.isActive && search.selectedPlace == null && search.source != null,
        onBack = actions.onClearResults,
    )

    // 내 위치 버튼·스낵바는 시트가 있으면 시트 윗변 위에, 없으면 화면 아래에 붙인다.
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val mediumSheetHeight = screenHeight * SHEET_MEDIUM_FRACTION
    /** 시트가 중간보다 올라와 칩·내 위치 버튼 자리를 덮는 중(목록을 보는 중). */
    val isSheetAboveMedium = isResultsSheetVisible && sheetHeight > mediumSheetHeight + 1.dp
    // 시트 위치는 중간 높이까지만 따른다. 시트를 끝까지 올리면 버튼은 숨긴다(화면 위로 밀려 올라가지 않게).
    val floatingBottom: Dp = when {
        route.isShowingRoute -> routeCardHeight + 12.dp
        isResultsSheetVisible -> minOf(sheetHeight, mediumSheetHeight) + 12.dp
        else -> navigationBarBottom + 24.dp
    }

    Box(
        Modifier.fillMaxSize().onSizeChanged {
            chrome.screenHeightDp = it.height / density.density
            screenHeight = with(density) { it.height.toDp() }
        },
    ) {
        // 지도는 상태바·내비게이션바 아래까지 꽉 채운다.
        KakaoMapView(state = mapState, modifier = Modifier.fillMaxSize())

        // 장소 시트는 위 UI보다 먼저(아래에) 그린다. 끝까지 올려도 검색창은 시트 위에 보인다(네이버 지도 방식).
        AnimatedVisibility(
            visible = isResultsSheetVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            MapBottomSheet(
                detent = sheetDetent,
                onDetentChange = { sheetDetent = it },
                onVisibleHeightChange = { sheetHeight = it },
                topReservedHeight = searchRowBottom + 8.dp,
                header = { PlaceResultsHeader(search) },
            ) {
                Column(Modifier.navigationBarsPadding()) {
                    PlaceResultsContent(
                        state = search,
                        myLocation = myPoint,
                        onSelect = actions.onSelectPlace,
                        onBackToList = actions.onBackToList,
                        onRoute = actions.onStartRoute,
                    )
                }
            }
        }

        // 목록을 중간보다 올리면 검색창 뒤(상태 표시줄까지)를 배경색으로 채워 지도가 비치지 않게 한다.
        AnimatedVisibility(visible = isSheetAboveMedium, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxWidth().height(searchRowBottom + 8.dp).background(MaterialTheme.colorScheme.surface))
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp)
                // 카메라를 맞출 때 위쪽 여백: 화면 맨 위부터 이 UI 아래 끝까지
                .onGloballyPositioned { chrome.topCoveredDp = it.boundsInRoot().bottom / density.density },
        ) {
            when {
                // 대중교통 경로를 보는 중: ‹ + "출발 → 도착"(누르면 목록으로)
                route.isShowingRoute && route.selectedMode == TravelMode.TRANSIT -> RouteShownBar(
                    originName = route.origin?.title().orEmpty(),
                    destinationName = route.destination?.title().orEmpty(),
                    onBack = actions.onBackToRouteList,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                // 자동차·도보 경로를 보는 중: 출발·도착 카드(바꾸기·맞바꾸기·✕) + 이동 수단 탭(대중교통을 누르면 목록으로)
                route.isShowingRoute -> {
                    RouteHeader(
                        originName = route.origin?.title(),
                        destinationName = route.destination?.title(),
                        onTapOrigin = actions.onTapRouteOrigin,
                        onTapDestination = actions.onTapRouteDestination,
                        onSwap = actions.onSwapRoute,
                        onClose = actions.onCloseRoute,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TravelModeTabs(
                        selected = route.selectedMode,
                        results = route.results,
                        onSelect = actions.onSelectMode,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .onGloballyPositioned { searchRowBottom = with(density) { it.boundsInRoot().bottom.toDp() } },
                    ) {
                        // 카테고리 결과면 검색창에 카테고리 이름("카페")을 보인다.
                        MapSearchBar(
                            keyword = search.keyword ?: search.selectedCategory?.let { stringResource(it.titleRes) },
                            showsClear = isResultsSheetVisible,
                            onClick = actions.onSearchBarClick,
                            onClear = actions.onClearResults,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        DirectionsButton(onClick = actions.onOpenPlanner)
                    }
                    // 목록이 칩을 덮을 만큼 올라오면 숨긴다(검색창 위에 그려지므로 그대로 두면 목록 위에 뜬다).
                    AnimatedVisibility(visible = !isSheetAboveMedium, enter = fadeIn(), exit = fadeOut()) {
                        CategoryChips(selected = search.selectedCategory, onClick = actions.onCategoryClick)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = route.isShowingRoute,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            RouteSummaryCard(
                mode = route.selectedMode,
                result = route.selectedResult,
                transitIndex = route.selectedTransitIndex,
                modifier = Modifier.onSizeChanged {
                    routeCardHeight = with(density) { it.height.toDp() }
                    chrome.routeCardHeightDp = it.height / density.density
                },
            )
        }

        AnimatedVisibility(
            visible = !isSheetAboveMedium,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = floatingBottom),
        ) {
            MyLocationButton(
                hasPermission = mapUi.hasLocationPermission,
                followMode = mapUi.followMode,
                isCameraOnMyLocation = mapUi.isCameraOnMyLocation,
                onClick = actions.onMyLocationClick,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = floatingBottom + 64.dp),
        )

        AnimatedVisibility(visible = search.isSearchPageVisible, enter = fadeIn(), exit = fadeOut()) {
            SearchPage(
                state = search,
                onQueryChange = actions.onQueryChange,
                onSuggestionClick = actions.onSuggestionClick,
                onSubmit = actions.onSubmit,
                onRecentClick = actions.onRecentClick,
                onRecentRemove = actions.onRecentRemove,
                onClearRecents = actions.onClearRecents,
                onBack = actions.onCloseSearchPage,
            )
        }

        overlays()
    }
}
