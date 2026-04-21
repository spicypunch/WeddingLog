package kr.jm.weddingledger.ui

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kr.jm.weddingledger.feature.auth.presentation.AuthMode
import kr.jm.weddingledger.feature.auth.presentation.AuthUiState
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.presentation.ProfileSetupUiState
import kr.jm.weddingledger.feature.schedule.presentation.ChecklistGroup
import kr.jm.weddingledger.feature.schedule.presentation.ChecklistItem
import kr.jm.weddingledger.feature.schedule.presentation.ChecklistItemSubtitleTone
import kr.jm.weddingledger.feature.schedule.presentation.ChecklistUiState
import kr.jm.weddingledger.feature.schedule.presentation.DTagStyle
import kr.jm.weddingledger.feature.settings.presentation.AccountSettingsUiState
import kr.jm.weddingledger.feature.settings.presentation.ProfileEditUiState

class UiStateContractTest {

    @Test
    fun `auth sign in submit enabled only when email password and terms are provided`() {
        val disabled = AuthUiState(mode = AuthMode.SignIn, email = "", password = "")
        val missingTerms = AuthUiState(mode = AuthMode.SignIn, email = "hello@weddinglog.app", password = "pw")
        val enabled = missingTerms.copy(isTermsAccepted = true)

        assertFalse(disabled.isSubmitEnabled)
        assertFalse(missingTerms.isSubmitEnabled)
        assertTrue(enabled.isSubmitEnabled)
    }

    @Test
    fun `auth sign up submit requires confirm password and terms`() {
        val invalid = AuthUiState(
            mode = AuthMode.SignUp,
            email = "hello@weddinglog.app",
            password = "password",
            passwordConfirm = "password",
            isTermsAccepted = false,
        )
        val valid = invalid.copy(isTermsAccepted = true)

        assertFalse(invalid.isSubmitEnabled)
        assertTrue(valid.isSubmitEnabled)
    }

    @Test
    fun `auth resend button label shows cooldown seconds`() {
        val state = AuthUiState(
            mode = AuthMode.SignIn,
            isEmailVerificationRequired = true,
            resendCooldownSeconds = 24,
        )

        assertEquals("인증 메일 재전송 (24초)", state.resendButtonLabel)
        assertFalse(state.isResendVerificationEnabled)
    }

    @Test
    fun `profile edit can save only with required inputs`() {
        val disabled = ProfileEditUiState.initial().copy(
            isLoading = false,
            nickname = " ",
            selectedRole = UserRole.Groom,
            weddingDate = LocalDate.parse("2026-10-10"),
            isDateUndecided = false,
        )
        val enabled = disabled.copy(nickname = "신랑")

        assertFalse(disabled.canSave)
        assertTrue(enabled.canSave)
    }

    @Test
    fun `onboarding profile date text reflects undecided and fixed date`() {
        val undecided = ProfileSetupUiState.initial().copy(isDateUndecided = true)
        val selected = ProfileSetupUiState.initial().copy(
            isDateUndecided = false,
            weddingDate = LocalDate.parse("2026-10-10"),
        )

        assertEquals("아직 정해지지 않았어요", undecided.weddingDateText)
        assertEquals("2026.10.10", selected.weddingDateText)
    }

    @Test
    fun `account settings email actions require valid email and idle state`() {
        val loadingState = AccountSettingsUiState.initial(email = "hello@weddinglog.app").copy(isLoading = true)
        val invalidEmailState = AccountSettingsUiState.initial(email = "invalid")
        val validState = AccountSettingsUiState.initial(email = "hello@weddinglog.app")

        assertFalse(loadingState.canSendEmailActions)
        assertFalse(invalidEmailState.canSendEmailActions)
        assertTrue(validState.canSendEmailActions)
    }

    @Test
    fun `checklist ui state exposes progress and counts`() {
        val uiState = ChecklistUiState(
            groups = listOf(
                ChecklistGroup(
                    id = "d-30",
                    dTag = "D-30",
                    title = "본식 준비",
                    tagStyle = DTagStyle.Primary,
                    items = listOf(
                        ChecklistItem(
                            id = "task-1",
                            title = "예식장 계약",
                            subtitle = "완료",
                            subtitleTone = ChecklistItemSubtitleTone.Muted,
                            isDone = true,
                        ),
                        ChecklistItem(
                            id = "task-2",
                            title = "청첩장 제작",
                            subtitle = null,
                            subtitleTone = ChecklistItemSubtitleTone.Muted,
                            isDone = false,
                        ),
                    ),
                )
            )
        )

        assertEquals(2, uiState.totalCount)
        assertEquals(1, uiState.completedCount)
        assertEquals("50%", uiState.percentText)
        assertEquals("1/2개", uiState.countText)
        assertEquals(0.5f, uiState.progressFraction)
    }
}
