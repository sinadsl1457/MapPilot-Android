package com.example.simpletracking.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * viewModelScope는 Dispatchers.Main(안드로이드 메인 스레드)에서 돈다.
 * JVM 테스트에는 메인 스레드가 없으므로 테스트용 디스패처로 바꿔 끼운다.
 * (Flutter 테스트에는 없는 준비 과정 - Dart는 싱글 스레드 이벤트 루프라 필요 없었다)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
