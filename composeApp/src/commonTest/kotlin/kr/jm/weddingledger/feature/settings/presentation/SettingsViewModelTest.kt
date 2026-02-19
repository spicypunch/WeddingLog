package kr.jm.weddingledger.feature.settings.presentation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.GetOrCreateInviteCodeUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class SettingsViewModelTest {

    @Test
    fun `loads invite code from profile when present`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForSettings(
            profileResult = OnboardingResult.Success(profile(inviteCode = "W-7777")),
            inviteCodeResult = OnboardingResult.Success("W-1234"),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("W-7777", state.inviteCode)
        assertEquals(0, repository.getInviteCodeCallCount)
    }

    @Test
    fun `loads invite code from usecase when profile invite code is empty`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForSettings(
            profileResult = OnboardingResult.Success(profile(inviteCode = " ")),
            inviteCodeResult = OnboardingResult.Success("W-1234"),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("W-1234", state.inviteCode)
        assertEquals(1, repository.getInviteCodeCallCount)
    }

    @Test
    fun `profile load error populates fallback state and message`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForSettings(
            profileResult = OnboardingResult.Error("프로필 로드 실패"),
            inviteCodeResult = OnboardingResult.Success("W-1234"),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("프로필 설정 필요", state.coupleName)
        assertEquals("프로필 로드 실패", state.message)
    }

    @Test
    fun `theme style change updates selected theme`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForSettings(
            profileResult = OnboardingResult.Success(profile(inviteCode = "W-7777")),
            inviteCodeResult = OnboardingResult.Success("W-1234"),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onThemeStyleChanged(WeddingLogThemeStyle.Lovely)

        assertEquals(WeddingLogThemeStyle.Lovely, viewModel.state.value.selectedTheme)
    }
}

private fun createViewModel(repository: FakeOnboardingRepositoryForSettings): SettingsViewModel {
    return SettingsViewModel(
        fetchMyProfileUseCase = FetchMyProfileUseCase(repository),
        getOrCreateInviteCodeUseCase = GetOrCreateInviteCodeUseCase(repository),
    )
}

private class FakeOnboardingRepositoryForSettings(
    private val profileResult: OnboardingResult<MyProfile>,
    private val inviteCodeResult: OnboardingResult<String>,
) : OnboardingRepository {
    var getInviteCodeCallCount: Int = 0

    override suspend fun fetchMyProfile(): OnboardingResult<MyProfile> = profileResult

    override suspend fun updateMyProfile(
        nickname: String,
        role: UserRole,
        weddingDate: kotlinx.datetime.LocalDate?,
        isWeddingDateUndecided: Boolean,
    ): OnboardingResult<Unit> = OnboardingResult.Success(Unit)

    override suspend fun getOrCreateInviteCode(): OnboardingResult<String> {
        getInviteCodeCallCount += 1
        return inviteCodeResult
    }

    override suspend fun connectPartner(inviteCode: String): OnboardingResult<Unit> = OnboardingResult.Success(Unit)

    override suspend fun completeOnboarding(): OnboardingResult<Unit> = OnboardingResult.Success(Unit)
}

private fun profile(
    inviteCode: String?,
): MyProfile {
    return MyProfile(
        id = "profile-1",
        email = "user@weddinglog.app",
        nickname = "신랑",
        role = UserRole.Groom,
        weddingId = "wedding-1",
        weddingDate = null,
        isWeddingDateUndecided = true,
        inviteCode = inviteCode,
        onboardingCompleted = false,
    )
}
