package kr.jm.weddingledger.feature.money.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole

class AddExpenseContractTest {

    @Test
    fun `default expense payer follows current user role`() {
        assertEquals(Payer.Groom, UserRole.Groom.toDefaultExpensePayer())
        assertEquals(Payer.Bride, UserRole.Bride.toDefaultExpensePayer())
    }

    @Test
    fun `default expense payer falls back to joint when role is missing`() {
        assertEquals(Payer.Joint, null.toDefaultExpensePayer())
    }
}
