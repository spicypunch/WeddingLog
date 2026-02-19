package kr.jm.weddingledger.feature.auth.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.model.AuthUser
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository
import kr.jm.weddingledger.feature.auth.domain.usecase.ResendSignUpVerificationEmailUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResetPasswordUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignUpUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class AuthViewModelTest {

    @Test
    fun `sign in email verification error shows verification guide`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForViewModel().apply {
            signInResult = AuthResult.Error("email confirm required")
        }
        val viewModel = createViewModel(repository)

        viewModel.onAction(AuthAction.OnEmailChange("user@weddinglog.app"))
        viewModel.onAction(AuthAction.OnPasswordChange("password"))
        viewModel.onAction(AuthAction.OnClickSubmit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isEmailVerificationRequired)
        assertIs<AuthMessage.Info>(state.message)
    }

    @Test
    fun `resend verification success starts cooldown and increments attempt`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForViewModel().apply {
            signInResult = AuthResult.Error("email confirm required")
            resendResult = AuthResult.Success(Unit)
        }
        val viewModel = createViewModel(repository)

        viewModel.onAction(AuthAction.OnEmailChange("user@weddinglog.app"))
        viewModel.onAction(AuthAction.OnPasswordChange("password"))
        viewModel.onAction(AuthAction.OnClickSubmit)
        advanceUntilIdle()

        viewModel.onAction(AuthAction.OnClickResendVerificationEmail)
        runCurrent()

        var state = viewModel.state.value
        assertEquals(1, repository.resendCallCount)
        assertEquals(1, state.resendAttemptCount)
        assertEquals(60, state.resendCooldownSeconds)
        assertIs<AuthMessage.Info>(state.message)

        advanceTimeBy(1_000)
        runCurrent()

        state = viewModel.state.value
        assertEquals(59, state.resendCooldownSeconds)

        // ViewModel 내부 cooldown job이 테스트 종료 후에도 남지 않도록 소진합니다.
        advanceTimeBy(59_000)
        runCurrent()

        state = viewModel.state.value
        assertEquals(0, state.resendCooldownSeconds)
    }

    @Test
    fun `sign up success switches to sign in mode with info message`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForViewModel().apply {
            signUpResult = AuthResult.Success(Unit)
        }
        val viewModel = createViewModel(repository)

        viewModel.onAction(AuthAction.OnClickToggleMode)
        viewModel.onAction(AuthAction.OnEmailChange("new@weddinglog.app"))
        viewModel.onAction(AuthAction.OnPasswordChange("123456"))
        viewModel.onAction(AuthAction.OnPasswordConfirmChange("123456"))
        viewModel.onAction(AuthAction.OnTermsAcceptedChange(true))
        viewModel.onAction(AuthAction.OnClickSubmit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, repository.signUpCallCount)
        assertEquals(AuthMode.SignIn, state.mode)
        assertEquals("", state.password)
        assertIs<AuthMessage.Info>(state.message)
    }

    @Test
    fun `reset password success shows info message`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForViewModel().apply {
            resetPasswordResult = AuthResult.Success(Unit)
        }
        val viewModel = createViewModel(repository)

        viewModel.onAction(AuthAction.OnEmailChange("user@weddinglog.app"))
        viewModel.onAction(AuthAction.OnClickResetPassword)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, repository.resetPasswordCallCount)
        assertIs<AuthMessage.Info>(state.message)
    }
}

private fun createViewModel(repository: FakeAuthRepositoryForViewModel): AuthViewModel {
    return AuthViewModel(
        signInUseCase = SignInUseCase(repository),
        signUpUseCase = SignUpUseCase(repository),
        resetPasswordUseCase = ResetPasswordUseCase(repository),
        resendSignUpVerificationEmailUseCase = ResendSignUpVerificationEmailUseCase(repository),
    )
}

private class FakeAuthRepositoryForViewModel : AuthRepository {
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated())

    var signInResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var signUpResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var resendResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var resetPasswordResult: AuthResult<Unit> = AuthResult.Success(Unit)

    var signUpCallCount: Int = 0
    var resendCallCount: Int = 0
    var resetPasswordCallCount: Int = 0

    override val authState: Flow<AuthState> = authStateFlow

    override suspend fun handleAuthDeepLink(url: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signUp(email: String, password: String): AuthResult<Unit> {
        signUpCallCount += 1
        return signUpResult
    }

    override suspend fun signIn(email: String, password: String): AuthResult<Unit> {
        return signInResult
    }

    override suspend fun resendSignUpVerificationEmail(email: String): AuthResult<Unit> {
        resendCallCount += 1
        return resendResult
    }

    override suspend fun signOut(): AuthResult<Unit> {
        authStateFlow.value = AuthState.Unauthenticated()
        return AuthResult.Success(Unit)
    }

    override suspend fun resetPassword(email: String): AuthResult<Unit> {
        resetPasswordCallCount += 1
        return resetPasswordResult
    }

    override fun isLoggedIn(): Boolean = authStateFlow.value is AuthState.Authenticated
}
