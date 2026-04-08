package kr.jm.weddingledger.feature.onboarding.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kr.jm.weddingledger.feature.auth.domain.model.AuthProvider
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.model.DeleteAccountRequest
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository
import kr.jm.weddingledger.feature.auth.domain.usecase.SignOutUseCase
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingErrorType
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

        assertEquals("초대 코드는 6자리 숫자로 입력해주세요.", viewModel.state.value.message)
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
        viewModel.onAction(OnboardingAction.OnPartnerCodeInputChange("123-456"))
        viewModel.onAction(OnboardingAction.OnClickConnectPartner)
        advanceUntilIdle()

        assertEquals(OnboardingStep.Done, viewModel.state.value.step)
        assertEquals("123456", repository.lastConnectedCode)
        assertEquals("신랑", repository.lastUpdatedNickname)
        assertEquals(1, repository.updateNicknameCallCount)
        assertTrue(repository.completeCalled)
    }

    @Test
    fun `initial load auth-related error signs out instead of showing entry choice`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForViewModel(
            profile = profile(
                nickname = null,
                role = null,
                weddingDate = null,
                isUndecided = false,
                onboardingCompleted = false,
            ),
            fetchProfileResult = OnboardingResult.Error(
                message = "임의 문구",
                type = OnboardingErrorType.ReauthenticationRequired,
            ),
        )
        val authRepository = FakeAuthRepositoryForViewModel()

        val viewModel = createViewModel(repository, authRepository)
        advanceUntilIdle()

        assertEquals(1, authRepository.signOutCallCount)
        assertEquals(OnboardingStep.Loading, viewModel.state.value.step)
    }

    @Test
    fun `initial load generic error shows entry choice without sign out`() = runMainDispatcherTest {
        val repository = FakeOnboardingRepositoryForViewModel(
            profile = profile(
                nickname = null,
                role = null,
                weddingDate = null,
                isUndecided = false,
                onboardingCompleted = false,
            ),
            fetchProfileResult = OnboardingResult.Error("프로필 정보를 불러오지 못했습니다."),
        )
        val authRepository = FakeAuthRepositoryForViewModel()

        val viewModel = createViewModel(repository, authRepository)
        advanceUntilIdle()

        assertEquals(0, authRepository.signOutCallCount)
        assertEquals(OnboardingStep.EntryChoice, viewModel.state.value.step)
        assertEquals("프로필 정보를 불러오지 못했습니다.", viewModel.state.value.message)
    }
}

private fun createViewModel(
    repository: FakeOnboardingRepositoryForViewModel,
    authRepository: FakeAuthRepositoryForViewModel = FakeAuthRepositoryForViewModel(),
): OnboardingViewModel {
    return OnboardingViewModel(
        fetchMyProfileUseCase = FetchMyProfileUseCase(repository),
        updateNicknameUseCase = UpdateNicknameUseCase(repository),
        updateMyProfileUseCase = UpdateMyProfileUseCase(repository),
        connectPartnerUseCase = ConnectPartnerUseCase(repository),
        completeOnboardingUseCase = CompleteOnboardingUseCase(repository),
        signOutUseCase = SignOutUseCase(authRepository),
    )
}

private class FakeOnboardingRepositoryForViewModel(
    private var profile: MyProfile,
    private val fetchProfileResult: OnboardingResult<MyProfile>? = null,
) : OnboardingRepository {
    var updateProfileCallCount: Int = 0
    var updateNicknameCallCount: Int = 0
    var lastUpdatedNickname: String? = null
    var lastConnectedCode: String? = null
    var completeCalled: Boolean = false

    override suspend fun fetchMyProfile(): OnboardingResult<MyProfile> {
        return fetchProfileResult ?: OnboardingResult.Success(profile)
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
        return OnboardingResult.Success("123456")
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

private class FakeAuthRepositoryForViewModel : AuthRepository {
    override val authState: Flow<AuthState> = MutableStateFlow(AuthState.Unauthenticated())

    var signOutCallCount: Int = 0
    var signOutResult: AuthResult<Unit> = AuthResult.Success(Unit)

    override suspend fun handleAuthDeepLink(url: String): AuthResult<Unit> = AuthResult.Error("unused")

    override suspend fun signInWithKakao(): AuthResult<Unit> = AuthResult.Error("unused")

    override suspend fun signInWithGoogle(): AuthResult<Unit> = AuthResult.Error("unused")

    override suspend fun signInWithApple(): AuthResult<Unit> = AuthResult.Error("unused")

    override suspend fun sendEmailOtp(email: String): AuthResult<Unit> = AuthResult.Error("unused")

    override suspend fun verifyEmailOtp(email: String, token: String): AuthResult<Unit> = AuthResult.Error("unused")

    override suspend fun signUp(email: String, password: String): AuthResult<Unit> = AuthResult.Error("unused")

    override suspend fun signIn(email: String, password: String): AuthResult<Unit> = AuthResult.Error("unused")

    override suspend fun resendSignUpVerificationEmail(email: String): AuthResult<Unit> = AuthResult.Error("unused")

    override suspend fun signOut(): AuthResult<Unit> {
        signOutCallCount += 1
        return signOutResult
    }

    override suspend fun resetPassword(email: String): AuthResult<Unit> = AuthResult.Error("unused")

    override suspend fun deleteAccount(request: DeleteAccountRequest): AuthResult<Unit> = AuthResult.Error("unused")

    override fun getCurrentAuthProvider(): AuthProvider = AuthProvider.Unknown

    override fun isLoggedIn(): Boolean = true
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
