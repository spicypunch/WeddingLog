package kr.jm.weddingledger.feature.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState

interface AuthRepository {
    val authState: Flow<AuthState>
    
    suspend fun handleAuthDeepLink(url: String): AuthResult<Unit>
    suspend fun signInWithKakao(): AuthResult<Unit>
    suspend fun signInWithGoogle(): AuthResult<Unit>
    suspend fun signInWithApple(): AuthResult<Unit>
    suspend fun sendEmailOtp(email: String): AuthResult<Unit>
    suspend fun verifyEmailOtp(email: String, token: String): AuthResult<Unit>
    suspend fun signUp(email: String, password: String): AuthResult<Unit>
    suspend fun signIn(email: String, password: String): AuthResult<Unit>
    suspend fun resendSignUpVerificationEmail(email: String): AuthResult<Unit>
    suspend fun signOut(): AuthResult<Unit>
    suspend fun resetPassword(email: String): AuthResult<Unit>
    fun isLoggedIn(): Boolean
}
