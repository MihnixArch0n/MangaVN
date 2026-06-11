package com.example.mybookslibrary.ui.viewmodel

import android.content.Context
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.repository.AuthRepository
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.test.MainDispatcherRule
import com.example.mybookslibrary.ui.util.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()
    private val libraryRepository = mockk<LibraryRepository>(relaxed = true)

    private fun viewModel() = AuthViewModel(repository, libraryRepository)

    @Test
    fun login_inputRong_baoLoiVaKhongGoiRepository() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val vm = viewModel()

            vm.login("", "pass")
            advanceUntilIdle()

            assertEquals(AuthState.Error(UiText.Resource(R.string.auth_error_empty_fields)), vm.uiState.value)
            coVerify(exactly = 0) { repository.signInWithEmail(any(), any()) }
        }

    @Test
    fun login_thanhCong_phatSuccess() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            coEvery { repository.signInWithEmail("u", "p") } returns
                Result.success(mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true))
            val vm = viewModel()

            vm.login("u", "p")
            advanceUntilIdle()

            assertEquals(AuthState.Success, vm.uiState.value)
        }

    @Test
    fun login_thatBaiCoMessage_phatErrorMessage() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            coEvery { repository.signInWithEmail("u", "p") } returns
                Result.failure(IllegalStateException("sai mật khẩu"))
            val vm = viewModel()

            vm.login("u", "p")
            advanceUntilIdle()

            assertEquals(AuthState.Error(UiText.Dynamic("sai mật khẩu")), vm.uiState.value)
        }

    @Test
    fun login_thatBaiNullMessage_dungFallback() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            coEvery { repository.signInWithEmail("u", "p") } returns Result.failure(RuntimeException())
            val vm = viewModel()

            vm.login("u", "p")
            advanceUntilIdle()

            assertEquals(AuthState.Error(UiText.Resource(R.string.error_unexpected)), vm.uiState.value)
        }

    @Test
    fun login_dangLoading_boQuaLanGoiThuHai() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            coEvery { repository.signInWithEmail("u", "p") } returns
                Result.success(mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true))
            val vm = viewModel()

            // Lần 1 set Loading (chưa advance); lần 2 trúng guard `is Loading -> return`
            vm.login("u", "p")
            vm.login("u", "p")
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.signInWithEmail("u", "p") }
        }

    @Test
    fun login_blankPassword_baoLoi() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val vm = viewModel()

            // username hợp lệ + password rỗng -> nhánh phải của `||`
            vm.login("user", "")
            advanceUntilIdle()

            assertEquals(AuthState.Error(UiText.Resource(R.string.auth_error_empty_fields)), vm.uiState.value)
        }

    @Test
    fun register_inputRong_baoLoi() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val vm = viewModel()

            vm.register("user", "")
            advanceUntilIdle()

            assertEquals(AuthState.Error(UiText.Resource(R.string.auth_error_empty_fields)), vm.uiState.value)
            coVerify(exactly = 0) { repository.registerWithEmail(any(), any()) }
        }

    @Test
    fun register_thanhCong_phatSuccess() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            coEvery { repository.registerWithEmail("u", "p") } returns
                Result.success(mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true))
            val vm = viewModel()

            vm.register("u", "p")
            advanceUntilIdle()

            assertEquals(AuthState.Success, vm.uiState.value)
        }

    @Test
    fun register_thatBaiNullMessage_dungFallback() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            coEvery { repository.registerWithEmail("u", "p") } returns Result.failure(RuntimeException())
            val vm = viewModel()

            vm.register("u", "p")
            advanceUntilIdle()

            assertEquals(AuthState.Error(UiText.Resource(R.string.error_unexpected)), vm.uiState.value)
        }

    @Test
    fun register_thatBaiCoMessage_phatErrorMessage() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            coEvery { repository.registerWithEmail("u", "p") } returns
                Result.failure(IllegalStateException("user tồn tại"))
            val vm = viewModel()

            vm.register("u", "p")
            advanceUntilIdle()

            assertEquals(AuthState.Error(UiText.Dynamic("user tồn tại")), vm.uiState.value)
        }

    @Test
    fun googleSignIn_thatBaiCoMessage_phatErrorMessage() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val context = mockk<Context>()
            coEvery { repository.signInWithGoogle(context) } returns
                Result.failure(IllegalStateException("huỷ đăng nhập"))
            val vm = viewModel()

            vm.googleSignIn(context)
            advanceUntilIdle()

            assertEquals(AuthState.Error(UiText.Dynamic("huỷ đăng nhập")), vm.uiState.value)
        }

    @Test
    fun googleSignIn_thanhCong_phatSuccess() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val context = mockk<Context>()
            coEvery { repository.signInWithGoogle(context) } returns
                Result.success(mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true))
            val vm = viewModel()

            vm.googleSignIn(context)
            advanceUntilIdle()

            assertEquals(AuthState.Success, vm.uiState.value)
        }

    @Test
    fun googleSignIn_thatBaiNullMessage_dungFallback() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val context = mockk<Context>()
            coEvery { repository.signInWithGoogle(context) } returns Result.failure(RuntimeException())
            val vm = viewModel()

            vm.googleSignIn(context)
            advanceUntilIdle()

            assertEquals(AuthState.Error(UiText.Resource(R.string.error_unexpected)), vm.uiState.value)
        }

    @Test
    fun register_blankUsername_baoLoi() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val vm = viewModel()

            vm.register("", "p") // username rỗng -> nhánh trái của `||`
            advanceUntilIdle()

            assertEquals(AuthState.Error(UiText.Resource(R.string.auth_error_empty_fields)), vm.uiState.value)
        }

    @Test
    fun register_dangLoading_boQuaLanGoiThuHai() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            coEvery { repository.registerWithEmail("u", "p") } returns
                Result.success(mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true))
            val vm = viewModel()

            vm.register("u", "p")
            vm.register("u", "p") // trúng guard is Loading -> return
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.registerWithEmail("u", "p") }
        }

    @Test
    fun googleSignIn_dangLoading_boQuaLanGoiThuHai() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val context = mockk<Context>()
            coEvery { repository.signInWithGoogle(context) } returns
                Result.success(mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true))
            val vm = viewModel()

            vm.googleSignIn(context)
            vm.googleSignIn(context) // trúng guard is Loading -> return
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.signInWithGoogle(context) }
        }

    @Test
    fun resetState_veIdle() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            coEvery { repository.signInWithEmail("u", "p") } returns
                Result.success(mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true))
            val vm = viewModel()
            vm.login("u", "p")
            advanceUntilIdle()
            assertEquals(AuthState.Success, vm.uiState.value)

            vm.resetState()

            assertTrue(vm.uiState.value is AuthState.Idle)
        }
}
