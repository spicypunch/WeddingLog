package kr.jm.weddingledger.feature.settings.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository
import kr.jm.weddingledger.feature.auth.domain.usecase.ResendSignUpVerificationEmailUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResetPasswordUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class AccountSettingsViewModelTest {

    @Test
    fun `blank email shows error on reset request`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForAccountSettings()
        val viewModel = createViewModel(email = " ", repository = repository)

        viewModel.onAction(AccountSettingsAction.OnClickSendPasswordReset)

        assertEquals("이메일 정보를 찾을 수 없습니다.", viewModel.state.value.message)
    }

    @Test
    fun `reset password success shows info message`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForAccountSettings().apply {
            resetPasswordResult = AuthResult.Success(Unit)
        }
        val viewModel = createViewModel(email = "user@weddinglog.app", repository = repository)

        viewModel.onAction(AccountSettingsAction.OnClickSendPasswordReset)
        advanceUntilIdle()

        assertEquals(1, repository.resetPasswordCallCount)
        assertEquals("비밀번호 재설정 메일을 보냈습니다. 메일함을 확인해주세요.", viewModel.state.value.message)
    }

    @Test
    fun `resend verification success shows info message`() = runMainDispatcherTest {
        val repository = FakeAuthRepositoryForAccountSettings().apply {
            resendResult = AuthResult.Success(Unit)
        }
        val viewModel = createViewModel(email = "user@weddinglog.app", repository = repository)

        viewModel.onAction(AccountSettingsAction.OnClickResendVerificationEmail)
        advanceUntilIdle()

        assertEquals(1, repository.resendCallCount)
        assertEquals("인증 메일을 다시 보냈습니다. 메일함을 확인해주세요.", viewModel.state.value.message)
    }
}

private fun createViewModel(
    email: String,
    repository: FakeAuthRepositoryForAccountSettings,
): AccountSettingsViewModel {
    return AccountSettingsViewModel(
        email = email,
        resetPasswordUseCase = ResetPasswordUseCase(repository),
        resendSignUpVerificationEmailUseCase = ResendSignUpVerificationEmailUseCase(repository),
    )
}

private class FakeAuthRepositoryForAccountSettings : AuthRepository {
    override val authState: Flow<AuthState> = flowOf(AuthState.Unauthenticated())

    var resetPasswordResult: AuthResult<Unit> = AuthResult.Success(Unit)
    var resendResult: AuthResult<Unit> = AuthResult.Success(Unit)

    var resetPasswordCallCount: Int = 0
    var resendCallCount: Int = 0

    override suspend fun handleAuthDeepLink(url: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signInWithKakao(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signInWithGoogle(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signInWithApple(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun sendEmailOtp(email: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun verifyEmailOtp(email: String, token: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signUp(email: String, password: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signIn(email: String, password: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun resendSignUpVerificationEmail(email: String): AuthResult<Unit> {
        resendCallCount += 1
        return resendResult
    }

    override suspend fun signOut(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun resetPassword(email: String): AuthResult<Unit> {
        resetPasswordCallCount += 1
        return resetPasswordResult
    }

    override fun isLoggedIn(): Boolean = false
}
