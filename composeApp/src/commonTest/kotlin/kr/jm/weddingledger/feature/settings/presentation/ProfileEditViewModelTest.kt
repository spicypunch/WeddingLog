package kr.jm.weddingledger.feature.settings.presentation

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.UpdateMyProfileUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class ProfileEditViewModelTest {

    @Test
    fun `loads profile into editable state`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForProfileEdit(
            profileResult = OnboardingResult.Success(
                profile(
                    nickname = "신부",
                    role = UserRole.Bride,
                    weddingDate = LocalDate.parse("2026-10-10"),
                    isUndecided = false,
                )
            ),
            updateResult = OnboardingResult.Success(Unit),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("신부", state.nickname)
        assertEquals(UserRole.Bride, state.selectedRole)
        assertEquals("2026.10.10", state.weddingDateText)
    }

    @Test
    fun `save validation fails when role is missing`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForProfileEdit(
            profileResult = OnboardingResult.Success(
                profile(
                    nickname = "신랑",
                    role = null,
                    weddingDate = LocalDate.parse("2026-10-10"),
                    isUndecided = false,
                )
            ),
            updateResult = OnboardingResult.Success(Unit),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(ProfileEditAction.OnClickSave)

        assertEquals("역할을 선택해주세요.", viewModel.state.value.message)
    }

    @Test
    fun `save success emits saved effect`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForProfileEdit(
            profileResult = OnboardingResult.Success(
                profile(
                    nickname = "신랑",
                    role = UserRole.Groom,
                    weddingDate = LocalDate.parse("2026-10-10"),
                    isUndecided = false,
                )
            ),
            updateResult = OnboardingResult.Success(Unit),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val effect = async { viewModel.effect.first() }
        viewModel.onAction(ProfileEditAction.OnClickSave)
        advanceUntilIdle()

        assertIs<ProfileEditEffect.Saved>(effect.await())
        assertEquals("프로필을 저장했습니다.", viewModel.state.value.message)
    }
}

private fun createViewModel(repository: FakeOnboardingRepositoryForProfileEdit): ProfileEditViewModel {
    return ProfileEditViewModel(
        fetchMyProfileUseCase = FetchMyProfileUseCase(repository),
        updateMyProfileUseCase = UpdateMyProfileUseCase(repository),
    )
}

private class FakeOnboardingRepositoryForProfileEdit(
    private val profileResult: OnboardingResult<MyProfile>,
    private val updateResult: OnboardingResult<Unit>,
) : OnboardingRepository {

    override suspend fun fetchMyProfile(): OnboardingResult<MyProfile> = profileResult

    override suspend fun updateNickname(nickname: String): OnboardingResult<Unit> = OnboardingResult.Success(Unit)

    override suspend fun updateMyProfile(
        nickname: String,
        role: UserRole,
        weddingDate: LocalDate?,
        isWeddingDateUndecided: Boolean,
    ): OnboardingResult<Unit> = updateResult

    override suspend fun getOrCreateInviteCode(): OnboardingResult<String> = OnboardingResult.Success("123456")

    override suspend fun connectPartner(inviteCode: String): OnboardingResult<Unit> = OnboardingResult.Success(Unit)

    override suspend fun completeOnboarding(): OnboardingResult<Unit> = OnboardingResult.Success(Unit)
}

private fun profile(
    nickname: String,
    role: UserRole?,
    weddingDate: LocalDate?,
    isUndecided: Boolean,
): MyProfile {
    return MyProfile(
        id = "profile-1",
        email = "user@weddinglog.app",
        nickname = nickname,
        role = role,
        weddingId = "wedding-1",
        weddingDate = weddingDate,
        isWeddingDateUndecided = isUndecided,
        inviteCode = "123456",
        onboardingCompleted = false,
    )
}
