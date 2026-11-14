package com.example.simpletracking.presentation.map.route

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.simpletracking.domain.model.TravelMode

/**
 * 길찾기 경로 목록(전체 화면, 지도 없음). 네이버 지도의 대중교통 경로 목록처럼 경로를 골라야 지도에 그린다.
 * 경로를 누르거나 자동차·도보 탭을 고르면(경로가 하나라 고를 게 없다) isShowingRoute가 켜져 닫히고 지도에 그 경로가 보인다.
 * ✕·뒤로 가기는 길찾기 종료. 상태는 지도 화면과 같은 RouteViewModel.
 */
@Composable
fun RouteListPage(
    route: RouteUiState,
    onTapOrigin: () -> Unit,
    onTapDestination: () -> Unit,
    onSwap: () -> Unit,
    onClose: () -> Unit,
    onSelectMode: (TravelMode) -> Unit,
    onSelectRoute: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            RouteHeader(
                originName = route.origin?.title(),
                destinationName = route.destination?.title(),
                onTapOrigin = onTapOrigin,
                onTapDestination = onTapDestination,
                onSwap = onSwap,
                onClose = onClose,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            )
            TravelModeTabs(
                selected = route.selectedMode,
                results = route.results,
                onSelect = onSelectMode,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            HorizontalDivider(Modifier.padding(top = 8.dp))
            // 자동차·도보 탭이면 곧 닫히므로(지도로) 대중교통 목록만 그린다.
            if (route.selectedMode == TravelMode.TRANSIT) {
                Column(Modifier.padding(top = 12.dp).navigationBarsPadding()) {
                    TransitRouteListHeader(route.selectedResult)
                    TransitRouteList(route.selectedResult, selectedIndex = null, onSelect = onSelectRoute)
                }
            }
        }
    }
}
