package kr.jm.weddingledger.feature.auth.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthResultTest {

    @Test
    fun `error defaults to unknown type`() {
        val error = AuthResult.Error("오류")

        assertEquals(AuthErrorType.Unknown, error.type)
    }

    @Test
    fun `error preserves explicit type`() {
        val error = AuthResult.Error(
            message = "인증 필요",
            type = AuthErrorType.EmailVerificationRequired,
        )

        assertEquals(AuthErrorType.EmailVerificationRequired, error.type)
    }
}
