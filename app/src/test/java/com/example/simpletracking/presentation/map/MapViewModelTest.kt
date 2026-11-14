package com.example.simpletracking.presentation.map

import com.example.simpletracking.R
import com.example.simpletracking.domain.result.DomainError
import com.example.simpletracking.domain.usecase.HasLocationPermissionUseCase
import com.example.simpletracking.domain.usecase.ObserveLocationUseCase
import com.example.simpletracking.testutil.FakeLocationRepository
import com.example.simpletracking.testutil.MainDispatcherRule
import com.example.simpletracking.testutil.userLocation
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val locationRepository = FakeLocationRepository()

    // by lazy: 필드 초기화는 @Rule이 Main 디스패처를 바꾸기 "전에" 실행된다.
    // ViewModel의 init이 viewModelScope.launch를 하므로, 테스트 본문에서 처음 접근할 때 만들어야 한다.
    private val viewModel by lazy {
        MapViewModel(ObserveLocationUseCase(locationRepository), HasLocationPermissionUseCase(locationRepository))
    }

    // backgroundScope의 기본 디스패처(Standard)는 launch를 큐에 쌓아두기만 한다.
    // Unconfined로 바로 실행해 "구독이 시작된 뒤"에 검증하도록 한다.
    private fun TestScope.immediate() = UnconfinedTestDispatcher(testScheduler)

    /** 화면이 uiState를 구독하는 상황을 흉내 낸다(collectAsStateWithLifecycle 대신). */
    private fun TestScope.subscribeUi() = backgroundScope.launch(immediate()) { viewModel.uiState.collect {} }

    private fun TestScope.collectEffects(): List<MapEffect> =
        mutableListOf<MapEffect>().also { list -> backgroundScope.launch(immediate()) { viewModel.effects.toList(list) } }

    @Test
    fun `권한이 없으면 GPS를 구독하지 않는다`() = runTest {
        subscribeUi()

        assertEquals(0, locationRepository.locations.subscriptionCount.value)
        assertNull(viewModel.uiState.value.myLocation)
    }

    @Test
    fun `화면에 처음 돌아왔을 때 권한이 없으면 한 번만 권한을 요청한다`() = runTest {
        locationRepository.permissionGranted = false
        subscribeUi()
        val effects = collectEffects()

        viewModel.onResume()
        viewModel.onResume() // 화면 회전·다른 앱에서 복귀

        assertEquals(listOf(MapEffect.RequestLocationPermission), effects)
    }

    @Test
    fun `화면에 돌아왔을 때 이미 권한이 있으면 바로 위치를 구독한다`() = runTest {
        subscribeUi()
        val effects = collectEffects()

        viewModel.onResume()

        assertTrue(viewModel.uiState.value.hasLocationPermission)
        assertEquals(1, locationRepository.locations.subscriptionCount.value)
        assertEquals(emptyList<MapEffect>(), effects)
    }

    @Test
    fun `권한을 받으면 위치가 상태에 반영되고, 첫 위치로 카메라를 한 번 이동한다`() = runTest {
        subscribeUi()
        val effects = collectEffects()

        viewModel.onLocationPermissionResult(granted = true)
        val first = userLocation(37.5, 127.0)
        val second = userLocation(37.6, 127.1)
        locationRepository.locations.emit(first)
        locationRepository.locations.emit(second)

        assertEquals(second, viewModel.uiState.value.myLocation)
        assertEquals(listOf(MapEffect.MoveCamera(first.point, MapViewModel.ZOOM_MY_LOCATION)), effects)
    }

    @Test
    fun `팝업에서 거부했을 뿐이면 설정 안내를 띄우지 않는다`() = runTest {
        subscribeUi()

        viewModel.onLocationPermissionResult(granted = false, canAskAgain = true)

        assertFalse(viewModel.uiState.value.isSettingsDialogVisible)
    }

    @Test
    fun `다시 묻지 않음 상태로 거부되면 설정 안내를 띄우고, 닫으면 내린다`() = runTest {
        subscribeUi()

        viewModel.onLocationPermissionResult(granted = false, canAskAgain = false)
        assertTrue(viewModel.uiState.value.isSettingsDialogVisible)

        viewModel.onSettingsDialogDismiss()
        assertFalse(viewModel.uiState.value.isSettingsDialogVisible)
    }

    @Test
    fun `화면이 구독을 끊고 5초가 지나면 GPS도 끊긴다`() = runTest {
        val ui = subscribeUi()
        viewModel.onLocationPermissionResult(granted = true)
        locationRepository.locations.emit(userLocation(37.5, 127.0)) // init의 첫 위치 대기를 끝낸다
        assertEquals(1, locationRepository.locations.subscriptionCount.value)

        ui.cancel() // 앱이 백그라운드로 감
        advanceTimeBy(4_000)
        assertEquals("5초 전에는 유지(화면 회전 대비)", 1, locationRepository.locations.subscriptionCount.value)

        advanceTimeBy(1_001)
        assertEquals("5초 후 GPS 해제", 0, locationRepository.locations.subscriptionCount.value)
    }

    @Test
    fun `위치 스트림이 권한 에러로 끝나면 에러를 알리고 권한 상태를 되돌린다`() = runTest {
        locationRepository.failWith = DomainError.LocationPermissionDenied
        subscribeUi()
        val effects = collectEffects()

        viewModel.onLocationPermissionResult(granted = true)

        assertFalse(viewModel.uiState.value.hasLocationPermission)
        assertEquals(listOf(MapEffect.ShowError(DomainError.LocationPermissionDenied)), effects)
    }

    @Test
    fun `권한 없이 내 위치 버튼을 누르면 권한을 요청한다`() = runTest {
        subscribeUi()
        val effects = collectEffects()

        viewModel.onMyLocationClick()

        assertEquals(listOf(MapEffect.RequestLocationPermission), effects)
    }

    @Test
    fun `위치를 아직 모르면 내 위치 버튼이 안내 문구를 띄운다`() = runTest {
        subscribeUi()
        viewModel.onLocationPermissionResult(granted = true)
        val effects = collectEffects()

        viewModel.onMyLocationClick()

        assertEquals(listOf(MapEffect.ShowMessage(R.string.message_locating)), effects)
    }

    @Test
    fun `위치를 알면 내 위치 버튼이 카메라를 옮긴다`() = runTest {
        subscribeUi()
        viewModel.onLocationPermissionResult(granted = true)
        val location = userLocation(37.5, 127.0)
        locationRepository.locations.emit(location)
        val effects = collectEffects()

        viewModel.onMyLocationClick()

        assertEquals(MapEffect.MoveCamera(location.point, MapViewModel.ZOOM_MY_LOCATION), effects.last())
    }

    // region 따라가기 (Flutter map_notifier_test · iOS MapViewModelTests와 같은 경우)

    /** 권한이 있고 첫 위치를 받아 카메라가 내 위치에 있는 상태. */
    private fun TestScope.onMyLocation() {
        subscribeUi()
        viewModel.onResume()
        runCurrentLocation()
    }

    private fun TestScope.runCurrentLocation() {
        backgroundScope.launch(immediate()) { locationRepository.locations.emit(userLocation(37.5, 127.0)) }
    }

    private fun TestScope.following(mode: FollowMode) {
        onMyLocation()
        while (viewModel.uiState.value.followMode != mode) viewModel.onMyLocationClick()
    }

    @Test
    fun `카메라가 내 위치면 누를 때마다 따라가기, 지도 회전, 해제 순으로 바뀐다`() = runTest {
        onMyLocation()
        val modes = (1..4).map {
            viewModel.onMyLocationClick()
            viewModel.uiState.value.followMode
        }

        assertEquals(listOf(FollowMode.FOLLOW, FollowMode.FOLLOW_WITH_HEADING, FollowMode.NONE, FollowMode.FOLLOW), modes)
    }

    @Test
    fun `지도를 옮긴 뒤 누르면 카메라만 옮기고, 한 번 더 누르면 따라가기를 켠다`() = runTest {
        onMyLocation()
        viewModel.onMapPanned()
        assertFalse(viewModel.uiState.value.isCameraOnMyLocation)

        viewModel.onMyLocationClick()
        assertEquals(FollowMode.NONE, viewModel.uiState.value.followMode)
        assertTrue(viewModel.uiState.value.isCameraOnMyLocation)

        viewModel.onMyLocationClick()
        assertEquals(FollowMode.FOLLOW, viewModel.uiState.value.followMode)
    }

    @Test
    fun `위치를 모르면 따라가기를 켜지 않는다`() = runTest {
        subscribeUi()
        viewModel.onResume()

        viewModel.onMyLocationClick()
        viewModel.onMyLocationClick()

        assertEquals(FollowMode.NONE, viewModel.uiState.value.followMode)
    }

    @Test
    fun `지도를 드래그하면 따라가기를 끈다`() = runTest {
        following(FollowMode.FOLLOW_WITH_HEADING)

        viewModel.onMapPanned()

        assertEquals(FollowMode.NONE, viewModel.uiState.value.followMode)
    }

    @Test
    fun `지도를 돌리면 회전만 풀고 따라가기는 유지한다`() = runTest {
        following(FollowMode.FOLLOW_WITH_HEADING)

        viewModel.onMapRotated()

        assertEquals(FollowMode.FOLLOW, viewModel.uiState.value.followMode)
    }

    @Test
    fun `다른 기능이 카메라를 옮기면 끈다`() = runTest {
        following(FollowMode.FOLLOW)

        viewModel.stopFollowing()

        assertEquals(FollowMode.NONE, viewModel.uiState.value.followMode)
        assertFalse(viewModel.uiState.value.isCameraOnMyLocation)
    }

    @Test
    fun `위치 권한을 잃으면 끈다`() = runTest {
        following(FollowMode.FOLLOW)

        viewModel.onLocationPermissionResult(granted = false)

        assertEquals(FollowMode.NONE, viewModel.uiState.value.followMode)
    }

    // endregion
}
