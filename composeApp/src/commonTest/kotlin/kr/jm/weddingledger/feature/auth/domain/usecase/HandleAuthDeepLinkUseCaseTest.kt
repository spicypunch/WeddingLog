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

class HandleAuthDeepLinkUseCaseTest {

    @Test
    fun `returns error when url is blank`() = runBlocking {
        val repository = FakeAuthRepositoryForHandleDeepLink()
        val useCase = HandleAuthDeepLinkUseCase(repository)

        val result = useCase(" ")

        val error = assertIs<AuthResult.Error>(result)
        assertEquals("인증 링크가 비어 있습니다.", error.message)
        assertEquals(0, repository.handleCallCount)
    }

    @Test
    fun `delegates to repository when url is valid`() = runBlocking {
        val repository = FakeAuthRepositoryForHandleDeepLink()
        val useCase = HandleAuthDeepLinkUseCase(repository)

        val result = useCase("weddingledger://auth?code=abc")

        assertIs<AuthResult.Success<Unit>>(result)
        assertEquals(1, repository.handleCallCount)
    }
}

private class FakeAuthRepositoryForHandleDeepLink : AuthRepository {
    var handleCallCount: Int = 0
    override val authState: Flow<AuthState> = flowOf(AuthState.Unauthenticated())

    override suspend fun handleAuthDeepLink(url: String): AuthResult<Unit> {
        handleCallCount += 1
        return AuthResult.Success(Unit)
    }

    override suspend fun signInWithKakao(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signInWithGoogle(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signInWithApple(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun sendEmailOtp(email: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun verifyEmailOtp(email: String, token: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signUp(email: String, password: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signIn(email: String, password: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun resendSignUpVerificationEmail(email: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signOut(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun resetPassword(email: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override fun isLoggedIn(): Boolean = false
}
