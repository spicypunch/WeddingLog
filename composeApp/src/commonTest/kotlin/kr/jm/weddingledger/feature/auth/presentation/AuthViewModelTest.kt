package kr.jm.weddingledger.feature.auth.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.model.AuthUser
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository
import kr.jm.weddingledger.feature.auth.domain.usecase.ResendSignUpVerificationEmailUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResetPasswordUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SendEmailOtpUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInWithAppleUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInWithGoogleUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInWithKakaoUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.VerifyEmailOtpUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class AuthViewModelTest {

    @Test
    fun `submit shows sign in disabled message`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForViewModel()
        val viewModel = createViewModel(repository)

        viewModel.onAction(AuthAction.OnEmailChange("user@weddinglog.app"))
        viewModel.onAction(AuthAction.OnPasswordChange("password"))
        viewModel.onAction(AuthAction.OnClickSubmit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<AuthMessage.Error>(state.message)
        assertEquals(
            "비밀번호 로그인은 현재 비활성화되어 있습니다.",
            (state.message as AuthMessage.Error).text,
        )
    }

    @Test
    fun `toggle mode shows sign up disabled message`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForViewModel().apply {
            signInResult = AuthResult.Success(Unit)
        }
        val viewModel = createViewModel(repository)

        viewModel.onAction(AuthAction.OnClickToggleMode)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthMode.SignIn, state.mode)
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

    @Test
    fun `kakao sign in success calls repository and shows info`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForViewModel().apply {
            signInWithKakaoResult = AuthResult.Success(Unit)
        }
        val viewModel = createViewModel(repository)

        viewModel.onAction(AuthAction.OnClickSignInWithKakao)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, repository.signInWithKakaoCallCount)
        assertIs<AuthMessage.Info>(state.message)
    }

    @Test
    fun `google sign in success calls repository and shows info`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForViewModel().apply {
            signInWithGoogleResult = AuthResult.Success(Unit)
        }
        val viewModel = createViewModel(repository)

        viewModel.onAction(AuthAction.OnClickSignInWithGoogle)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, repository.signInWithGoogleCallCount)
        assertIs<AuthMessage.Info>(state.message)
    }

    @Test
    fun `send and verify otp delegates to repository`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForViewModel().apply {
            sendOtpResult = AuthResult.Success(Unit)
            verifyOtpResult = AuthResult.Success(Unit)
        }
        val viewModel = createViewModel(repository)

        viewModel.onAction(AuthAction.OnEmailChange("otp@weddinglog.app"))
        viewModel.onAction(AuthAction.OnClickSendOtp)
        runCurrent()
        assertEquals(1, repository.sendOtpCallCount)

        viewModel.onAction(AuthAction.OnOtpCodeChange("123456"))
        viewModel.onAction(AuthAction.OnClickVerifyOtp)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, repository.verifyOtpCallCount)
        assertEquals("", state.otpCode)
        assertIs<AuthMessage.Info>(state.message)
    }
}

    private fun createViewModel(repository: FakeAuthRepositoryForViewModel): AuthViewModel {
        return AuthViewModel(
            signInUseCase = SignInUseCase(repository),
            signInWithKakaoUseCase = SignInWithKakaoUseCase(repository),
            signInWithGoogleUseCase = SignInWithGoogleUseCase(repository),
            signInWithAppleUseCase = SignInWithAppleUseCase(repository),
            sendEmailOtpUseCase = SendEmailOtpUseCase(repository),
        verifyEmailOtpUseCase = VerifyEmailOtpUseCase(repository),
        resetPasswordUseCase = ResetPasswordUseCase(repository),
        resendSignUpVerificationEmailUseCase = ResendSignUpVerificationEmailUseCase(repository),
    )
}

private class FakeAuthRepositoryForViewModel : AuthRepository {
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated())

    var signInResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var signInWithKakaoResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var signInWithGoogleResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var signInWithAppleResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var sendOtpResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var verifyOtpResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var resendResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var resetPasswordResult: AuthResult<Unit> = AuthResult.Success(Unit)

    var signInWithKakaoCallCount: Int = 0
    var signInWithGoogleCallCount: Int = 0
    var signInWithAppleCallCount: Int = 0
    var sendOtpCallCount: Int = 0
    var verifyOtpCallCount: Int = 0
    var resendCallCount: Int = 0
    var resetPasswordCallCount: Int = 0

    override val authState: Flow<AuthState> = authStateFlow

    override suspend fun handleAuthDeepLink(url: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signUp(email: String, password: String): AuthResult<Unit> {
        return AuthResult.Error("비활성")
    }

    override suspend fun signInWithKakao(): AuthResult<Unit> {
        signInWithKakaoCallCount += 1
        return signInWithKakaoResult
    }

    override suspend fun signInWithGoogle(): AuthResult<Unit> {
        signInWithGoogleCallCount += 1
        return signInWithGoogleResult
    }

    override suspend fun signInWithApple(): AuthResult<Unit> {
        signInWithAppleCallCount += 1
        return signInWithAppleResult
    }

    override suspend fun sendEmailOtp(email: String): AuthResult<Unit> {
        sendOtpCallCount += 1
        return sendOtpResult
    }

    override suspend fun verifyEmailOtp(email: String, token: String): AuthResult<Unit> {
        verifyOtpCallCount += 1
        return verifyOtpResult
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
