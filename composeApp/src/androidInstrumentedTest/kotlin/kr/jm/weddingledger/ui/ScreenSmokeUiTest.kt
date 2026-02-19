package kr.jm.weddingledger.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.datetime.LocalDate
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.feature.auth.presentation.AuthAction
import kr.jm.weddingledger.feature.auth.presentation.AuthScreen
import kr.jm.weddingledger.feature.auth.presentation.AuthUiState
import kr.jm.weddingledger.feature.guests.presentation.GuestsAction
import kr.jm.weddingledger.feature.guests.presentation.GuestsScreen
import kr.jm.weddingledger.feature.guests.presentation.GuestsUiState
import kr.jm.weddingledger.feature.home.presentation.HomeAction
import kr.jm.weddingledger.feature.home.presentation.HomeScreen
import kr.jm.weddingledger.feature.home.presentation.HomeUiState
import kr.jm.weddingledger.feature.money.presentation.MoneyAction
import kr.jm.weddingledger.feature.money.presentation.MoneyScreen
import kr.jm.weddingledger.feature.money.presentation.MoneyTab
import kr.jm.weddingledger.feature.money.presentation.MoneyUiState
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationItemType
import kr.jm.weddingledger.feature.notifications.presentation.NotificationCenterAction
import kr.jm.weddingledger.feature.notifications.presentation.NotificationCenterScreen
import kr.jm.weddingledger.feature.notifications.presentation.NotificationCenterUiState
import kr.jm.weddingledger.feature.notifications.presentation.NotificationItemUi
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.presentation.OnboardingAction
import kr.jm.weddingledger.feature.onboarding.presentation.PartnerConnectScreen
import kr.jm.weddingledger.feature.onboarding.presentation.PartnerConnectUiState
import kr.jm.weddingledger.feature.onboarding.presentation.ProfileSetupScreen
import kr.jm.weddingledger.feature.onboarding.presentation.ProfileSetupUiState
import kr.jm.weddingledger.feature.schedule.presentation.ScheduleAction
import kr.jm.weddingledger.feature.schedule.presentation.ScheduleScreen
import kr.jm.weddingledger.feature.schedule.presentation.ScheduleTab
import kr.jm.weddingledger.feature.schedule.presentation.ScheduleUiState
import kr.jm.weddingledger.feature.settings.presentation.SettingsAction
import kr.jm.weddingledger.feature.settings.presentation.SettingsScreen
import kr.jm.weddingledger.feature.settings.presentation.SettingsUiState
import kr.jm.weddingledger.feature.tools.presentation.ValuesSurveyAction
import kr.jm.weddingledger.feature.tools.presentation.ValuesSurveyOptionUi
import kr.jm.weddingledger.feature.tools.presentation.ValuesSurveyScreen
import kr.jm.weddingledger.feature.tools.presentation.ValuesSurveyUiState
import kr.jm.weddingledger.feature.tools.presentation.VenueComparisonAction
import kr.jm.weddingledger.feature.tools.presentation.VenueComparisonScreen
import kr.jm.weddingledger.feature.tools.presentation.VenueComparisonUiState
import kr.jm.weddingledger.feature.tools.presentation.VenueUiItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
@LargeTest
class ScreenSmokeUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun authScreen_clickLogin_dispatchesSubmitAction() {
        val actions = mutableListOf<AuthAction>()

        setScreen {
            AuthScreen(
                state = AuthUiState(
                    email = "user@weddinglog.app",
                    password = "password123",
                ),
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithText("로그인").assertIsDisplayed().performClick()
        assertTrue(actions.contains(AuthAction.OnClickSubmit))
    }

    @Test
    fun homeScreen_clickNotification_dispatchesAction() {
        val actions = mutableListOf<HomeAction>()

        setScreen {
            HomeScreen(
                state = HomeUiState.sample(),
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithContentDescription("알림").assertIsDisplayed().performClick()
        assertTrue(actions.contains(HomeAction.OnClickNotifications))
    }

    @Test
    fun guestsScreen_clickFab_dispatchesAddAction() {
        val actions = mutableListOf<GuestsAction>()

        setScreen {
            GuestsScreen(
                state = GuestsUiState.sample(),
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithContentDescription("추가").assertIsDisplayed().performClick()
        assertTrue(actions.contains(GuestsAction.OnClickAdd))
    }

    @Test
    fun moneyScreen_selectBudgetTab_dispatchesAction() {
        val actions = mutableListOf<MoneyAction>()

        setScreen {
            MoneyScreen(
                state = MoneyUiState.sample(),
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithText("예산").assertIsDisplayed().performClick()
        assertTrue(actions.contains(MoneyAction.OnSelectTab(MoneyTab.Budget)))
    }

    @Test
    fun scheduleScreen_selectChecklistTab_dispatchesAction() {
        val actions = mutableListOf<ScheduleAction>()

        setScreen {
            ScheduleScreen(
                state = ScheduleUiState.sample(),
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithText("체크리스트").assertIsDisplayed().performClick()
        assertTrue(actions.contains(ScheduleAction.OnSelectTab(ScheduleTab.Checklist)))
    }

    @Test
    fun settingsScreen_clickAccountSettings_dispatchesAction() {
        val actions = mutableListOf<SettingsAction>()

        setScreen {
            SettingsScreen(
                state = SettingsUiState.sample(WeddingLogThemeStyle.Minimal),
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithText("계정 관리").assertIsDisplayed().performClick()
        assertTrue(actions.contains(SettingsAction.OnClickAccountSettings))
    }

    @Test
    fun notificationCenterScreen_clickScheduleReminder_dispatchesAction() {
        val actions = mutableListOf<NotificationCenterAction>()

        setScreen {
            NotificationCenterScreen(
                state = NotificationCenterUiState(
                    title = "알림센터",
                    isLoading = false,
                    items = listOf(
                        NotificationItemUi(
                            id = "noti-1",
                            type = NotificationItemType.Schedule,
                            title = "스튜디오 촬영",
                            subtitle = "내일 14:00",
                            detail = "강남구 루브르 스튜디오",
                            canScheduleReminder = true,
                            isReminderScheduled = false,
                        )
                    ),
                    message = null,
                    showPermissionAction = false,
                ),
                onBack = {},
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithText("하루 전 리마인더 예약").assertIsDisplayed().performClick()
        assertTrue(actions.contains(NotificationCenterAction.OnClickScheduleReminder("noti-1")))
    }

    @Test
    fun profileSetupScreen_clickNext_dispatchesAction() {
        val actions = mutableListOf<OnboardingAction>()

        setScreen {
            ProfileSetupScreen(
                state = ProfileSetupUiState(
                    nickname = "김민수",
                    selectedRole = UserRole.Groom,
                    weddingDate = LocalDate(2026, 5, 24),
                    isDateUndecided = false,
                ),
                isLoading = false,
                message = null,
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithText("다음으로").assertIsDisplayed().performClick()
        assertTrue(actions.contains(OnboardingAction.OnClickProfileNext))
    }

    @Test
    fun partnerConnectScreen_clickConnect_dispatchesAction() {
        val actions = mutableListOf<OnboardingAction>()

        setScreen {
            PartnerConnectScreen(
                state = PartnerConnectUiState(
                    inviteCode = "WDLG-1234",
                    partnerCodeInput = "ABCD",
                ),
                isLoading = false,
                message = null,
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithText("파트너 연결하기").assertIsDisplayed().performClick()
        assertTrue(actions.contains(OnboardingAction.OnClickConnectPartner))
    }

    @Test
    fun valuesSurveyScreen_selectOption_dispatchesAction() {
        val actions = mutableListOf<ValuesSurveyAction>()

        setScreen {
            ValuesSurveyScreen(
                state = ValuesSurveyUiState.initial().copy(
                    isLoading = false,
                    progressText = "1 / 3",
                    progress = 0.33f,
                    currentIndex = 0,
                    totalCount = 3,
                    questionTitle = "예산 운영 방식이 중요하다",
                    questionDescription = "예산 계획과 절약 중심으로 준비하고 싶어요.",
                    options = listOf(
                        ValuesSurveyOptionUi(
                            id = "opt-1",
                            label = "실속 중심",
                            score = 3,
                        )
                    ),
                    selectedOptionId = null,
                    canGoPrev = false,
                    nextButtonText = "다음",
                    isCompleted = false,
                ),
                onBack = {},
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithText("실속 중심").assertIsDisplayed().performClick()
        assertTrue(actions.contains(ValuesSurveyAction.OnSelectOption("opt-1")))
    }

    @Test
    fun venueComparisonScreen_clickShare_dispatchesAction() {
        val actions = mutableListOf<VenueComparisonAction>()

        setScreen {
            VenueComparisonScreen(
                state = VenueComparisonUiState.initial().copy(
                    resultCountText = "1곳",
                    isLoading = false,
                    isEmpty = false,
                    items = listOf(
                        VenueUiItem(
                            id = "venue-1",
                            name = "웨딩홀A",
                            location = "강남",
                            priceText = "₩5,000,000",
                            ratingText = "4.8",
                            capacityText = "200명",
                            tags = listOf("교통 편리", "식사 우수"),
                            isFavorite = false,
                            accent = Color(0xFFEF3948),
                            recommendationNote = "동선이 깔끔하고 견적이 안정적입니다.",
                        )
                    ),
                ),
                onBack = {},
                onAction = actions::add,
            )
        }

        composeRule.onNodeWithContentDescription("공유").assertIsDisplayed().performClick()
        assertTrue(actions.contains(VenueComparisonAction.OnShareTopResults))
    }

    private fun setScreen(content: @Composable () -> Unit) {
        composeRule.setContent {
            MaterialTheme {
                content()
            }
        }
    }
}
