package kr.jm.weddingledger.feature.onboarding.presentation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingStep
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository
import kr.jm.weddingledger.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.ConnectPartnerUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.UpdateMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.UpdateNicknameUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class OnboardingViewModelTest {

    @Test
    fun `initial load with incomplete profile enters entry choice step`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForViewModel(
            profile = profile(
                nickname = null,
                role = null,
                weddingDate = null,
                isUndecided = false,
                onboardingCompleted = false,
            )
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals(OnboardingStep.EntryChoice, viewModel.state.value.step)
    }

    @Test
    fun `initial load with completed profile still enters entry choice when onboarding not completed`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForViewModel(
            profile = profile(
                nickname = "예비신부",
                role = UserRole.Bride,
                weddingDate = LocalDate.parse("2026-10-10"),
                isUndecided = false,
                onboardingCompleted = false,
            )
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals(OnboardingStep.EntryChoice, viewModel.state.value.step)
    }

    @Test
    fun `start path submitting profile completes onboarding immediately`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForViewModel(
            profile = profile(
                nickname = null,
                role = null,
                weddingDate = null,
                isUndecided = false,
                onboardingCompleted = false,
            )
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(OnboardingAction.OnClickStartOnboarding)
        viewModel.onAction(OnboardingAction.OnNicknameChange("신부"))
        viewModel.onAction(OnboardingAction.OnSelectRole(UserRole.Bride))
        viewModel.onAction(OnboardingAction.OnWeddingDateSelected(LocalDate.parse("2026-10-10")))
        viewModel.onAction(OnboardingAction.OnClickProfileNext)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(OnboardingStep.Done, state.step)
        assertEquals(1, repository.updateProfileCallCount)
        assertTrue(repository.completeCalled)
    }

    @Test
    fun `invalid partner invite code shows validation message on join path`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForViewModel(
            profile = profile(
                nickname = null,
                role = null,
                weddingDate = null,
                isUndecided = false,
                onboardingCompleted = false,
            )
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(OnboardingAction.OnClickJoinWithSharedCode)
        viewModel.onAction(OnboardingAction.OnJoinNicknameChange("예비신랑"))
        viewModel.onAction(OnboardingAction.OnPartnerCodeInputChange("1234"))
        viewModel.onAction(OnboardingAction.OnClickConnectPartner)

        assertEquals("초대 코드는 W-1234 형식으로 입력해주세요.", viewModel.state.value.message)
    }

    @Test
    fun `join path connect partner success completes onboarding`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForViewModel(
            profile = profile(
                nickname = null,
                role = null,
                weddingDate = null,
                isUndecided = false,
                onboardingCompleted = false,
            )
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(OnboardingAction.OnClickJoinWithSharedCode)
        viewModel.onAction(OnboardingAction.OnJoinNicknameChange("신랑"))
        viewModel.onAction(OnboardingAction.OnPartnerCodeInputChange("w1234"))
        viewModel.onAction(OnboardingAction.OnClickConnectPartner)
        advanceUntilIdle()

        assertEquals(OnboardingStep.Done, viewModel.state.value.step)
        assertEquals("W-1234", repository.lastConnectedCode)
        assertEquals("신랑", repository.lastUpdatedNickname)
        assertEquals(1, repository.updateNicknameCallCount)
        assertTrue(repository.completeCalled)
    }
}

private fun createViewModel(repository: FakeOnboardingRepositoryForViewModel): OnboardingViewModel {
    return OnboardingViewModel(
        fetchMyProfileUseCase = FetchMyProfileUseCase(repository),
        updateNicknameUseCase = UpdateNicknameUseCase(repository),
        updateMyProfileUseCase = UpdateMyProfileUseCase(repository),
        connectPartnerUseCase = ConnectPartnerUseCase(repository),
        completeOnboardingUseCase = CompleteOnboardingUseCase(repository),
    )
}

private class FakeOnboardingRepositoryForViewModel(
    private var profile: MyProfile,
) : OnboardingRepository {
    var updateProfileCallCount: Int = 0
    var updateNicknameCallCount: Int = 0
    var lastUpdatedNickname: String? = null
    var lastConnectedCode: String? = null
    var completeCalled: Boolean = false

    override suspend fun fetchMyProfile(): OnboardingResult<MyProfile> {
        return OnboardingResult.Success(profile)
    }

    override suspend fun updateNickname(nickname: String): OnboardingResult<Unit> {
        updateNicknameCallCount += 1
        lastUpdatedNickname = nickname
        profile = profile.copy(nickname = nickname)
        return OnboardingResult.Success(Unit)
    }

    override suspend fun updateMyProfile(
        nickname: String,
        role: UserRole,
        weddingDate: LocalDate?,
        isWeddingDateUndecided: Boolean,
    ): OnboardingResult<Unit> {
        updateProfileCallCount += 1
        profile = profile.copy(
            nickname = nickname,
            role = role,
            weddingDate = weddingDate,
            isWeddingDateUndecided = isWeddingDateUndecided,
        )
        return OnboardingResult.Success(Unit)
    }

    override suspend fun getOrCreateInviteCode(): OnboardingResult<String> {
        return OnboardingResult.Success("W-1234")
    }

    override suspend fun connectPartner(inviteCode: String): OnboardingResult<Unit> {
        lastConnectedCode = inviteCode
        return OnboardingResult.Success(Unit)
    }

    override suspend fun completeOnboarding(): OnboardingResult<Unit> {
        completeCalled = true
        profile = profile.copy(onboardingCompleted = true)
        return OnboardingResult.Success(Unit)
    }
}

private fun profile(
    nickname: String?,
    role: UserRole?,
    weddingDate: LocalDate?,
    isUndecided: Boolean,
    onboardingCompleted: Boolean,
): MyProfile {
    return MyProfile(
        id = "profile-1",
        email = "user@weddinglog.app",
        nickname = nickname,
        role = role,
        weddingId = null,
        weddingDate = weddingDate,
        isWeddingDateUndecided = isUndecided,
        inviteCode = null,
        onboardingCompleted = onboardingCompleted,
    )
}
