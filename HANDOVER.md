# WeddingLedger 인수인계서 (KMP + Compose Multiplatform)

작성일: 2026-02-15  
워크스페이스: `/Users/jm/StudioProjects/WeddingLedger`

이 문서는 “새로운 세션”이 이 파일만 읽고도 바로 작업을 이어갈 수 있도록, 현재 코드 상태/결정/구조/다음 작업을 최대한 구체적으로 정리한 인수인계서입니다.

---

## 0) 5분 퀵스타트 (새 세션)

1) 전역 규칙 확인: `/Users/jm/StudioProjects/WeddingLedger/AGENTS.md`  
2) Supabase 키 설정 확인(커밋 금지): `/Users/jm/StudioProjects/WeddingLedger/local.properties`  
3) Supabase `profiles` 온보딩 컬럼 적용: `/Users/jm/StudioProjects/WeddingLedger/supabase/profiles_onboarding.sql` 실행  
4) 빌드 확인:
```bash
cd /Users/jm/StudioProjects/WeddingLedger
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```
5) 앱 흐름 확인: 로그인 → (첫 로그인) 온보딩(S04-1/2) → 메인(바텀탭)

---

## 1) 프로젝트 목표 (요약)

- 앱 목적: 결혼 준비(예산/지출, 일정/체크리스트, 하객, 도구)를 **한글 UI**로 관리하는 “WeddingLog/WeddingLedger”
- 백엔드: Supabase (Auth/PostgREST/Realtime)
- 아키텍처: Feature-first + Clean Architecture + MVVM
- 화면 구성: 가능한 Stateless(= `Screen(state, onAction)`) 유지
- 통화/표기: KRW(₩)
- 글꼴: Pretendard

---

## 2) 전역 룰 (필수 준수)

전역 규칙은 `/Users/jm/StudioProjects/WeddingLedger/AGENTS.md`에 정의되어 있습니다.

핵심:
- Feature-first + Clean Architecture + MVVM
- Composable Screen은 가능하면 Stateless:
  - `*Route.kt`: 상태 보관/액션 처리/바텀시트 오픈 등 “상태ful” 로직
  - `*Screen.kt`: `state + onAction`만 받는 UI
- UI 텍스트는 한국어만
- Git 작업: 프로젝트 방침은 **`main` 브랜치에서만 작업**

---

## 3) 기술 스택 / 모듈 구조

- Kotlin Multiplatform + Compose Multiplatform
- 모듈:
  - `composeApp`: 공용 UI/로직(안드/ iOS 공유)
  - `iosApp`: iOS 호스트 앱
- DI: Koin
- 네트워크: Supabase-kt (Auth/PostgREST/Realtime)
- 날짜: `kotlinx-datetime`

Gradle:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/build.gradle.kts`
  - Money 탭 pull-to-refresh 용으로 `implementation(compose.material)` 추가됨(Compose Material2 pullrefresh 사용)

---

## 4) Supabase 설정 (중요)

### 4-1) 프로젝트 정보

현재 사용 중인 Supabase 프로젝트:
- Project Ref: `wmfzhnoxdmfniewgcmzu`
- URL: `https://wmfzhnoxdmfniewgcmzu.supabase.co`

### 4-2) 로컬 설정

Supabase URL/Anon Key는 `local.properties`에서 읽습니다.

- `/Users/jm/StudioProjects/WeddingLedger/local.properties`
  - `SUPABASE_URL=...`
  - `SUPABASE_ANON_KEY=...`

주의:
- `local.properties`는 **gitignore 대상**입니다(키 커밋/푸시 금지).
- 보안은 “키 숨기기”가 아니라 **RLS(행 수준 보안)**로 막는 게 핵심입니다.

BuildConfig 생성:
- 생성 위치(빌드 산출물): `composeApp/build/generated/buildconfig/kr/jm/weddingledger/core/config/BuildConfig.kt`
- Supabase Client 생성:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/network/SupabaseClient.kt`
- Koin 등록:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/di/CoreModule.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/di/AppModule.kt`

### 4-3) profiles 온보딩 컬럼 추가(SQL)

온보딩(S04-1/2) 기능이 `profiles`에 아래 컬럼을 사용합니다. Supabase SQL Editor에서 실행:
- `/Users/jm/StudioProjects/WeddingLedger/supabase/profiles_onboarding.sql`

추가 컬럼:
- `profiles.role` (text)
- `profiles.wedding_date` (date)
- `profiles.is_wedding_date_undecided` (boolean)
- `profiles.invite_code` (text)
- `profiles.onboarding_completed` (boolean)
- `invite_code` unique index(권장)

### 4-4) Money 관련 테이블

Money 탭은 Supabase 테이블을 직접 사용합니다:
- `categories` (wedding_id, name, is_default, ...)
- `budget_items` (wedding_id, category_id, name, target_amount, memo, ...)
- `expenses` (wedding_id, budget_item_id, amount, date, payment_method, payer, memo, ...)

중요:
- `supabase/money_tables.sql` 파일은 **의도적으로 레포에서 제거된 상태**입니다(복구/재생성하지 않기).
- 새 환경에서 테이블이 필요하면 Supabase Dashboard에서 스키마를 확인/내보내기 하거나, 별도 마이그레이션 체계를 도입하는 쪽이 안전합니다.

### 4-5) RLS 권장 방향(요약)

아래를 지키는 것이 목표입니다:
- `profiles`: 기본적으로 본인 row만 접근
- 나머지 주요 테이블: `wedding_id` 기준으로 커플 범위만 접근

주의(중요):
- “파트너 연결”은 현재 `invite_code`로 상대 profiles row를 조회합니다.
- `profiles`를 “본인 row만 select 가능”하게 RLS를 켜면, 이 조회가 막힙니다.

권장 해결:
- `join_wedding(invite_code text)` 같은 RPC(SECURITY DEFINER)를 만들어서,
  - 함수 내부에서 inviter wedding_id를 조회
  - 호출자(auth.uid())의 profiles.wedding_id를 업데이트
  - 외부에서는 profiles를 여전히 본인 row만 보게 유지

---

## 5) 앱 엔트리 / DI 초기화

Android:
- Application: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/androidMain/kotlin/kr/jm/weddingledger/WeddingLedgerApp.kt`
- Manifest: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/androidMain/AndroidManifest.xml`

iOS:
- Koin init: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/iosMain/kotlin/kr/jm/weddingledger/KoinInit.kt`
- VC entry: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/iosMain/kotlin/kr/jm/weddingledger/MainViewController.kt`

---

## 6) 디자인 시스템 (테마/폰트)

테마 정의:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/designsystem/WeddingLogTheme.kt`

테마 스타일:
- `WeddingLogThemeStyle.Minimal`
- `WeddingLogThemeStyle.Lovely`

Pretendard 폰트:
- 리소스: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/composeResources/font/pretendard_*.otf`
- 라이선스: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/composeResources/font/PRETENDARD_LICENSE.txt`

---

## 7) 네비게이션 구조

Bottom bar 목적지 enum:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/TopLevelDestination.kt`
  - Home / Money / Schedule / Guests / Tools

Bottom bar:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppBottomBar.kt`

NavHost:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppNavHost.kt`

앱 엔트리(App):
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/App.kt`
  - Auth 상태에 따라 `AuthRoute` 또는 `OnboardingGateRoute`로 분기

---

## 7-1) Auth 흐름 (Supabase)

- Auth 상태 관찰:
  - UseCase: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/domain/usecase/ObserveAuthStateUseCase.kt`
  - Model: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/domain/model/AuthState.kt`
- App 분기:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/App.kt`
  - `AuthState.Unauthenticated`이면 `AuthRoute`
  - `AuthState.Authenticated`이면 `OnboardingGateRoute`
- 로그아웃:
  - Settings 화면 → `onLogout` → `SignOutUseCase`

---

## 8) 구현 완료: 온보딩(S04-1 / S04-2)

목표:
- 회원가입 후 “첫 로그인” 유저에게만
  1) S04-1: 닉네임/역할/예식일 입력
  2) S04-2: 초대코드 공유/파트너 연결(또는 스킵)
  3) 메인 진입

게이트:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/OnboardingGateRoute.kt`

화면/로직:
- S04-1: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/ProfileSetupScreen.kt`
- S04-2: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/PartnerConnectScreen.kt`
- Route(날짜피커/클립보드): `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/OnboardingRoute.kt`
- ViewModel: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/OnboardingViewModel.kt`

프로필 매핑:
- 닉네임: `profiles.name`
- 역할: `profiles.role` (GROOM/BRIDE)
- 예식일: `profiles.wedding_date` 또는 `profiles.is_wedding_date_undecided=true`
- 초대코드: `profiles.invite_code` (예: `W-7249`)
- 완료 플래그: `profiles.onboarding_completed=true`

파트너 연결 방식:
- `invite_code`로 상대 profiles 조회 → 상대 `wedding_id`를 내 `wedding_id`로 설정
- (Best-effort) 상대가 입력한 예식일 정보를 복사해서 두 사람이 동일한 결혼일 기준으로 시작 가능하게 함

주의:
- “초대 코드 공유하기”는 현재 OS 공유 시트가 아니라 **클립보드 복사**로 구현됨(추후 개선 가능).
- RLS를 엄격하게 켜면 partner connect가 막힐 수 있음(4-5 참고).

---

## 9) 구현 완료: Money 탭(예산/지출)

현재 상태: Money 탭은 Supabase 연동 포함하여 **실사용 가능한 수준으로 CRUD가 완성**되어 있습니다.

기능 요약:
- 카테고리 seed(없으면 기본 6개 생성): categories
- 예산 항목: 추가/수정/삭제
- 지출: 추가/수정/삭제, 날짜 선택(Material3 DatePicker)
- 그룹핑/합산/잔액 계산
- Pull-to-refresh(아래로 당기면 새로고침)

주요 파일:
- Route/Screen/VM:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/MoneyRoute.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/MoneyScreen.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/MoneyViewModel.kt`
- 바텀시트:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/AddExpenseBottomSheet.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/AddBudgetItemBottomSheet.kt`
- Repository:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/data/repository/MoneyRepositoryImpl.kt`

메모(리팩터링 후보):
- `MoneyRepositoryImpl`이 `profiles` seed/조회용 DTO를 `feature/schedule`에서 import하고 있습니다.
  - 동작은 하지만 feature 간 결합이 생기므로, 추후 `core`로 공용 DTO를 올리는 정리가 좋습니다.

---

## 10) Schedule/Guests/Tools 현황 (요약)

Schedule:
- 체크리스트는 Supabase 연동 CRUD 일부 구현되어 있음(fetch/create/setChecked/delete)
  - Repository: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/data/repository/ChecklistRepositoryImpl.kt`
- 달력(S08)은 UI 중심이며 월 계산/실데이터 매핑은 확장 여지 있음

Guests:
- 현재는 UI 중심(샘플 상태)이며 Supabase 연동은 TODO

Tools/Settings:
- 설정에서 테마 변경 UI는 존재(영속화는 TODO)

---

## 11) 공용 UI 컴포넌트

바텀시트 핸들/헤더 공용화:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/ui/components/WeddingLogBottomSheet.kt`

---

## 12) 빌드/실행 방법

Android:
```bash
cd /Users/jm/StudioProjects/WeddingLedger
./gradlew :composeApp:assembleDebug
```

iOS(Kotlin 컴파일 확인):
```bash
cd /Users/jm/StudioProjects/WeddingLedger
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

---

## 13) 현재 남아있는 TODO (우선순위)

### (P0) Supabase RLS 정리 + “파트너 연결”을 RPC로 안전하게
- 목표: `profiles`는 본인 row만 select 가능하게 유지하면서도, 초대코드로 wedding_id 합치기 가능
- 작업:
  - RPC: `join_wedding(invite_code text)` 작성(SECURITY DEFINER)
  - 앱: `connectPartner()`가 profiles 직접 select 대신 RPC 호출로 전환
  - RLS 정책 재정의(테이블별 wedding_id 기반)

### (P0) 테마 설정 영속화(DataStore)
- 현재 `App.kt`에서 themeStyle을 `remember`로만 유지
- 목표: 앱 재실행 후에도 마지막 테마 유지

### (P1) Schedule 달력 로직
- 달력 그리드(월 계산) `kotlinx-datetime` 기반으로 구현
- 이벤트/디데이 실데이터 매핑

### (P1) Guests: DB/연동 + 비즈니스 로직
- guests 테이블 설계/연동
- Weighted Count/Expected Gift 합산 등 구현

### (P2) Onboarding UX 개선
- “초대 코드 공유하기”를 OS 공유 시트로 변경(Android/iOS)

### (P2) 경고/Deprecated 정리
- `Divider` -> `HorizontalDivider`, `TabRow` -> `Primary/SecondaryTabRow` 등
- `LocalClipboardManager` -> `LocalClipboard` 대응
- `Icons.AutoMirrored...` 적용

---

## 14) DB 스키마(참고)

초기 기획 기준(추후 Supabase SQL 작성/적용 필요):
- profiles(id UUID PK, email, name, wedding_id UUID, ... + onboarding columns)
- categories(id, wedding_id, name, is_default, ...)
- budget_items(id, wedding_id, category_id, name, target_amount, memo, ...)
- expenses(id, wedding_id, budget_item_id, amount, date, payment_method, payer, memo, image_url, ...)
- guests(id, wedding_id, name, group, relationship_score, expected_gift_money, is_attended, ...)
- checklists(id, wedding_id, title, d_day_tag, is_checked, target_date, memo, ...)
- venues(...)
- values_survey(...)

모든 주요 테이블은 `wedding_id`로 커플 데이터 공유(필터링/권한 설계 필요).

---

## 15) 작업 패턴(권장)

- `*Contract.kt`: UI state + action 정의
- `*Route.kt`: 상태 보관/액션 처리/바텀시트 오픈 등 “상태ful” 로직
- `*Screen.kt`: Stateless UI (`state + onAction`)

---

## 16) 다음 세션 체크리스트

1) `/Users/jm/StudioProjects/WeddingLedger/AGENTS.md` 확인  
2) `git branch --show-current`가 `main`인지 확인  
3) `/Users/jm/StudioProjects/WeddingLedger/local.properties` Supabase 키 설정 확인  
4) Supabase에 온보딩 컬럼 적용(미적용 시): `/Users/jm/StudioProjects/WeddingLedger/supabase/profiles_onboarding.sql`  
5) 빌드:
   - `./gradlew :composeApp:assembleDebug`
   - `./gradlew :composeApp:compileKotlinIosSimulatorArm64`

---

## 17) 이번 변경에 포함된 것(요약)

- Money 탭: CRUD(추가/수정/삭제), 날짜 선택, Pull-to-refresh
- 온보딩: S04-1(프로필/예식일) + S04-2(파트너 연결) + 첫 로그인 게이트
- Supabase: 온보딩용 profiles 컬럼 SQL 추가
- 문서: `HANDOFF.md` -> `HANDOVER.md`로 파일명 변경 + 내용 최신화

