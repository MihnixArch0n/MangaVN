package com.example.mybookslibrary.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Kiểm tra DispatchersModule cung cấp đúng dispatcher và scope.
 * Test thuần Kotlin — không cần Hilt hay Robolectric.
 */
class DispatchersModuleTest {
    @Test
    fun provideIoDispatcher_returnsDispatchersIO() {
        val dispatcher = DispatchersModule.provideIoDispatcher()
        assertSame(Dispatchers.IO, dispatcher)
    }

    @Test
    fun provideApplicationScope_withTestDispatcher_returnsNonNullScope() {
        val testDispatcher = UnconfinedTestDispatcher()
        val scope = DispatchersModule.provideApplicationScope(testDispatcher)
        assertNotNull(scope)
    }

    @Test
    fun provideApplicationScope_withTestDispatcher_scopeIsActive() {
        val testDispatcher = UnconfinedTestDispatcher()
        val scope = DispatchersModule.provideApplicationScope(testDispatcher)
        assert(scope.coroutineContext[kotlinx.coroutines.Job]?.isActive == true)
    }
}
