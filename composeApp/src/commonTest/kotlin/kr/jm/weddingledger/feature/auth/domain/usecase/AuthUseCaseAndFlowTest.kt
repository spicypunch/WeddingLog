package kr.jm.weddingledger.feature.auth.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kr.jm.weddingledger.feature.auth.domain.model.AuthProvider
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.model.AuthUser
import kr.jm.weddingledger.feature.auth.domain.model.DeleteAccountRequest
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class AuthUseCaseAndFlowTest {

    @Test
    fun `sign in use case delegates to repository`() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = SignInUseCase(repository)

        val result = useCase("user@weddinglog.app", "password")

        assertIs<AuthResult.Success<Unit>>(result)
        assertEquals(1, repository.signInCallCount)
        assertEquals("user@weddinglog.app", repository.lastSignInEmail)
        assertEquals("password", repository.lastSignInPassword)
    }

    @Test
    fun `sign up use case returns disabled message`() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = SignUpUseCase(repository)

        val result = useCase("user@weddinglog.app", "123456")

        assertEquals(
            "비밀번호 회원가입은 현재 비활성화되어 있습니다.",
            assertIs<AuthResult.Error>(result).message,
        )
    }

    @Test
    fun `reset password validates email before repository call`() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = ResetPasswordUseCase(repository)

        val blank = useCase(" ")
        val invalid = useCase("hello")
        val success = useCase("hello@weddinglog.app")

        assertEquals("이메일을 입력해주세요", assertIs<AuthResult.Error>(blank).message)
        assertEquals("올바른 이메일 형식이 아닙니다", assertIs<AuthResult.Error>(invalid).message)
        assertIs<AuthResult.Success<Unit>>(success)
        assertEquals(1, repository.resetPasswordCallCount)
        assertEquals("hello@weddinglog.app", repository.lastResetPasswordEmail)
    }

    @Test
    fun `sign out delegates to repository`() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = SignOutUseCase(repository)

        val result = useCase()

        assertIs<AuthResult.Success<Unit>>(result)
        assertEquals(1, repository.signOutCallCount)
    }

    @Test
    fun `observe auth state returns repository stream`() = runBlocking {
        val repository = FakeAuthRepository()
        val user = AuthUser(id = "u-1", email = "user@weddinglog.app", createdAt = null)
        repository.authStateFlow.value = AuthState.Authenticated(user)

        val state = ObserveAuthStateUseCase(repository).invoke().first()

        assertEquals(AuthState.Authenticated(user), state)
    }

    @Test
    fun `get current user maps authenticated state only`() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = GetCurrentUserUseCase(repository)

        val user = AuthUser(id = "u-2", email = "groom@weddinglog.app", createdAt = null)
        repository.authStateFlow.value = AuthState.Authenticated(user)
        assertEquals(user, useCase().first())

        repository.authStateFlow.value = AuthState.Unauthenticated()
        assertNull(useCase().first())
    }

    @Test
    fun `integration flow sign in authenticates current user`() = runBlocking {
        val repository = FakeAuthRepository()
        val signInUseCase = SignInUseCase(repository)
        val currentUserUseCase = GetCurrentUserUseCase(repository)

        signInUseCase("bride@weddinglog.app", "password")
        assertEquals(
            AuthUser(id = "user-1", email = "bride@weddinglog.app", createdAt = null),
            currentUserUseCase().first(),
        )
    }
}

private class FakeAuthRepository : AuthRepository {
    val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated())

    var signInCallCount: Int = 0
    var signOutCallCount: Int = 0
    var resetPasswordCallCount: Int = 0

    var lastSignInEmail: String? = null
    var lastSignInPassword: String? = null
    var lastResetPasswordEmail: String? = null

    override val authState: Flow<AuthState> = authStateFlow

    override suspend fun handleAuthDeepLink(url: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signInWithKakao(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signInWithGoogle(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signInWithApple(): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun sendEmailOtp(email: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun verifyEmailOtp(email: String, token: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signUp(email: String, password: String): AuthResult<Unit> = AuthResult.Error("비활성")

    override suspend fun signIn(email: String, password: String): AuthResult<Unit> {
        signInCallCount += 1
        lastSignInEmail = email
        lastSignInPassword = password
        authStateFlow.value = AuthState.Authenticated(
            AuthUser(
                id = "user-1",
                email = email,
                createdAt = null,
            )
        )
        return AuthResult.Success(Unit)
    }

    override suspend fun resendSignUpVerificationEmail(email: String): AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun signOut(): AuthResult<Unit> {
        signOutCallCount += 1
        authStateFlow.value = AuthState.Unauthenticated()
        return AuthResult.Success(Unit)
    }

    override suspend fun resetPassword(email: String): AuthResult<Unit> {
        resetPasswordCallCount += 1
        lastResetPasswordEmail = email
        return AuthResult.Success(Unit)
    }

    override suspend fun deleteAccount(request: DeleteAccountRequest): AuthResult<Unit> = AuthResult.Success(Unit)

    override fun getCurrentAuthProvider(): AuthProvider = AuthProvider.Unknown

    override fun isLoggedIn(): Boolean = authStateFlow.value is AuthState.Authenticated
}
