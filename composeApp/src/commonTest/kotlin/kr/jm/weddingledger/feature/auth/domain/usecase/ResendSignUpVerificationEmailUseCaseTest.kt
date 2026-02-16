package kr.jm.weddingledger.feature.auth.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class ResendSignUpVerificationEmailUseCaseTest {

    @Test
    fun `returns error when email is blank`() = runBlocking {
        val repository = FakeAuthRepositoryForResend()
        val useCase = ResendSignUpVerificationEmailUseCase(repository)

        val result = useCase(" ")

        val error = assertIs<AuthResult.Error>(result)
        assertEquals("이메일을 입력해주세요", error.message)
        assertEquals(0, repository.resendCallCount)
    }

    @Test
    fun `returns error when email format is invalid`() = runBlocking {
        val repository = FakeAuthRepositoryForResend()
        val useCase = ResendSignUpVerificationEmailUseCase(repository)

        val result = useCase("invalid-email")

        val error = assertIs<AuthResult.Error>(result)
        assertEquals("올바른 이메일 형식이 아닙니다", error.message)
        assertEquals(0, repository.resendCallCount)
    }

    @Test
    fun `delegates to repository when email is valid`() = runBlocking {
        val repository = FakeAuthRepositoryForResend()
        val useCase = ResendSignUpVerificationEmailUseCase(repository)

        val result = useCase("valid@email.com")

        assertIs<AuthResult.Success<Unit>>(result)
        assertEquals(1, repository.resendCallCount)
        assertEquals("valid@email.com", repository.lastResendEmail)
    }
}

private class FakeAuthRepositoryForResend : AuthRepository {
    var resendCallCount: Int = 0
    var lastResendEmail: String? = null

    override val authState: Flow<AuthState> = flowOf(AuthState.Unauthenticated())

    override suspend fun handleAuthDeepLink(url: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signUp(email: String, password: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signIn(email: String, password: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun resendSignUpVerificationEmail(email: String): AuthResult<Unit> {
        resendCallCount += 1
        lastResendEmail = email
        return AuthResult.Success(Unit)
    }

    override suspend fun signOut(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun resetPassword(email: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override fun isLoggedIn(): Boolean = false
}
