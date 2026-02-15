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

- Supabase 프로젝트:
  - Project Ref: `wmfzhnoxdmfniewgcmzu`
  - URL: `https://wmfzhnoxdmfniewgcmzu.supabase.co`

- `local.properties` 키:
  - `SUPABASE_URL=...`
  - `SUPABASE_ANON_KEY=...`

주의:
- `local.properties`는 gitignore 대상입니다(키가 커밋/푸시되지 않게 하기 위함).
- 키를 숨기는 것만으로는 보안이 해결되지 않습니다. 클라이언트 앱에는 결국 키가 포함될 수 있으므로, **RLS(행 수준 보안)**으로 서버에서 접근을 차단해야 합니다.

`composeApp/build.gradle.kts`에서 Gradle task로 `BuildConfig.kt`를 생성합니다:
- 생성 위치(빌드 산출물): `composeApp/build/generated/buildconfig/kr/jm/weddingledger/core/config/BuildConfig.kt`
- 소스셋 추가: `kotlin.sourceSets.commonMain { kotlin.srcDir(layout.buildDirectory.dir("generated/buildconfig")) }`

Supabase Client 생성:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/network/SupabaseClient.kt`

Koin 등록:
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/di/CoreModule.kt`
- `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/di/AppModule.kt`

---

## 4-1) Supabase 보안(RLS) 권장 SQL (필수)

아래 SQL은 개발/초기 단계에서 최소한의 보안(Authenticated 유저만 접근)을 잡기 위한 권장 설정입니다.

핵심:
- `auth.users`에 새 유저가 생성되면 `public.profiles` row가 자동 생성되도록 `handle_new_user` 트리거를 둡니다.
- `profiles`는 본인 row만 접근 가능
- `checklists`는 `profiles.wedding_id`가 같은 커플 데이터만 접근 가능

```sql
-- updated_at 트리거용 함수
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- 회원가입 시 profiles 자동 생성 + wedding_id 기본값(임시) = 본인 id
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, wedding_id)
  values (new.id, new.email, new.id)
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- RLS on
alter table public.profiles enable row level security;
alter table public.checklists enable row level security;

-- profiles: 본인 row만
drop policy if exists profiles_select_own on public.profiles;
create policy profiles_select_own on public.profiles
for select to authenticated
using (id = auth.uid());

drop policy if exists profiles_insert_own on public.profiles;
create policy profiles_insert_own on public.profiles
for insert to authenticated
with check (id = auth.uid());

drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own on public.profiles
for update to authenticated
using (id = auth.uid())
with check (id = auth.uid());

-- checklists: 내 wedding_id(= profiles.wedding_id) 범위만
drop policy if exists checklists_select_wedding on public.checklists;
create policy checklists_select_wedding on public.checklists
for select to authenticated
using (wedding_id = (select wedding_id from public.profiles where id = auth.uid()));

drop policy if exists checklists_insert_wedding on public.checklists;
create policy checklists_insert_wedding on public.checklists
for insert to authenticated
with check (wedding_id = (select wedding_id from public.profiles where id = auth.uid()));

drop policy if exists checklists_update_wedding on public.checklists;
create policy checklists_update_wedding on public.checklists
for update to authenticated
using (wedding_id = (select wedding_id from public.profiles where id = auth.uid()))
with check (wedding_id = (select wedding_id from public.profiles where id = auth.uid()));

drop policy if exists checklists_delete_wedding on public.checklists;
create policy checklists_delete_wedding on public.checklists
for delete to authenticated
using (wedding_id = (select wedding_id from public.profiles where id = auth.uid()));
```

주의:
- 위는 "커플 연결(초대코드)" 기능이 완성되기 전까지의 임시 기본값(`wedding_id = user.id`) 가정이 포함됩니다.
- 실제 제품 단계에서는 wedding 생성/초대 흐름에 맞춰 `wedding_id`를 따로 발급/연결하도록 바꾸는 게 안전합니다.

---

## 4-2) Supabase MCP 연결 (Codex, 개발 편의)

이 프로젝트는 Supabase MCP를 Codex에 연결해두었습니다. (앱 런타임과는 무관하며, 개발 편의용입니다.)

- MCP 설정 저장 위치: `~/.codex/config.toml` (레포에 커밋되지 않음)
- 프로젝트 Ref: `wmfzhnoxdmfniewgcmzu`
- MCP URL:
  - `https://mcp.supabase.com/mcp?project_ref=wmfzhnoxdmfniewgcmzu&read_only=false`

연결/로그인:
```bash
codex mcp list
codex mcp add supabase --url "https://mcp.supabase.com/mcp?project_ref=wmfzhnoxdmfniewgcmzu&read_only=false"
codex mcp login supabase
```

확인/제거:
```bash
codex mcp get supabase
codex mcp remove supabase
```

정상 상태 예시:
- `codex mcp list` 출력에서 `supabase ... Auth: OAuth`로 표시됨

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
  - Supabase Auth 상태에 따라 `AuthRoute` 또는 메인 UI로 분기(아래 "Auth 흐름" 참고)

---

## 7-1) Auth 흐름 (Supabase)

현재 앱은 "로그인 전/후" 화면을 분리해두었습니다.

- Auth 상태 관찰:
  - UseCase: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/domain/usecase/ObserveAuthStateUseCase.kt`
  - Model: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/domain/model/AuthState.kt`
- App 분기:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/App.kt`
  - `AuthState.Loading`이면 `SplashScreen`
  - `AuthState.Unauthenticated`이면 `AuthRoute`
  - `AuthState.Authenticated`이면 BottomBar 포함 메인 UI
- Auth UI(MVVM):
  - ViewModel: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthViewModel.kt`
  - Screen: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthScreen.kt`
  - Route: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthRoute.kt`
- 로그아웃:
  - Settings 화면에서 `SettingsAction.OnClickLogout` -> `AppNavHost(onLogout)` -> `SignOutUseCase` 호출
  - 파일: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/SettingsRoute.kt`

---

## 8) 화면 매핑 현황 (Stitch HTML -> CMP)

### Auth (로그인/회원가입)
- S01 로그인 (미니멀 수정)
- S02 회원가입 (미니멀)
  - Route: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthRoute.kt`
  - Screen: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthScreen.kt`
  - ViewModel: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthViewModel.kt`
  - Contract: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthContract.kt`

Auth 화면 구현 메모:
- `AuthMode.SignIn`(S01) / `AuthMode.SignUp`(S02) 모드 전환으로 1개의 Screen에서 분기
- SignUp은 디자인 요구사항에 맞춰 다음을 추가로 받음:
  - 비밀번호 확인(`passwordConfirm`)
  - 이용약관 동의(`isTermsAccepted`)
  - 비밀번호 표시 토글(`isPasswordVisible`)

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

iOS(Framework 링크 확인, Xcode 빌드가 여기서 터지면 실패):
```bash
cd /Users/jm/StudioProjects/WeddingLedger
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew :composeApp:linkDebugFrameworkIosArm64
```

iOS 빌드 주의:
- Supabase `realtime-kt` 포함 상태에서 Kotlin/Native compiler cache 빌드가 크래시 나는 케이스가 있어, 현재는 캐시를 비활성화했습니다.
  - 설정 파일: `/Users/jm/StudioProjects/WeddingLedger/gradle.properties`
  - 설정 값: `kotlin.native.cacheKind=none`

---

## 11) 현재 남아있는 TODO (우선순위 제안)

### (P0) 데이터/동기화: Supabase 연동
- 현재 대부분 화면은 "샘플 상태" 기반(UI만 구현)
- 목표:
  - `wedding_id` 기준 필터링
  - RLS/정책/트리거 정리(중요): `profiles`, `checklists`는 Auth 기반으로 제한해야 함
    - RLS off 상태면 anon 키만으로도 데이터 접근 가능(클라이언트 앱은 anon 키가 공개되므로 반드시 RLS 필요)
    - 추천: `handle_new_user` 트리거로 `profiles` 자동 생성 + `wedding_id` 기본값 설정
  - Realtime로 동기화
  - 첫 번째 실데이터 연동(추천): 체크리스트(S09 + M03) CRUD

권장 작업 순서:
1. Supabase RLS/정책/트리거 적용(위 4-1 참고)
2. 각 feature별 Domain/Data 계층 추가:
   - Repository 인터페이스(도메인) + 구현(data)
   - Supabase PostgREST CRUD
3. Presentation(Route)에서 ViewModel(또는 state holder)로 교체하고, Flow 기반 상태로 전환

현재 Auth 상태:
- 로그인/회원가입 UI + 앱 진입 분기(로그인 전/후)까지 구현되어 있음(7-1 참고)

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
2. Git 브랜치 확인(프로젝트 방침: `main`에서만 작업)
   - `git checkout main && git pull`
3. `local.properties`에 Supabase 키가 설정되어 있는지 확인
4. `./gradlew :composeApp:assembleDebug` / `./gradlew :composeApp:compileKotlinIosSimulatorArm64`로 빌드 확인
5. 다음 작업을 무엇으로 잡을지 결정:
   - Supabase CRUD 연결(P0)
   - 테마 영속화(P0)
   - 달력 로직/일정 추가 UI(P1)
