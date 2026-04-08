# WeddingLedger

Kotlin Multiplatform + Compose Multiplatform 기반의 결혼 준비/가계부 앱입니다.  
Android/iOS 공용 코드로 홈 결산, 가계부, 일정, 하객, 온보딩/연결 코드, 알림센터 기능을 제공합니다.

## 기술 스택

- Kotlin Multiplatform (KMP)
- Compose Multiplatform (CMP)
- Koin (DI)
- Ktor Client
- Supabase (Auth + Postgrest + Realtime)
- Room/SQLite + DataStore

## 프로젝트 구조

- Android 앱 셸: `androidApp/`
- 공용 코드: `composeApp/src/commonMain/kotlin`
- Android 전용: `composeApp/src/androidMain/kotlin`
- iOS 전용: `composeApp/src/iosMain/kotlin`
- iOS 호스트(Swift): `iosApp/iosApp`

아키텍처 규칙은 [AGENTS.md](./AGENTS.md)를 따릅니다.

## 로컬 설정

`local.properties`에 최소 아래 키가 필요합니다.

```properties
SUPABASE_URL=...
SUPABASE_ANON_KEY=...
FIREBASE_ANDROID_APPLICATION_ID=...
FIREBASE_PROJECT_ID=...
FIREBASE_API_KEY=...
FIREBASE_GCM_SENDER_ID=...
KAKAO_NATIVE_APP_KEY=...
TERMS_OF_SERVICE_URL=https://example.com/terms
PRIVACY_POLICY_URL=https://example.com/privacy
ADMOB_ANDROID_APP_ID=ca-app-pub-3940256099942544~3347511713
ADMOB_ANDROID_BANNER_UNIT_ID=ca-app-pub-3940256099942544/9214589741
ADMOB_IOS_APP_ID=ca-app-pub-3940256099942544~1458002511
ADMOB_IOS_BANNER_UNIT_ID=ca-app-pub-3940256099942544/2435281174

# 소셜 로그인 노출/활성 토글 (기본값: Kakao/Google=true, Apple=false)
ENABLE_KAKAO_LOGIN=true
ENABLE_GOOGLE_LOGIN=true
ENABLE_APPLE_LOGIN=false
ENABLE_TEST_AD_BANNER=false
```

- `KAKAO_NATIVE_APP_KEY`는 Kakao Developers 콘솔의 `네이티브 앱 키`를 사용합니다.
- `ADMOB_ANDROID_APP_ID`는 Android release 빌드용 AdMob App ID입니다. debug 빌드는 공식 테스트 App ID를 사용합니다.
- `ADMOB_ANDROID_BANNER_UNIT_ID`는 Android release 빌드용 배너 광고 단위 ID입니다. debug 빌드는 공식 테스트 배너 unit ID를 사용합니다.
- `ADMOB_IOS_APP_ID`는 iOS release 빌드용 AdMob App ID입니다. debug 빌드는 공식 테스트 App ID를 사용합니다.
- `ADMOB_IOS_BANNER_UNIT_ID`는 iOS release 빌드용 배너 광고 단위 ID입니다. debug 빌드는 공식 테스트 배너 unit ID를 사용합니다.
- `TERMS_OF_SERVICE_URL`, `PRIVACY_POLICY_URL`은 로그인 화면에서 여는 실제 약관/개인정보 처리방침 문서 URL입니다.
- 약관 동의는 로그인 화면에서 최초 1회만 받고, 현재 약관 버전 동의 상태를 DataStore에 저장합니다.
- 파트너 활동 푸시까지 쓰려면 Android Firebase 앱의 `Application ID`, `Project ID`, `Web API Key`, `Sender ID`를 `local.properties`에 넣어야 합니다.
- iOS 파트너 활동 푸시는 `iosApp/Configuration/Config.local.xcconfig`에 아래 값을 채워야 동작합니다.
  - `FIREBASE_IOS_GOOGLE_APP_ID`
  - `FIREBASE_PROJECT_ID`
  - `FIREBASE_API_KEY`
  - `FIREBASE_GCM_SENDER_ID`
- `iosApp/Configuration/Config.xcconfig`는 로컬 전용 `Config.local.xcconfig`를 include 하도록 정리되어 있습니다.
- Supabase Edge Function `send-partner-activity-push`가 실제 OS push까지 보내려면 Supabase 프로젝트 secret `FCM_SERVICE_ACCOUNT_JSON`도 별도로 설정해야 합니다.
- Android 카카오 로그인은 네이티브 SDK + Supabase `signInWithIdToken` 경로를 사용합니다.
- iOS 카카오 로그인도 네이티브 SDK + Supabase `signInWithIdToken` 경로를 사용합니다.
- `ENABLE_APPLE_LOGIN=false`면 앱에서 Apple 로그인 버튼/동작이 비활성화됩니다.
- `ENABLE_TEST_AD_BANNER=true`면 Android/iOS 메인 탭 화면 하단에 배너를 표시합니다. debug 빌드는 테스트 광고, release 빌드는 실제 광고 ID를 사용합니다.
- iOS Apple 로그인은 네이티브 `AuthenticationServices` + Supabase `signInWith(IDToken)` 경로를 사용합니다.
- Apple 로그인을 활성화하려면 Supabase Apple Provider `Client IDs`에 아래 두 값을 함께 넣어야 합니다.
  - `kr.jm.weddingledger.service`
  - `kr.jm.weddingledger.WeddingLedger`
- iOS 타깃은 Xcode에서 `Sign In with Apple`, `Push Notifications`, `Background Modes > Remote notifications` capability가 켜져 있어야 합니다.
- Apple Developer 유료 계정 / Supabase Apple Provider / Xcode capability 설정이 끝나면 `ENABLE_APPLE_LOGIN=true`로 바로 활성화할 수 있습니다.
- 회원탈퇴 v1은 새 Supabase Edge Function `delete-account`를 사용합니다.
  - Apple 계정 revoke까지 포함하려면 Supabase secret `APPLE_TEAM_ID`, `APPLE_KEY_ID`, `APPLE_CLIENT_ID`, `APPLE_PRIVATE_KEY`가 설정돼 있어야 합니다.

## 빌드/테스트

```bash
cd /Users/jm/StudioProjects/WeddingLedger

# Android debug 빌드
./gradlew :androidApp:assembleDebug

# Android instrumented test assemble
./gradlew :composeApp:assembleAndroidTest

# Android unit test
./gradlew :androidApp:testDebugUnitTest

# KMP shared test
./gradlew :composeApp:allTests

# iOS 공용 framework 링크 확인
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Android Studio 업데이트 이후 간헐적으로 Gradle sync에서 `convertPbxprojToJson` / `plutil` 오류가 날 수 있습니다.
그 경우 Finder/Launcher 대신 아래처럼 터미널에서 Android Studio를 실행하면 정상화된 사례가 있습니다.

```bash
"/Applications/Android Studio.app/Contents/MacOS/studio" /Users/jm/StudioProjects/WeddingLedger
```

## 인증 딥링크

- 앱 딥링크 스킴: `weddingledger://auth`
- Android: `AndroidManifest.xml`의 `intent-filter` 등록
- iOS: `Info.plist` `CFBundleURLTypes` + `iOSApp.swift` `onOpenURL`

Supabase 대시보드 권장값:

- `Authentication > URL Configuration`
  - `Site URL`: `weddingledger://auth`
  - `Redirect URLs`: `weddingledger://auth`

## 최근 UX 반영 (2026-02-25)

- 온보딩 플로우 개편:
  - 첫 진입 화면을 `혼자 시작하기 / 공유 코드로 시작하기` 분기형으로 변경
  - `혼자 시작하기` 경로: 프로필 입력(닉네임/역할/예식일) 후 즉시 온보딩 완료
  - `공유 코드로 시작하기` 경로: 닉네임 + 공유 코드 입력으로 파트너 연결
  - 공유 코드는 `6자리 숫자` 형식으로 통일(`W-` 접두어 제거)
  - 온보딩의 초대 코드 공유 단계를 필수 경로에서 제거(코드 공유는 설정 화면에서 선택적으로 수행)
- iOS 키보드 대응 보강:
  - SwiftUI 호스트의 키보드 자동 safe-area 회피를 비활성화해 이중 보정 제거
  - 전역 탭 dismiss는 키보드가 보일 때만 동작하도록 제한(첫 포커스 탭 간섭 방지)
- 회원가입/온보딩 프로필: 키보드 열림 시 고정 하단 CTA 대신 스크롤 영역 CTA 사용
- 입력 중심 화면 공통 정책:
  - `core/ui/keyboard/KeyboardAware.kt`로 IME 표시 상태/하단 패딩 계산 통일
  - 주요 입력 바텀시트 하단 액션 영역에 `imePadding()` 적용
- 딥링크 처리 성공/실패 안내 배너를 로그인 화면에 표시
- 다크모드 분기 정리:
  - 로그인/회원가입/온보딩 화면의 다크모드 분기를 제거하고 단일 스타일 기준으로 통일
- 탭별 새로고침 UX 정리:
  - `홈/일정/하객` 탭에 아래로 당겨 새로고침(Pull-to-refresh) 적용
  - `돈` 탭은 기존 새로고침 유지, `도구` 탭은 제외
- 알림 권한 요청 타이밍 변경:
  - 로그인/온보딩 완료 후 첫 홈 진입 시 알림 권한을 1회 요청하도록 적용
  - 재진입 시 반복 팝업 방지를 위해 DataStore 플래그로 요청 이력 저장
- 파트너 연결 알림 현황:
  - 현재 알림센터 피드는 일정/체크리스트/예산 초과 기반이며, 파트너 연결 즉시 알림은 미구현 상태

## 최근 제품 개편 (2026-03-15)

- Home:
  - 결산 대시보드 구조로 재편
  - 총 예산/총 지출/잔액, 신랑/신부/공동 분담, 카테고리 결산, 다가오는 일정 표시
  - 큰 금액에서도 깨지지 않도록 결산 카드의 사용률 칩을 금액 아래 별도 행으로 이동
  - 중간 `지출 추가하기 / 일정 확인하기` 카드 및 홈 FAB 제거
- 가계부:
  - 하단 탭명을 `돈`에서 `가계부`로 변경
  - `예산 / 지출` 분리 구조로 재배치
  - 카테고리 기반 예산/지출 입력 흐름으로 단순화
  - 지출은 `카테고리 필수`, `예산 항목 선택` 구조 지원
  - `남은 결제`, `분담 요약` 추가
- 일정:
  - 선택 날짜 일정, 다가오는 일정, 마감 임박 체크리스트를 한 화면에서 항상 보이도록 개편
- 하객:
  - `예상 하객 | 축의대` 이중 구조로 재설계
  - 보증인원, 예상 하객 수(꼭 옴/애매/안 옴 + 동반 인원), 실제 참석/실제 축의금 기록 지원
- 알림:
  - 알림센터를 `새 활동 / 리마인더` 2섹션으로 확장
  - 지출 추가 / 일정 추가 시 파트너 활동 기록 생성
  - Android/iOS 원격 푸시 토큰 저장소 및 Supabase Edge Function 발송 경로 추가
- Supabase:
  - 리포지토리 SQL 파일 생성 없이 원격 DB에 직접 migration 적용
  - `budget_items.stage`, `budget_items.due_date`
  - `guests.attendance_likelihood`, `guests.companion_count`, `guests.actual_attended`, `guests.actual_gift_money`
  - `guest_settings.guaranteed_guest_count`
  - `expenses.created_by`, `schedule_events.created_by`
  - `device_push_tokens`, `partner_activity_notifications`
  - `join_wedding`는 원격 DB 함수에서 제3자 합류를 차단하도록 직접 보강

## 최근 안정화/푸시 반영 (2026-04-08)

- Android 파트너 활동 push
  - 실기기에서 FCM 토큰 등록, Edge Function 발송, foreground 수신, 시스템 알림 표시까지 확인
  - Android는 `WL_SUPABASE` 로그 태그로 다음 흐름을 추적할 수 있습니다.
    - `remote_push_token_fetch_success`
    - `push_token_synced`
    - `partner_activity_dispatch_start/success`
    - `onMessageReceived`
    - `notification_displayed`
- 일정 저장 안정화
  - `schedule_events` insert 반환 형식 변화에 덜 민감하도록 client-generated UUID + 안전 파싱 방식으로 변경
  - 일정 추가 시트의 제목/장소 placeholder 예시 제거
- iOS 파트너 활동 push
  - Apple Developer App ID / APNs key / Firebase APNs 업로드 / Xcode capability 설정 후 실기기 push 수신까지 확인
  - iOS도 `push_token_synced platform=ios` 이후 실제 시스템 알림 표시 확인
- iOS Apple 로그인
  - 브라우저 OAuth 교환 대신 네이티브 `AuthenticationServices` + Supabase `signInWith(IDToken)` 경로로 마무리
  - Kakao 딥링크 처리와 충돌하지 않도록 iOS 호스트의 URL 라우팅을 안전화
- Push 토큰 계정 전환 보강
  - `manage-device-push-token` Edge Function을 추가해 `installation_id` 기준 stale row 정리 후 `sync/deactivate` 하도록 변경
  - 남은 검증은 같은 기기에서 계정 A/B 전환 시 이전 계정 push가 새지 않는지 QA 확인입니다.
- 저장 UX / 알림 읽음 처리 개선
  - 가계부/일정/하객 저장 바텀시트는 저장 성공 시에만 닫히고, 실패 시 입력값을 그대로 유지합니다.
  - 알림센터 `새 활동`은 fetch 시 자동 읽음 처리하지 않고, 사용자가 활동 카드를 눌렀을 때만 read 처리합니다.
- 가계부 안정화 / 문구 정리
  - `expenses`는 예산 항목을 선택하지 않은 지출도 저장 가능하도록 RLS를 조정했습니다.
  - 지출 저장은 client-generated UUID + plain insert 방식으로 바꿔 `JsonDecodingException` 재발을 줄였습니다.
  - 지출 필터 기본 기간을 `전체 기간`으로 변경했습니다.
  - 예산 화면의 `목표` 문구를 `예산`으로 통일했습니다.
- Tools / 샘플 데이터 정리
  - `웨딩홀 비교`, `가치관 설문`은 현재 클릭 시 `준비 중인 기능이에요.` 안내를 보여줍니다.
  - shared 공용 상태의 `sample()` 더미 데이터를 제거하고, smoke test는 실제 최소 상태 생성 방식으로 정리했습니다.
- AdMob 배너 1차 구현
  - Android/iOS 메인 탭 하단에 공통 배너 슬롯을 추가했습니다.
  - debug 빌드는 공식 테스트 App ID / 배너 unit ID를 사용합니다.
  - release 빌드는 `local.properties`와 `iosApp/Configuration/Config.local.xcconfig`의 실제 광고 ID를 사용합니다.
- 회원탈퇴 v1
  - `설정 > 계정 관리`에서 회원탈퇴를 시작할 수 있습니다.
  - 일반 계정은 즉시 탈퇴를 요청하고, Apple 계정은 탈퇴 직전에 네이티브 재인증으로 `authorizationCode`를 다시 받아 서버에 전달합니다.
  - Supabase Edge Function `delete-account`가 `device_push_tokens`, `partner_activity_notifications`, `profiles`, `auth.users`를 순차 삭제합니다.
  - Apple 계정은 서버에서 사용자 provider를 직접 판별해 revoke를 강제하고, `auth.users` 삭제 실패 시 public row를 복구하도록 보강했습니다.
- Build 메모
  - clean 후 사라지던 `BuildConfig.kt` 생성 문제를 `composeApp/build.gradle.kts`에서 보강했습니다.
  - 현재 `generateBuildConfig`는 configuration cache 경고가 남지만 Android/iOS 빌드는 성공합니다.
- 참고
  - iOS에서 Firebase `GoogleService-Info.plist` 관련 경고 로그가 남을 수 있으나, 현재 기준 FCM 토큰 발급 / Supabase 토큰 등록 / 실제 push 수신은 정상 동작 확인

## QA

- 통합 QA 체크리스트는 [qa.md](./qa.md)에서 확인할 수 있습니다.

## 최근 기술 반영 (2026-03-08)

- Android 프로젝트 구조 정리:
  - `composeApp`를 Android-KMP shared library로 전환
  - `androidApp` 모듈을 추가해 `MainActivity`, `Application`, Android `Manifest`, launcher 리소스를 분리
- Auth/Onboarding 안정성 보강:
  - Auth/Onboarding 에러 분기를 문자열 기반에서 타입 기반으로 정리
  - 공통 Supabase repository 지원 로직(`wedding_id` 조회/로그) 추출
- 테스트 보강:
  - `AuthResult`, `OnboardingViewModel`, `WeddingIdResolver` 중심 회귀 테스트 추가
- 인증 화면 정책:
  - 이메일/OTP 로그인 UI는 주석 처리로 숨김
  - 내부 use case / repository 구현은 유지되어 필요 시 UI만 복구 가능

## 인수인계

상세 변경 이력/백로그/검증 결과는 [HANDOVER.md](./HANDOVER.md)에서 확인할 수 있습니다.
