# WeddingLedger 인수인계서 (KMP + Compose Multiplatform)

작성일: 2026-02-16  
워크스페이스: `/Users/jm/StudioProjects/WeddingLedger`

---

## 0) 퀵스타트

1) 전역 규칙 확인: `/Users/jm/StudioProjects/WeddingLedger/AGENTS.md`  
2) Supabase 키 확인: `/Users/jm/StudioProjects/WeddingLedger/local.properties`  
3) 빌드/테스트:

```bash
cd /Users/jm/StudioProjects/WeddingLedger
./gradlew :composeApp:compileDebugKotlinAndroid -x test
./gradlew :composeApp:compileKotlinIosSimulatorArm64 -x test
./gradlew :composeApp:allTests
```

---

## 1) 현재 상태 요약

- 아키텍처: Feature-first + Clean Architecture + MVVM 유지
- Auth/Onboarding: `join_wedding` RPC 포함 실데이터 동작
  - 이메일 인증 딥링크 E2E 연결 완료(Android `intent-filter` + iOS `onOpenURL` + KMP bridge)
  - 로그인 미인증 전용 안내 카드 + 재전송 60초 쿨다운 + 최대 5회 제한 추가
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

---

## 2) 이번 턴 주요 변경

### 2-1) 알림센터 + 로컬 리마인더

- 신규 feature:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/notifications/`
- 플랫폼 알림 스케줄링:
  - Common expect: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/platform/LocalNotification.kt`
  - Android actual: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/androidMain/kotlin/kr/jm/weddingledger/core/platform/LocalNotification.android.kt`
  - iOS actual: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/iosMain/kotlin/kr/jm/weddingledger/core/platform/LocalNotification.ios.kt`
  - Android receiver: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/androidMain/kotlin/kr/jm/weddingledger/core/platform/WeddingLedgerReminderReceiver.kt`
- Android 설정:
  - `POST_NOTIFICATIONS` 권한 + 리시버 등록
  - 파일: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/androidMain/AndroidManifest.xml`

### 2-2) 네비게이션/설정 액션 연결

- 신규 라우트:
  - `notifications`, `settings/profile-edit`, `settings/account`, `settings/app-info`
  - 파일: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppRoutes.kt`
- `AppNavHost`에 알림센터/설정 서브화면 연결:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppNavHost.kt`
- 설정 서브화면:
  - 프로필 수정: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/ProfileEditRoute.kt`
  - 계정 관리: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/AccountSettingsScreen.kt`
  - 앱 정보: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/AppInfoScreen.kt`

### 2-3) 테스트 보강

- `commonTest` 신규 테스트:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonTest/kotlin/kr/jm/weddingledger/feature/guests/domain/usecase/UpdateGuestUseCaseTest.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonTest/kotlin/kr/jm/weddingledger/feature/schedule/domain/usecase/UpdateChecklistUseCaseTest.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonTest/kotlin/kr/jm/weddingledger/feature/notifications/domain/usecase/ScheduleReminderUseCaseTest.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonTest/kotlin/kr/jm/weddingledger/feature/auth/domain/usecase/ResendSignUpVerificationEmailUseCaseTest.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonTest/kotlin/kr/jm/weddingledger/feature/money/domain/usecase/ExpenseFilterPreferenceUseCaseTest.kt`

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

성공:

```bash
./gradlew :composeApp:compileDebugKotlinAndroid -x test
./gradlew :composeApp:compileKotlinIosSimulatorArm64 -x test
./gradlew :composeApp:allTests
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -configuration Debug build
```

---

## 5) 남은 백로그 (우선순위)

### P1

- Auth:
  - 실기기에서 이메일 인증 딥링크 진입(메일 앱 -> 앱 복귀) QA

### P2

- 실기기 QA(Android/iOS 알림 권한/로컬 리마인더 실제 수신 확인)
- ViewModel 단위 테스트 추가 확장(특히 Schedule/Guests 화면 상태 계산 및 알림 동기화 경로)

---

## 6) 참고 핵심 파일

- 네비게이션:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppNavHost.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppRoutes.kt`
- 알림센터:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/notifications/presentation/NotificationCenterRoute.kt`
- 설정:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/SettingsRoute.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/ProfileEditViewModel.kt`
