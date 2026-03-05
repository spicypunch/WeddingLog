# WeddingLedger 인수인계서 (KMP + Compose Multiplatform)

작성일: 2026-02-24  
마지막 갱신: 2026-03-05  
워크스페이스: `/Users/mackim/AndroidStudioProjects/WeddingLedger`

---

## 0) 퀵스타트

1) 전역 규칙 확인: `/Users/mackim/AndroidStudioProjects/WeddingLedger/AGENTS.md`  
2) Supabase 키 확인: `/Users/mackim/AndroidStudioProjects/WeddingLedger/local.properties`  
3) 빌드/테스트:

```bash
cd /Users/mackim/AndroidStudioProjects/WeddingLedger
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:compileTestKotlinIosSimulatorArm64
# UI(instrumented) 테스트는 에뮬레이터/디바이스 연결 후 실행
./gradlew :composeApp:connectedDebugAndroidTest
```

---

## 1) 현재 상태 요약

- 아키텍처: Feature-first + Clean Architecture + MVVM 유지
- Auth/Onboarding: `join_wedding` RPC 포함 실데이터 동작
  - 이메일 인증 딥링크 E2E 연결 완료(Android `intent-filter` + iOS `onOpenURL` + KMP bridge)
  - 딥링크 처리 성공/실패 시 로그인 화면 안내 배너 표시(`AuthMessage.Info/Error`)
  - 로그인 미인증 전용 안내 카드 + 재전송 60초 쿨다운 + 최대 5회 제한 추가
  - 소셜 로그인 정책 전환: `Google + Kakao` 메인, `Email OTP` 보조, `Email/Password` 비활성
  - Kakao 로그인 네이티브 SDK 경로(iOS/Android) 적용 및 양 플랫폼 성공 검증 완료(2026-03-04)
  - 온보딩 엔트리 분기 도입(`혼자 시작하기` / `공유 코드로 시작하기`)
  - `혼자 시작하기` 경로: 프로필 입력(닉네임/역할/예식일) 후 즉시 온보딩 완료
  - `공유 코드로 시작하기` 경로: 닉네임 + 공유 코드 입력으로 파트너 연결
  - 초대 코드는 `6자리 숫자` 형식으로 통일(`W-` 접두어 제거)
  - 초대 코드 공유는 온보딩 필수 단계에서 제거하고 설정 화면에서 선택적으로 제공
  - 회원가입/온보딩 프로필 화면에서 키보드 열림 시 고정 하단 CTA를 스크롤 영역 CTA로 전환(가림/과상승 완화)
  - iOS 키보드 대응 통합:
    - SwiftUI 키보드 safe-area 자동 회피 비활성화
    - 전역 탭 dismiss는 키보드가 떠 있을 때만 동작
- Guests: 추가/상세/수정/삭제 + 참석 상태 빠른 변경 + 메모 지원
  - 참석 예상 인원/참석 기준 축의금 예측 표시
  - 그룹별(신랑측/신부측/공통) 합계 카드 표시
- Schedule:
  - 체크리스트 생성/수정/삭제/체크
  - 일정 CRUD + 상세 + 수정/삭제
  - 일정 시간 입력 UX(24h 마스킹 + 유효성)
  - 일정 반복(반복 없음/매주/매월) 지원
  - `repeat_rule` 컬럼 우선 + 레거시(`memo` 메타) 자동 폴백으로 반복 규칙 저장 호환성 유지
  - 다가오는 일정 검색/정렬(날짜 오름/내림, 이름순)
  - 캘린더 밀집일 마커 강화(다건 일정 수량 표시)
- Money:
  - 예산/지출 CRUD
  - 예산 탭 필터 동작
  - 헤더 설정 네비게이션 동작
  - 헤더 알림 아이콘 추가
  - 지출 탭 조합 필터(카테고리/기간/상태) + 프리셋 저장/적용(최대 5개) 지원
  - 지출 필터/프리셋/예산 초과 필터 상태 DataStore 영속화
- Settings:
  - 프로필/초대코드 실데이터 로드
  - 초대코드 복사 동작
  - 프로필 수정/계정 관리/앱 정보 화면 연결
  - 알림 설정 항목에서 OS 앱 알림 설정 화면 바로 열기(실패 시 알림센터 폴백)
  - 앱 버전 표기 BuildConfig 동적화(`APP_VERSION_NAME`, `APP_VERSION_CODE`)
  - 테마 영속화(DataStore) 유지
- Notifications:
  - 알림센터 화면 신설 (`feature/notifications`)
  - Home/Schedule/Money/Settings에서 알림센터 진입 연결
  - 일정/체크리스트/예산 초과 기반 알림 피드 구성
  - 로컬 리마인더 예약(Android/iOS `expect/actual`) 구현
  - 일정/체크리스트 수정/삭제 및 체크 완료 시 기존 예약 리마인더 자동 해제
- Tools:
  - 웨딩홀 비교 결과 공유(Top 결과 텍스트 공유) 지원
  - 즐겨찾기 우선 정렬 + 추천 점수 규칙 강화
- 테스트:
  - `commonTest` 대폭 확장 (도메인/usecase/flow + ViewModel 레이어) 및 Android 단위 테스트 103건 통과
  - `androidInstrumentedTest` UI smoke 테스트 11건 추가(Auth/Home/Guests/Money/Schedule/Settings/Notifications/Onboarding/Tools)

---

## 2) 이번 턴 주요 변경

### 2-1) 알림센터 + 로컬 리마인더

- 신규 feature:
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/notifications/`
- 플랫폼 알림 스케줄링:
  - Common expect: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/platform/LocalNotification.kt`
  - Android actual: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/androidMain/kotlin/kr/jm/weddingledger/core/platform/LocalNotification.android.kt`
  - iOS actual: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/iosMain/kotlin/kr/jm/weddingledger/core/platform/LocalNotification.ios.kt`
  - Android receiver: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/androidMain/kotlin/kr/jm/weddingledger/core/platform/WeddingLedgerReminderReceiver.kt`
- Android 설정:
  - `POST_NOTIFICATIONS` 권한 + 리시버 등록
  - 파일: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/androidMain/AndroidManifest.xml`

### 2-2) 네비게이션/설정 액션 연결

- 신규 라우트:
  - `notifications`, `settings/profile-edit`, `settings/account`, `settings/app-info`
  - 파일: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppRoutes.kt`
- `AppNavHost`에 알림센터/설정 서브화면 연결:
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppNavHost.kt`
- 설정 서브화면:
  - 프로필 수정: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/ProfileEditRoute.kt`
  - 계정 관리: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/AccountSettingsScreen.kt`
  - 앱 정보: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/AppInfoScreen.kt`

### 2-3) 테스트 보강

- `commonTest` 신규 테스트:
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonTest/kotlin/kr/jm/weddingledger/feature/guests/domain/usecase/UpdateGuestUseCaseTest.kt`
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonTest/kotlin/kr/jm/weddingledger/feature/schedule/domain/usecase/UpdateChecklistUseCaseTest.kt`
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonTest/kotlin/kr/jm/weddingledger/feature/notifications/domain/usecase/ScheduleReminderUseCaseTest.kt`
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonTest/kotlin/kr/jm/weddingledger/feature/auth/domain/usecase/ResendSignUpVerificationEmailUseCaseTest.kt`
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonTest/kotlin/kr/jm/weddingledger/feature/money/domain/usecase/ExpenseFilterPreferenceUseCaseTest.kt`

### 2-4) P1 기능 대량 반영

- Schedule:
  - 반복 일정(`EventRepeatRule`)을 `memo` 메타 인코딩 방식으로 저장/복원
  - 캘린더 탭 내 검색/정렬 컨트롤 추가
  - 반복 일정 occurrence 확장 로직 추가(다가오는 일정/캘린더 마커 반영)
- Guests:
  - 참석 확률 기반 예상 참석 인원/예상 축의금 계산 추가
  - 그룹별 리포트 카드 추가
- Tools:
  - 웨딩홀 비교 공유 액션 추가
  - 추천 정렬 점수 계산을 즐겨찾기 가중 포함으로 고도화

### 2-5) 1~5번 묶음 마무리 (2026-02-16)

- Auth 딥링크:
  - `weddingledger://auth` 리다이렉트 처리(`exchangeCodeForSession`, 토큰 fragment import, OTP verify fallback)
  - 공통 브리지: `AuthDeepLinkBridge`, `DeepLinkInteropKt.pushIncomingDeepLink(...)`
- Auth 미인증 UX:
  - `AuthUiState`에 `isEmailVerificationRequired`, `resendCooldownSeconds`, `resendAttemptCount` 추가
  - Sign-in 화면에서만 미인증 가이드 카드 표시
- Checklist 편집 정책:
  - 제목 2~40자, 메모 120자, D-Tag(`D-0..D-365`) 검증을 Route + UseCase 양쪽에 반영
  - 목표일 초기화 버튼 추가
- Money 지출 필터:
  - 기간 필터(`전체/이번 달/최근 3개월`) + 상태 필터(`전체/50만원 이상/메모 있음`) 추가
  - 현재 조합 저장/재적용 프리셋 UI 및 ViewModel 로직 반영

### 2-6) 이번 턴 추가 마무리 (2026-02-16)

- Money:
  - `feature/money`에 필터 상태 저장용 도메인/리포지토리/usecase/DataStore 계층 추가
  - 앱 재실행 후에도 카테고리/기간/상태/프리셋/예산초과 필터 복원
- Settings:
  - 알림 설정 클릭 시 OS 알림 설정 진입(`openAppNotificationSettings`)
  - 설정/앱정보 화면 버전 문자열 하드코딩 제거(BuildConfig 기반)
- Schedule:
  - 일정 반복 저장 시 `repeat_rule` 컬럼 우선 사용, 컬럼 미존재 환경은 자동 레거시 폴백
  - 일정/체크리스트 변경 시 연관 리마인더 ID 자동 취소하여 stale 예약 방지
- 테스트:
  - `ExpenseFilterPreferenceUseCaseTest` 추가(조회/저장 유즈케이스 검증)

### 2-7) 테스트 코드 완성 작업 (2026-02-19)

- 공통 테스트 인프라/설정
  - `composeApp/build.gradle.kts`
    - `commonTest`에 `kotlinx.coroutines.test` 의존성 추가
    - Android instrumented test용 `compose.uiTestJUnit4`, `androidx.test.ext:junit`, `espresso-core` 의존성 추가
    - `testInstrumentationRunner` 유지
  - `gradle/libs.versions.toml`
    - `kotlinx-coroutines-test` alias 추가
- 실패 테스트 수정
  - `AuthViewModelTest`: resend cooldown 코루틴 종료까지 advance하여 `Dispatchers.Main` 누수성 실패 제거
  - `GuestsViewModelTest`: 필터 개수 기대값을 실제 정책(전체/신랑측/신부측/공통)과 일치하도록 수정
  - `ScheduleViewModelTest`: 다가오는 일정 정렬 정책(오늘 일정 우선) 반영
- UI smoke 테스트 추가
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/androidInstrumentedTest/kotlin/kr/jm/weddingledger/ui/ScreenSmokeUiTest.kt`
  - 주요 화면 액션 디스패치 검증 11건
    - Auth, Home, Guests, Money, Schedule, Settings, NotificationCenter, ProfileSetup, InviteJoin, ValuesSurvey, VenueComparison

### 2-8) 키보드 UX 보강 + 딥링크 안내 문구 (2026-02-24)

- iOS 전역 키보드 dismiss:
  - 빈 영역 탭 시 `endEditing(true)`로 키보드 닫힘
  - 적용 파일: `/Users/mackim/AndroidStudioProjects/WeddingLedger/iosApp/iosApp/ContentView.swift`
- 키보드 열림 시 CTA 정책 통일(고정 CTA -> 스크롤 CTA):
  - 회원가입: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthScreen.kt`
  - 온보딩 프로필: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/ProfileSetupScreen.kt`
- 딥링크 처리 결과 메시지 개선:
  - 성공 시 안내 문구(`이메일 인증 완료`, `비밀번호 재설정 링크 확인`) 표시
  - 실패 시 오류 배너 유지
  - 적용 파일: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/App.kt`

### 2-9) 온보딩 화면 플로우 단순화 (2026-02-25)

- 신규 온보딩 단계 추가:
  - `OnboardingStep.EntryChoice`
  - `OnboardingStep.InviteJoin`
  - 파일: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/domain/model/OnboardingModels.kt`
- 화면 구조 변경:
  - `OnboardingEntryScreen` 선택지를 `혼자 시작하기` / `공유 코드로 시작하기`로 명확화
  - `InviteJoinScreen`에서 닉네임 + 공유 코드 입력으로 파트너 연결
  - 온보딩 필수 단계에서 `InviteShareScreen` 제거(초대 코드 공유는 Settings에서 선택적으로 수행)
  - `PartnerConnectScreen` 제거 유지
- ViewModel 플로우 변경:
  - `decideStep`을 `EntryChoice` 중심으로 단순화(`onboardingCompleted=true`만 `Done`)
  - 시작 경로에서 프로필 저장 성공 시 `InviteShare`로 가지 않고 `finishOnboarding()`로 즉시 완료
  - 공유 코드 경로 연결 시 닉네임 동기화(`UpdateNicknameUseCase`) 후 `join_wedding` RPC 호출
  - 코드 정규화/검증(`W-1234`) 및 에러 메시지 정교화
  - 관련 파일:
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/OnboardingContract.kt`
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/OnboardingRoute.kt`
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/OnboardingViewModel.kt`
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/data/repository/OnboardingRepositoryImpl.kt`

### 2-10) 키보드 이슈 원인군 일괄 대응 (2026-02-24)

- 공통 키보드 정책 유틸 추가:
  - `rememberImeVisible()`, `keyboardAwareBottomPadding(...)`
  - 파일: `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/ui/keyboard/KeyboardAware.kt`
- 입력 화면 공통 적용:
  - 로그인/회원가입/온보딩/프로필수정 화면에서 IME 열림 기준 패딩/정렬 정책 통일
  - 적용 파일:
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthScreen.kt`
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/ProfileSetupScreen.kt`
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/InviteJoinScreen.kt`
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/ProfileEditScreen.kt`
- 입력 바텀시트 공통 적용:
  - 하단 CTA 영역에 `imePadding()` 적용해 키보드 위로 버튼 노출 보장
  - 적용 파일:
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/presentation/AddGuestBottomSheet.kt`
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/AddBudgetItemBottomSheet.kt`
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/AddExpenseBottomSheet.kt`
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/AddChecklistBottomSheet.kt`
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/AddScheduleEventBottomSheet.kt`
  - iOS 호스트 보정:
    - `ComposeView().ignoresSafeArea(.keyboard, edges: .bottom)` 적용
    - 탭 dismiss 제스처를 `UIGestureRecognizerDelegate`로 제어해 첫 포커스 탭 간섭 제거
    - 파일: `/Users/mackim/AndroidStudioProjects/WeddingLedger/iosApp/iosApp/ContentView.swift`

### 2-11) 홈 권한 요청/리프레시/테마 분기 정리 (2026-02-25)

- 로그인/회원가입/온보딩 화면 다크모드 분기 제거:
  - 대상: `AuthScreen`, `OnboardingEntryScreen`, `ProfileSetupScreen`, `InviteJoinScreen`, `InviteShareScreen`
  - 목적: 분기 없는 단일 UI 스타일로 일관성 확보
- 탭 새로고침(Pull-to-refresh) 확장:
  - `홈/일정/하객` 탭에 pull-to-refresh 적용
  - `도구` 탭은 제외, `돈` 탭은 기존 동작 유지
- 알림 권한 요청 시점 변경:
  - 로그인 + 온보딩 완료 후 **첫 홈 진입 시 1회** 권한 요청
  - 재요청 방지 플래그(DataStore) 추가
  - 공용 저장소:
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/settings/data/NotificationPermissionPromptStorage.kt`
  - Android actual:
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/androidMain/kotlin/kr/jm/weddingledger/core/settings/data/NotificationPermissionPromptStorage.android.kt`
  - iOS actual:
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/iosMain/kotlin/kr/jm/weddingledger/core/settings/data/NotificationPermissionPromptStorage.ios.kt`
  - 적용 라우트:
    - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/home/presentation/HomeRoute.kt`
- 파트너 연결 즉시 알림 현황:
  - `join_wedding` 성공 시점에 즉시 알림(푸시/로컬) 트리거는 현재 없음
  - 알림센터 피드는 일정/체크리스트/예산 초과 중심
  - 향후 필요 시 서버 기반 푸시(FCM/APNs + 디바이스 토큰 + 조인 이벤트 발송) 설계 필요

### 2-12) 초대 코드 포맷 6자리 숫자 통일 (2026-02-28)

- 앱 입력/검증/생성 로직에서 `W-####` 제거 후 `######`(6자리 숫자)로 통일
- 온보딩/설정/스모크 테스트/단위 테스트 fixture를 6자리 숫자 기준으로 갱신
- Supabase 마이그레이션 적용:
  - 버전: `20260228021452` (`invite_code_six_digit_numeric`)
  - 내용: legacy 코드 정규화, `profiles.invite_code` 포맷 제약(6자리 숫자), `join_wedding` RPC 숫자 코드 처리

### 2-13) 소셜 로그인 정책 전환 + Kakao 네이티브 마무리 (2026-03-04)

- 로그인 정책:
  - `Email/Password` 회원가입/로그인 비활성
  - `Google + Kakao` 우선, `Email OTP` 보조
  - Apple은 계정 준비 전까지 UI 비활성 상태 유지
- Kakao 구현:
  - iOS: `KakaoSDKUser` 네이티브 로그인 + 에러 상세 파싱 + Supabase ID Token 연동
  - Android: Kakao SDK 네이티브 로그인 + Talk 실패 시 Account 로그인 폴백 + 에러 상세 파싱(`ClientError/AuthError/ApiError`)
  - 공통: `idToken + accessToken`을 Supabase `signInWith(IDToken, provider=Kakao)`로 교환
- 이슈/해결:
  - iOS `invalid_request: IOS bundleid validation failed` 해결 (Kakao iOS Bundle ID 정합성)
  - iOS `Unacceptable audience in id_token` 해결 (Supabase Kakao Provider 설정과 토큰 `aud` 정합성)
  - Android generic 에러 문구 문제 해결 (실제 원인 표시되도록 파서 확장)
- 현재 상태:
  - Kakao 로그인 iOS/Android 모두 성공 확인
  - 다음 단계는 Apple provider 설정 및 검증

### 2-14) Money/탑바 UX 개선 + 계정 전환 캐시 안정화 (2026-03-05)

- Money 필수 UX/데이터 확장:
  - 지출 입력에 `업체명(선택)` 필드 추가 및 목록 서브텍스트(`날짜 · 업체명`) 표시
  - 결제 수단에 `공동계좌` 추가
  - `공동계좌` 선택 시 결제자 입력 숨김 + 내부 결제자 `공동` 자동 세팅
  - 카테고리 관리 바텀시트 추가(사용자 카테고리 추가/수정)
- DB 반영:
  - `public.expenses.vendor_name text` 컬럼 추가 적용
- 상단 헤더 통일:
  - 홈/돈/일정/하객 상단 액션을 `알림 아이콘 1개`로 통일
  - 공용 컴포넌트 `WeddingLogTopBar` 신설 후 4개 탭 적용
- 계정 전환 안정화:
  - `cachedWeddingId` 단독 캐시를 `cachedUserId + cachedWeddingId` 방식으로 교체
  - 적용 대상: Money/Guests/Schedule(Checklist/Event)/Tools Repository
  - 효과: 로그아웃/계정 전환 시 이전 weddingId 재사용 방지

---

## 3) Supabase 상태 (MCP 재확인, 2026-02-16)

### 적용 완료

- 이전 적용:
  - 함수 `search_path` 고정 (`set_updated_at`, `my_wedding_id`)
  - FK 인덱스 추가 (`budget_items.category_id`, `expenses.budget_item_id`, `venue_favorites.venue_id`)
- 이번 적용:
  - RLS 정책 내 `auth.uid()`를 `(select auth.uid())` 패턴으로 일괄 변경
  - migration name: `optimize_rls_auth_uid_initplan`

### 현재 남은 advisory

- Security WARN:
  - `auth_leaked_password_protection` (Auth 설정에서 직접 Enable 필요)
  - 문서: https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection
- Performance WARN은 해소됨 (남은 것은 `unused_index` INFO 항목)

### Auth 대시보드 권장 체크리스트

- `Authentication > URL Configuration`
  - Redirect URL allow list에 `weddingledger://auth` 포함
- `Authentication > Providers > Email`
  - `Confirm email` 활성화
  - `Leaked password protection` 활성화

---

## 4) 최근 검증 결과

성공(2026-03-04 기준):

```bash
./gradlew :composeApp:assembleDebug
# iOS host
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

성공(2026-03-05 기준):

```bash
./gradlew :composeApp:assembleDebug
```

수동 QA(2026-03-04):
- iOS Kakao 로그인 성공
- Android Kakao 로그인 성공

성공(2026-02-25 기준):

```bash
./gradlew :composeApp:testDebugUnitTest --tests '*OnboardingViewModelTest'
./gradlew :composeApp:assembleDebug
```

성공(2026-02-19 기준):

```bash
./gradlew :composeApp:testDebugUnitTest --rerun-tasks
# 결과: 103 tests, 0 failures, 0 errors

./gradlew :composeApp:compileTestKotlinIosSimulatorArm64 --rerun-tasks

./gradlew :composeApp:compileDebugAndroidTestKotlinAndroid

./gradlew :composeApp:assembleDebug
```

실행 제약:

```bash
./gradlew :composeApp:connectedDebugAndroidTest
# 실패 원인: No connected devices
```

---

## 5) 남은 백로그 (우선순위)

### P1

- Auth:
  - 실기기에서 이메일 인증 딥링크 진입(메일 앱 -> 앱 복귀) QA
- Apple 로그인:
  - Apple Developer 계정 준비 후 Supabase Apple provider 설정 및 실기기 로그인 검증
- Android UI 테스트 실기기/에뮬레이터 1회 실행 검증
  - `./gradlew :composeApp:connectedDebugAndroidTest`

### P2

- 실기기 QA(Android/iOS 알림 권한/로컬 리마인더 실제 수신 확인)
- iOS host 앱 빌드/실행 재확인(Xcode) 및 알림 권한 동작 점검

---

## 6) 참고 핵심 파일

- 네비게이션:
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppNavHost.kt`
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppRoutes.kt`
- 알림센터:
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/notifications/presentation/NotificationCenterRoute.kt`
- 설정:
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/SettingsRoute.kt`
  - `/Users/mackim/AndroidStudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/ProfileEditViewModel.kt`
