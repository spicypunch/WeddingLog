package kr.jm.weddingledger.feature.onboarding.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository

class OnboardingUseCaseAndFlowTest {

    @Test
    fun `fetch and update usecases delegate to repository`() = runBlocking {
        val repository = InMemoryOnboardingRepository()
        val fetchUseCase = FetchMyProfileUseCase(repository)
        val updateNicknameUseCase = UpdateNicknameUseCase(repository)
        val updateUseCase = UpdateMyProfileUseCase(repository)

        val fetchResult = fetchUseCase()
        updateNicknameUseCase("웨딩로그2")
        updateUseCase(
            nickname = "민지",
            role = UserRole.Bride,
            weddingDate = LocalDate.parse("2026-10-10"),
            isWeddingDateUndecided = false,
        )

        val profile = assertIs<OnboardingResult.Success<MyProfile>>(fetchResult).data
        assertEquals("웨딩로그", profile.nickname)
        assertEquals(1, repository.fetchCallCount)
        assertEquals(1, repository.updateNicknameCallCount)
        assertEquals(1, repository.updateCallCount)
    }

    @Test
    fun `invite and complete usecases delegate to repository`() = runBlocking {
        val repository = InMemoryOnboardingRepository()
        val inviteUseCase = GetOrCreateInviteCodeUseCase(repository)
        val connectUseCase = ConnectPartnerUseCase(repository)
        val completeUseCase = CompleteOnboardingUseCase(repository)

        val invite = inviteUseCase()
        val connect = connectUseCase("123456")
        val complete = completeUseCase()

        assertEquals("123456", assertIs<OnboardingResult.Success<String>>(invite).data)
        assertIs<OnboardingResult.Success<Unit>>(connect)
        assertIs<OnboardingResult.Success<Unit>>(complete)
        assertEquals(1, repository.inviteCallCount)
        assertEquals(1, repository.connectCallCount)
        assertEquals(1, repository.completeCallCount)
    }

    @Test
    fun `integration onboarding flow updates profile and completes`() = runBlocking {
        val repository = InMemoryOnboardingRepository()
        val updateUseCase = UpdateMyProfileUseCase(repository)
        val connectUseCase = ConnectPartnerUseCase(repository)
        val completeUseCase = CompleteOnboardingUseCase(repository)
        val fetchUseCase = FetchMyProfileUseCase(repository)

        updateUseCase(
            nickname = "신랑",
            role = UserRole.Groom,
            weddingDate = LocalDate.parse("2026-09-20"),
            isWeddingDateUndecided = false,
        )
        connectUseCase("123456")
        completeUseCase()

        val profile = assertIs<OnboardingResult.Success<MyProfile>>(fetchUseCase()).data
        assertEquals("신랑", profile.nickname)
        assertEquals(UserRole.Groom, profile.role)
        assertEquals("wedding-1", profile.weddingId)
        assertEquals(true, profile.onboardingCompleted)
    }
}

private class InMemoryOnboardingRepository : OnboardingRepository {
    private var profile = MyProfile(
        id = "profile-1",
        email = "user@weddinglog.app",
        nickname = "웨딩로그",
        role = UserRole.Bride,
        weddingId = null,
        weddingDate = null,
        isWeddingDateUndecided = true,
        inviteCode = null,
        onboardingCompleted = false,
    )

    var fetchCallCount: Int = 0
    var updateNicknameCallCount: Int = 0
    var updateCallCount: Int = 0
    var inviteCallCount: Int = 0
    var connectCallCount: Int = 0
    var completeCallCount: Int = 0

    override suspend fun fetchMyProfile(): OnboardingResult<MyProfile> {
        fetchCallCount += 1
        return OnboardingResult.Success(profile)
    }

    override suspend fun updateNickname(nickname: String): OnboardingResult<Unit> {
        updateNicknameCallCount += 1
        profile = profile.copy(nickname = nickname)
        return OnboardingResult.Success(Unit)
    }

    override suspend fun updateMyProfile(
        nickname: String,
        role: UserRole,
        weddingDate: LocalDate?,
        isWeddingDateUndecided: Boolean,
    ): OnboardingResult<Unit> {
        updateCallCount += 1
        profile = profile.copy(
            nickname = nickname,
            role = role,
            weddingDate = weddingDate,
            isWeddingDateUndecided = isWeddingDateUndecided,
        )
        return OnboardingResult.Success(Unit)
    }

    override suspend fun getOrCreateInviteCode(): OnboardingResult<String> {
        inviteCallCount += 1
        val inviteCode = profile.inviteCode ?: "123456"
        profile = profile.copy(inviteCode = inviteCode)
        return OnboardingResult.Success(inviteCode)
    }

    override suspend fun connectPartner(inviteCode: String): OnboardingResult<Unit> {
        connectCallCount += 1
        profile = profile.copy(weddingId = "wedding-1", inviteCode = inviteCode)
        return OnboardingResult.Success(Unit)
    }

    override suspend fun completeOnboarding(): OnboardingResult<Unit> {
        completeCallCount += 1
        profile = profile.copy(onboardingCompleted = true)
        return OnboardingResult.Success(Unit)
    }
}
