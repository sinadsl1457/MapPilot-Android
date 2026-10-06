package com.example.simpletracking.domain.usecase

import com.example.simpletracking.testutil.FakeLocationRepository
import com.example.simpletracking.testutil.userLocation
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveLocationUseCaseTest {

    private val repository = FakeLocationRepository()
    private val observeLocation = ObserveLocationUseCase(repository)

    @Test
    fun `마지막 위치를 먼저 내보내고 실시간 위치를 이어 붙이며, 부정확한 위치는 버린다`() = runTest {
        val last = userLocation(37.0, 127.0)
        val live = userLocation(37.1, 127.1)
        val inaccurate = userLocation(37.2, 127.2, accuracy = 120f)
        repository.lastLocation = last

        val received = mutableListOf<com.example.simpletracking.domain.model.UserLocation>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { observeLocation().toList(received) }
        repository.locations.emit(inaccurate)
        repository.locations.emit(live)
        job.cancel()

        assertEquals(listOf(last, live), received)
    }
}
