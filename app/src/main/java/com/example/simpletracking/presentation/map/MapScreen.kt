package com.example.simpletracking.presentation.map

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
import kotlinx.coroutines.launch

/** 검색 결과·내 위치로 카메라를 옮길 때의 줌 레벨 */
private const val ZOOM_PLACE = 17

/**
 * Route = BlocProvider + BlocListener 역할.
 * ViewModel을 꺼내 서로 잇고, 권한·이펙트처럼 Android 플랫폼(Activity)이 필요한 일을 처리한다.
 */
@Composable
fun MapRoute(
    mapViewModel: MapViewModel = hiltViewModel(),
    searchViewModel: PlaceSearchViewModel = hiltViewModel(),
    routeViewModel: RouteViewModel = hiltViewModel(),
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
    val mapState = rememberKakaoMapState()

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
    DisposableEffect(mapState, searchViewModel) {
        mapState.onPlaceClick = searchViewModel::selectPlace
        onDispose { mapState.onPlaceClick = null }
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
                        is MapEffect.MoveCamera -> mapState.moveCamera(effect.target, effect.zoomLevel)
                        is MapEffect.ShowError -> showMessage(resources.getString(effect.error.messageRes()))
                        is MapEffect.ShowMessage -> showMessage(resources.getString(effect.messageRes))
                        MapEffect.RequestLocationPermission -> permissionLauncher.launch(LOCATION_PERMISSIONS)
                    }
                }
            }
            launch {
                searchViewModel.effects.collect { effect ->
                    when (effect) {
                        is PlaceSearchEffect.FitCamera -> mapState.fitCamera(effect.points, MapFitLayout.SEARCH_RESULTS)
                        is PlaceSearchEffect.Focus -> mapState.moveCamera(effect.point, ZOOM_PLACE)
                        is PlaceSearchEffect.ShowError -> showMessage(resources.getString(effect.error.messageRes()))
                    }
                }
            }
            launch {
                routeViewModel.effects.collect { effect ->
                    when (effect) {
                        is RouteEffect.FitCamera -> {
                            val transit = routeViewModel.uiState.value.selectedMode == TravelMode.TRANSIT
                            mapState.fitCamera(effect.points, if (transit) MapFitLayout.ROUTE_WITH_SHEET else MapFitLayout.ROUTE)
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
            onCloseRoute = {
                routeViewModel.close()
                search.selectedPlace?.let { mapState.moveCamera(it.location, ZOOM_PLACE) }
            },
            onSelectMode = routeViewModel::selectMode,
            onSelectTransitRoute = routeViewModel::selectTransitRoute,
        ),
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
    val onSelectTransitRoute: (Int) -> Unit,
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
) {
    val density = LocalDensity.current
    var sheetDetent by rememberSaveable { mutableStateOf(SheetDetent.MEDIUM) }
    var transitSheetDetent by rememberSaveable { mutableStateOf(SheetDetent.MEDIUM) }
    var sheetHeight by remember { mutableStateOf(0.dp) }
    var routeCardHeight by remember { mutableStateOf(0.dp) }

    val isResultsSheetVisible = search.source != null && !route.isActive
    val isTransitSheetVisible = route.isActive && route.selectedMode == TravelMode.TRANSIT
    val myPoint: GeoPoint? = mapUi.myLocation?.point

    // 장소를 고르면 지도가 잘 보이도록 시트를 접고, 목록으로 돌아오거나 새로 검색하면 중간 높이로 연다.
    LaunchedEffect(search.selectedPlace == null, search.source) {
        sheetDetent = if (search.selectedPlace == null) SheetDetent.MEDIUM else SheetDetent.COLLAPSED
    }
    LaunchedEffect(route.destination) { if (route.destination != null) transitSheetDetent = SheetDetent.MEDIUM }

    // 뒤로 가기: 가장 위에 열린 것부터 하나씩 닫는다.
    BackHandler(enabled = search.isSearchPageVisible, onBack = actions.onCloseSearchPage)
    BackHandler(enabled = !search.isSearchPageVisible && route.isActive, onBack = actions.onCloseRoute)
    BackHandler(enabled = !search.isSearchPageVisible && !route.isActive && search.selectedPlace != null, onBack = actions.onBackToList)
    BackHandler(
        enabled = !search.isSearchPageVisible && !route.isActive && search.selectedPlace == null && search.source != null,
        onBack = actions.onClearResults,
    )

    // 내 위치 버튼·스낵바는 시트가 있으면 시트 윗변 위에, 없으면 화면 아래에 붙인다.
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val floatingBottom: Dp = when {
        isTransitSheetVisible -> sheetHeight + 12.dp
        route.isActive -> routeCardHeight + 12.dp
        isResultsSheetVisible -> sheetHeight + 12.dp
        else -> navigationBarBottom + 24.dp
    }

    Box(Modifier.fillMaxSize()) {
        // 지도는 상태바·내비게이션바 아래까지 꽉 채운다.
        KakaoMapView(state = mapState, modifier = Modifier.fillMaxSize())

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.statusBarsPadding().padding(top = 8.dp),
        ) {
            val destination = route.destination
            if (destination != null) {
                RouteHeader(destination.name, onClose = actions.onCloseRoute, modifier = Modifier.padding(horizontal = 16.dp))
                TravelModeTabs(
                    selected = route.selectedMode,
                    results = route.results,
                    onSelect = actions.onSelectMode,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                MapSearchBar(
                    keyword = search.keyword,
                    showsClear = isResultsSheetVisible,
                    onClick = actions.onSearchBarClick,
                    onClear = actions.onClearResults,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                CategoryChips(selected = search.selectedCategory, onClick = actions.onCategoryClick)
            }
        }

        AnimatedVisibility(
            visible = isResultsSheetVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            MapBottomSheet(
                detent = sheetDetent,
                onDetentChange = { sheetDetent = it },
                onVisibleHeightChange = { sheetHeight = it },
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

        AnimatedVisibility(
            visible = isTransitSheetVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            MapBottomSheet(
                detent = transitSheetDetent,
                onDetentChange = { transitSheetDetent = it },
                onVisibleHeightChange = { sheetHeight = it },
                // 펼쳐도 출발·도착 카드와 이동 수단 탭은 가리지 않는다.
                topReservedHeight = 160.dp,
                header = { TransitRouteListHeader(route.selectedResult) },
            ) {
                Column(Modifier.navigationBarsPadding()) {
                    TransitRouteList(route.selectedResult, route.selectedTransitIndex, actions.onSelectTransitRoute)
                }
            }
        }

        AnimatedVisibility(
            visible = route.isActive && !isTransitSheetVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            RouteSummaryCard(
                mode = route.selectedMode,
                result = route.selectedResult,
                modifier = Modifier.onSizeChanged { routeCardHeight = with(density) { it.height.toDp() } },
            )
        }

        MyLocationButton(
            hasPermission = mapUi.hasLocationPermission,
            onClick = actions.onMyLocationClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = floatingBottom),
        )

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
    }
}
