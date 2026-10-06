package com.example.simpletracking.presentation.map

import com.example.simpletracking.domain.result.DomainError
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
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val locationRepository = FakeLocationRepository()

    // by lazy: 필드 초기화는 @Rule이 Main 디스패처를 바꾸기 "전에" 실행된다.
    // ViewModel의 init이 viewModelScope.launch를 하므로, 테스트 본문에서 처음 접근할 때 만들어야 한다.
    private val viewModel by lazy { MapViewModel(ObserveLocationUseCase(locationRepository)) }

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
    fun `권한을 받으면 위치가 상태에 반영되고, 첫 위치로 카메라를 한 번 이동한다`() = runTest {
        subscribeUi()
        val effects = collectEffects()

        viewModel.onLocationPermissionResult(granted = true)
        val first = userLocation(37.5, 127.0)
        val second = userLocation(37.6, 127.1)
        locationRepository.locations.emit(first)
        locationRepository.locations.emit(second)

        assertEquals(second, viewModel.uiState.value.myLocation)
        assertEquals(listOf(MapEffect.MoveCamera(first.point, MapViewModel.ZOOM_STREET)), effects)
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
}
