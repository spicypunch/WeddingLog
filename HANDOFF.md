# WeddingLedger 인수인계서 (KMP + Compose Multiplatform)

작성일: 2026-02-14  
워크스페이스: `/Users/jm/StudioProjects/WeddingLedger`

이 문서는 "새로운 세션"이 이 파일만 읽고도 바로 작업을 이어갈 수 있도록, 현재 코드 상태/결정/구조/다음 작업을 최대한 구체적으로 정리한 인수인계서입니다.

---

## 1) 프로젝트 목표 (요약)

- 앱 목적: 결혼 준비(예산/지출, 일정/체크리스트, 하객, 도구)를 한글 UI로 관리하는 "WeddingLog/WeddingLedger" 앱
- 백엔드: Supabase (Auth/PostgREST/Realtime) 연동을 전제로 진행
- UI: 미니멀/러블리 2가지 테마를 설정에서 즉시 전환 가능
- 통화/표기: KRW(₩) 중심, 텍스트는 한국어만 사용

---

## 2) 전역 룰 (필수 준수)

이 프로젝트의 전역 규칙은 `/Users/jm/StudioProjects/WeddingLedger/AGENTS.md`에 한국어로 정리되어 있습니다.

핵심만 다시 적으면:

- 아키텍처: Feature-first + Clean Architecture + MVVM
- 화면(Composable Screen)은 가능하면 Stateless 유지
  - 상태는 `*Route.kt`에서 관리(임시로 `remember` 기반), `*Screen.kt`는 `state + onAction`만 받게 유지
- UI 텍스트: 한국어만
- 글꼴: Pretendard
- 테마: Minimal / Lovely (설정에서 변경)

---

## 3) 기술 스택 / 모듈 구조

- Kotlin Multiplatform + Compose Multiplatform
- 모듈:
  - `composeApp`: 공용 UI/로직(안드/ iOS 공유)
  - `iosApp`: iOS 호스트 앱
- DI: Koin (Android Application, iOS ComposeUIViewController configure에서 초기화)
- 네트워크:
  - Supabase-kt (Auth/PostgREST/Realtime 설치됨)
  - Ktor Client (core 의존성만 세팅, 화면에서는 아직 미사용)
- 로컬:
  - Room / DataStore 의존성은 이미 추가되어 있으나, 현재 UI 작업은 샘플 상태 기반(저장/동기화는 TODO)
- 날짜:
  - `kotlinx-datetime` 의존성 있음 (캘린더/일정 로직 구현 시 사용 권장)

Gradle 파일:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/build.gradle.kts`

---

## 4) Supabase 설정 방식 (중요)

Supabase URL/Anon Key는 `local.properties`에서 읽습니다.

- `local.properties` 키:
  - `SUPABASE_URL=...`
  - `SUPABASE_ANON_KEY=...`

`composeApp/build.gradle.kts`에서 Gradle task로 `BuildConfig.kt`를 생성합니다:
- 생성 위치(빌드 산출물): `composeApp/build/generated/buildconfig/kr/jm/weddingledger/core/config/BuildConfig.kt`
- 소스셋 추가: `kotlin.sourceSets.commonMain { kotlin.srcDir(layout.buildDirectory.dir("generated/buildconfig")) }`

Supabase Client 생성:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/network/SupabaseClient.kt`

Koin 등록:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/di/CoreModule.kt`
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/di/AppModule.kt`

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
  - primary `#EF3948`
  - background `#F8F6F6`
- `WeddingLogThemeStyle.Lovely`
  - primary `#FF8FA3`
  - background `#FFF0F3`

Pretendard 폰트:
- 리소스: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/composeResources/font/pretendard_*.otf`
- 라이선스: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/composeResources/font/PRETENDARD_LICENSE.txt`
- Compose resources로 로딩해서 Typography에 적용하는 방식(멀티플랫폼 안전)

---

## 7) 네비게이션 구조 (Bottom Tab 5개)

Bottom bar 목적지 enum:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/TopLevelDestination.kt`
  - Home / Money / Schedule / Guests / Tools

Bottom bar:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppBottomBar.kt`
  - Tools 탭은 `settings` 라우트에서도 selected 처리됨

NavHost:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppNavHost.kt`

앱 스캐폴드(App):
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/App.kt`
  - 현재 테마 변경은 in-memory `remember` 상태 (DataStore 저장은 TODO)

---

## 8) 화면 매핑 현황 (Stitch HTML -> CMP)

### Home
- S05 홈(미니멀)
  - Route: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/home/presentation/HomeRoute.kt`
  - Screen: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/home/presentation/HomeScreen.kt`
  - Contract: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/home/presentation/HomeContract.kt`

### Money
- S06 돈-지출, S07 돈-예산
  - Route: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/MoneyRoute.kt`
  - Screen: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/MoneyScreen.kt`
  - Contract: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/MoneyContract.kt`

- M01 지출 추가 바텀시트
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/AddExpenseBottomSheet.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/AddExpenseContract.kt`

- M02 예산 항목 추가 바텀시트
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/AddBudgetItemBottomSheet.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/AddBudgetItemContract.kt`

### Schedule
- S08 일정-달력(미니멀)
- S09 일정-체크리스트(미니멀)
  - Route: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/ScheduleRoute.kt`
  - Screen: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/ScheduleScreen.kt`
  - Contract: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/ScheduleContract.kt`

- M03 체크리스트 추가 바텀시트(HTML에서 "Add Checklist")
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/AddChecklistBottomSheet.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/AddChecklistContract.kt`

현재 Schedule 구현 메모:
- 달력(S08): 달력 그리드는 샘플 셀 리스트 기반(실제 월 계산/다음달 꼬리 날짜 등은 TODO)
- 체크리스트(S09): `stickyHeader`로 D-태그 그룹 헤더 구현
- `OnClickPrevMonth/OnClickNextMonth`, `OnClickUpcomingSchedule` 등은 TODO로 no-op 상태

### Guests
- S10 하객(미니멀)
  - Route: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/presentation/GuestsRoute.kt`
  - Screen: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/presentation/GuestsScreen.kt`
  - Contract: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/presentation/GuestsContract.kt`

- M04 하객 추가 바텀시트
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/presentation/AddGuestBottomSheet.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/presentation/AddGuestContract.kt`

현재 Guests 구현 메모:
- 리스트는 섹션(ㄱ/ㅇ/ㅎ) 샘플 기반
- 검색/필터는 UI 상태만 변경(실제 필터링 로직은 TODO)
- 하객 추가에서 "+ 30,000" 같은 quick add는 숫자 파싱 후 콤마 포맷만 적용(실제 화폐 포맷/검증은 TODO)

### Tools / Settings
- Tools 메뉴:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/tools/presentation/ToolsRoute.kt`
- S15 설정(테마 선택):
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/SettingsRoute.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/SettingsScreen.kt`

---

## 9) 공용 UI 컴포넌트(재사용) 정리

바텀시트 핸들/헤더는 feature 내부에서 공용으로 재사용하려고 `core`로 이동했습니다.

- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/ui/components/WeddingLogBottomSheet.kt`

이전 파일은 제거됨:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/BottomSheetComponents.kt`

---

## 10) 빌드/실행 방법

Android 빌드:
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

## 11) 현재 남아있는 TODO (우선순위 제안)

### (P0) 데이터/동기화: Supabase 연동
- 현재 대부분 화면은 "샘플 상태" 기반(UI만 구현)
- 목표:
  - `wedding_id` 기준 필터링
  - Realtime로 동기화
  - Auth 로그인/세션 유지

권장 작업 순서:
1. Auth 플로우(로그인/회원가입 화면) 결정 및 네비게이션 연결
2. 각 feature별 Domain/Data 계층 추가:
   - Repository 인터페이스(도메인) + 구현(data)
   - Supabase PostgREST CRUD
3. Presentation(Route)에서 ViewModel(또는 state holder)로 교체하고, Flow 기반 상태로 전환

### (P0) 테마 설정 영속화
- 현재: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/App.kt`에서 `remember`로만 관리
- 목표: DataStore로 저장해서 재실행 시 유지

### (P1) Schedule 달력 로직
- 현재: `CalendarUiState.sample()`의 하드코딩된 셀 리스트
- 목표:
  - `kotlinx-datetime`로 월별 그리드 생성(일~토 기준)
  - 이벤트/디데이 표시를 서버 데이터로부터 매핑
  - 날짜 선택 시 해당 날짜의 일정 리스트 필터링

### (P1) CRUD 완성
- 체크리스트 CRUD(추가/수정/삭제/완료 토글)와 하객 CRUD를 실제 DB로 연결

### (P2) UI/코드 정리(경고 제거)
- 현재 컴파일 경고:
  - `TabRow` deprecated -> `PrimaryTabRow/SecondaryTabRow`로 교체
  - `Divider` deprecated -> `HorizontalDivider`로 교체
  - `Icons.Rounded.Help`, `Icons.Rounded.Logout` deprecated -> `Icons.AutoMirrored...`로 교체

---

## 12) DB 스키마(참고)

이 프로젝트의 초기 기획 문서 기준(추후 Supabase SQL 작성/적용 필요):
- profiles(id UUID PK, email, name, wedding_id UUID)
- categories(id, name, is_default)
- budget_items(id, category_id, name, target_amount)
- expenses(id, budget_item_id, amount, date, payment_method, payer, memo, image_url)
- guests(id, name, group, relationship_score, expected_gift_money, is_attended)
- checklists(id, title, d_day_tag, is_checked, target_date)
- venues(id, name, rental_fee, meal_cost, parking_desc, rating_total, rating_details JSON)
- values_survey(id, question_text, groom_answer, bride_answer, is_completed)

모든 주요 테이블은 `wedding_id`로 커플 데이터 공유(필터링/권한 설계 필요).

---

## 13) 작업 패턴(권장)

- `*Contract.kt`: UI state + action 정의(샘플 제공)
- `*Route.kt`: 상태 보관/액션 처리/바텀시트 오픈 등 "상태ful" 로직
- `*Screen.kt`: 완전 Stateless(가능한 한), `state + onAction`만으로 UI 구성
- 바텀시트:
  - `ModalBottomSheet` + `WeddingLogSheetHandle/Header` 재사용
  - 스크롤 영역과 하단 CTA 분리(현재 Money/Guests/Schedule 모두 같은 패턴)

---

## 14) 다음 세션이 바로 시작할 때 체크리스트

1. `/Users/jm/StudioProjects/WeddingLedger/AGENTS.md` 확인 (전역 룰)
2. `local.properties`에 Supabase 키가 설정되어 있는지 확인
3. `./gradlew :composeApp:assembleDebug` / `./gradlew :composeApp:compileKotlinIosSimulatorArm64`로 빌드 확인
4. 다음 작업을 무엇으로 잡을지 결정:
   - Supabase CRUD 연결(P0)
   - 테마 영속화(P0)
   - 달력 로직/일정 추가 UI(P1)

