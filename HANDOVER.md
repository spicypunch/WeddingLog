# WeddingLedger 인수인계서 (KMP + Compose Multiplatform)

작성일: 2026-02-24  
마지막 갱신: 2026-05-01
워크스페이스: `/Users/jm/StudioProjects/WeddingLedger`

---

## 0) 퀵스타트

1) 전역 규칙 확인: `/Users/jm/StudioProjects/WeddingLedger/AGENTS.md`  
2) Supabase 키 확인: `/Users/jm/StudioProjects/WeddingLedger/local.properties`  
3) 빌드/테스트:

```bash
cd /Users/jm/StudioProjects/WeddingLedger
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:testDebugUnitTest
./gradlew :composeApp:allTests
./gradlew :composeApp:assembleAndroidTest
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator build -quiet
# UI(instrumented) 테스트는 에뮬레이터/디바이스 연결 후 실행
./gradlew :androidApp:connectedDebugAndroidTest
```

4) Android Studio 업데이트 이후 `convertPbxprojToJson` / `plutil` 오류가 나면 Finder가 아니라 터미널에서 IDE 실행:

```bash
"/Applications/Android Studio.app/Contents/MacOS/studio" /Users/jm/StudioProjects/WeddingLedger
```

5) Supabase QA 데이터 상태는 작업 전 Dashboard/MCP에서 먼저 확인한다.
   - 최근에는 심사용 이메일/비밀번호 계정과 샘플 데이터가 수동으로 준비된 적이 있다.
   - 비밀번호는 Supabase에서 평문 조회가 불가능하므로, 잊어버리면 Dashboard에서 재설정하거나 새 리뷰 계정을 만드는 편이 빠르다.
6) 파트너 활동 푸시는 코드/DB/Edge Function까지 연결되어 있다.
   - Android는 `local.properties`의 Firebase 값이 필요하다.
   - iOS는 `iosApp/Configuration/Config.local.xcconfig`에 Firebase 값을 두고, `Config.xcconfig`에서 로컬 include 한다.
   - 실제 OS push까지 보내려면 Supabase secret `FCM_SERVICE_ACCOUNT_JSON`도 설정해야 한다.
   - Android는 실기기 foreground 수신 + 시스템 알림 표시까지 확인했다(2026-03-29).
   - iOS도 Apple Developer/APNs/Firebase 연동 후 실기기 push 수신까지 확인했다(2026-03-31).
7) Apple 로그인은 iOS 네이티브 `AuthenticationServices` + Supabase `signInWith(IDToken)` 경로로 전환해 실기기 성공까지 확인했다(2026-03-31).
   - Supabase Apple Provider `Client IDs`에는 아래 두 값을 함께 넣어야 한다.
   - `kr.jm.weddingledger.service`
   - `kr.jm.weddingledger.WeddingLedger`
8) 첫 출시 인증 정책은 `Kakao/Apple + Email/Password` 중심으로 정리했다(2026-04-21).
   - Google 로그인 구현은 유지하지만 Android에서 Gmail 작성 화면으로 빠지는 케이스가 있어 화면에서는 일시 미노출
   - Email OTP는 다시 숨기고, 심사용 임시 이메일/비밀번호 로그인을 노출
   - 회원가입은 열지 않고 Supabase Auth에 미리 만든 리뷰 계정으로만 로그인
9) 현재 배포 식별자/버전(2026-05-01)
   - Android `applicationId`: `kr.jm.weddinglog`
   - Android `namespace`: `kr.jm.weddingledger`
   - Android `versionName/versionCode`: `1.2.0 / 3`
   - iOS Bundle ID: `kr.jm.weddingledger.WeddingLedger`
   - iOS `MARKETING_VERSION/CURRENT_PROJECT_VERSION`: `1.2 / 4`
   - iOS는 `TARGETED_DEVICE_FAMILY = 1`로 iPhone only

---

## 0-1) 내일 바로 할 일

1. `main` 최신 pull
2. Android Studio는 필요 시 터미널 실행 방식으로 열기
3. `local.properties` 값 확인
4. 심사용 로그인 계정 준비
   - Kakao/Apple 계정만 믿지 말고 이메일/비밀번호용 기존 Supabase Auth 계정을 준비
   - App Store Connect / Play Console 리뷰 메모에 이메일과 비밀번호를 제공
   - 필요하면 해당 계정으로 온보딩/샘플 데이터까지 먼저 구성
5. **계정 전환 온보딩 QA를 먼저 확인**
   - 기존에 온보딩 완료된 계정으로 로그인
   - 로그아웃
   - 새로 만든 계정으로 로그인
   - 홈으로 바로 가지 않고 `혼자 시작하기 / 공유 코드로 시작하기` 화면이 나오는지 확인
   - 앱을 완전히 종료 후 재실행해도 같은 계정 기준 분기가 유지되는지 확인
6. **계정 전환 push QA를 확인**
   - 같은 기기에서 계정 A 로그인
   - 로그아웃
   - 계정 B 로그인
   - A 활동 push가 더 이상 새지 않는지 확인
   - `push_token_deactivated`, `push_token_synced` 로그 확인
7. **홈 + 가계부 탭을 제일 먼저 QA**
   - 홈에서 큰 금액이 들어가도 결산 카드 레이아웃이 안 깨지는지
   - 홈에서 중간 CTA 카드와 FAB가 제거되었는지
   - 첫 진입 시 `예산` 탭이 기본 선택되는지
   - 지출 필터 기본 기간이 `전체 기간`인지
   - `예산 / 지출` 분리가 직관적인지
   - `예산 항목 추가` / `지출 추가` 흐름이 쉬운지
   - 저장 실패 시 바텀시트가 닫히지 않고 입력값이 유지되는지
   - 아직도 복잡하면 어느 구간에서 막히는지 메모
8. 그 다음 일정/하객/알림센터 순서로 통합 QA 진행
9. 파트너 계정 2개로 지출/일정 추가 후
   - 알림센터 `새 활동`에 상대방 활동이 뜨는지
   - Android는 실제 OS push까지 오는지 확인
   - iOS도 실제 OS push까지 오는지 교차 확인
10. **회원탈퇴 QA**
   - Android에서 Kakao 계정 회원탈퇴
   - Android 또는 iOS에서 이메일/비밀번호 계정 회원탈퇴
   - iOS에서 Apple 계정 회원탈퇴
   - 탈퇴 후 로그인 화면으로 복귀하는지
   - 탈퇴한 계정 세션으로 재진입되지 않는지
   - `profiles`, `device_push_tokens`, `partner_activity_notifications`, `auth.users` 정리 확인

### Windows 컴퓨터에서 우선 진행할 작업

1. 로그아웃/계정 전환 시 푸시 토큰 누수 QA
2. 3초 강제 스플래시 완화
3. iOS Firebase 초기화/`GoogleService-Info.plist` 경고 정리
4. iOS foreground push UX polishing
5. 약관 동의 UX 미세조정

주의:
- Windows에서는 Android/shared 작업 위주로 진행
- iOS APNs/Apple 로그인/Xcode capability 검증은 Mac에서만 이어서 진행

---

## 1) 현재 상태 요약

- 아키텍처: Feature-first + Clean Architecture + MVVM 유지
- Android 구조:
  - `composeApp`는 Android-KMP shared library
  - `androidApp`는 Android 앱 셸(`MainActivity`, `Application`, `AndroidManifest`, launcher 리소스`) 담당
- Auth/Onboarding: `join_wedding` RPC 포함 실데이터 동작
  - 이메일 인증 딥링크 E2E 연결 완료(Android `intent-filter` + iOS `onOpenURL` + KMP bridge)
  - 딥링크 처리 성공/실패 시 로그인 화면 안내 배너 표시(`AuthMessage.Info/Error`)
  - 로그인 미인증 전용 안내 카드 + 재전송 60초 쿨다운 + 최대 5회 제한 추가
  - 첫 출시 로그인 정책: Android는 `Kakao + Email/Password`, iOS는 `Kakao + Apple + Email/Password`
  - Google 로그인은 Android에서 Gmail 작성 화면으로 잘못 라우팅되는 케이스가 있어 첫 출시 안정화를 위해 화면에서 일시 미노출
  - 이메일/비밀번호 로그인 UI는 심사용 공통 로그인 수단으로 임시 노출
  - Email OTP UI는 다시 숨김
  - 이메일/비밀번호 로그인도 약관 및 개인정보 처리방침 동의 후에만 진행
  - 이메일 회원가입은 비활성
  - Kakao 로그인 네이티브 SDK 경로(iOS/Android) 적용 및 양 플랫폼 성공 검증 완료(2026-03-04)
  - 온보딩 엔트리 분기 도입(`혼자 시작하기` / `공유 코드로 시작하기`)
  - `혼자 시작하기` 경로: 프로필 입력(닉네임/역할/예식일) 후 즉시 온보딩 완료
  - `공유 코드로 시작하기` 경로: 닉네임 + 공유 코드 입력으로 파트너 연결
  - 초대 코드는 `6자리 숫자` 형식으로 통일(`W-` 접두어 제거)
  - 초대 코드 공유는 온보딩 필수 단계에서 제거하고 설정 화면에서 선택적으로 제공
  - 계정 전환 시 이전 계정의 온보딩 완료 상태가 새 계정에 재사용되지 않도록 사용자 ID 기준으로 `OnboardingGateRoute`/`OnboardingViewModel` key 분리 완료
  - 회원가입/온보딩 프로필 화면에서 키보드 열림 시 고정 하단 CTA를 스크롤 영역 CTA로 전환(가림/과상승 완화)
  - iOS 키보드 대응 통합:
    - SwiftUI 키보드 safe-area 자동 회피 비활성화
    - 전역 탭 dismiss는 키보드가 떠 있을 때만 동작
- Home:
  - 결산 대시보드 구조로 재편
  - 총 예산/총 지출/남은 금액/사용률
  - 신랑/신부/공동 분담
  - 카테고리별 결산, 다가오는 일정, 최근 지출 요약
- 가계부:
  - 하단 탭명 `가계부`
  - `예산 / 지출` 분리 탭 레이아웃으로 재정리
  - 기본 진입 탭은 `예산`
  - 상단 총 예산/총 지출/남은 금액 요약은 유지
  - `예산` 탭:
    - `남은 결제 확인`
    - 카테고리별 예산 항목 관리
    - `상세 결산 보기`를 눌렀을 때만 인사이트/분담/단계별 결산 노출
  - `지출` 탭:
    - 지출 요약 + 지출 기록 중심
    - 필터는 기본 숨김
  - FAB는 탭에 따라 문맥적으로 변경
    - 예산: `예산 항목 추가` 또는 `카테고리 만들기`
    - 지출: `지출 추가`
  - `계약금/중도금/잔금/일반` 단계 지원
  - 단계별 요약, 남은 결제, 결제 분담, 예정일 지원
  - `public.budget_items.stage`, `public.budget_items.due_date` 실DB 반영 완료
- Schedule:
  - 체크리스트 생성/수정/삭제/체크
  - 일정 CRUD + 상세 + 수정/삭제
  - 일정 시간 입력 UX(24h 마스킹 + 유효성)
  - 일정 반복(반복 없음/매주/매월) 지원
  - `repeat_rule` 컬럼 우선 + 레거시(`memo` 메타) 자동 폴백으로 반복 규칙 저장 호환성 유지
  - 선택 날짜 일정 + 다가오는 일정 + 마감 임박 체크리스트를 한 화면에서 상시 노출
  - 캘린더 밀집일 마커 강화(다건 일정 수량 표시)
- Guests:
  - `예상 하객 | 축의대` 2개 화면으로 재설계
  - 예상 하객 수 계산:
    - `꼭 옴 = 1 x (본인 + 동반 인원)`
    - `애매 = 0.5 x (본인 + 동반 인원)`
    - `안 옴 = 0`
  - 보증인원 설정 및 보증 대비 초과/부족 표시
  - 실제 참석 여부/실제 축의금 기록 지원
  - `public.guests.attendance_likelihood`, `companion_count`, `actual_attended`, `actual_gift_money` 반영 완료
  - `public.guest_settings.guaranteed_guest_count` 테이블/정책 반영 완료
- Settings:
  - 프로필/초대코드 실데이터 로드
  - 초대코드 복사 동작
  - 프로필 수정/계정 관리/앱 정보 화면 연결
  - 계정 관리 화면에서 회원탈퇴 v1 지원
    - 일반 계정은 바로 삭제 요청
    - Apple 계정은 탈퇴 직전에 네이티브 재인증 후 `authorizationCode`를 서버에 전달
  - 알림 설정 항목에서 OS 앱 알림 설정 화면 바로 열기(실패 시 알림센터 폴백)
  - 앱 버전 표기 BuildConfig 동적화(`APP_VERSION_NAME`, `APP_VERSION_CODE`)
  - 테마 영속화(DataStore) 유지
- Notifications:
  - 알림센터 화면 신설 (`feature/notifications`)
  - Home/Schedule/Money/Settings에서 알림센터 진입 연결
  - 일정/체크리스트/예산 초과 기반 알림 피드 구성
  - 로컬 리마인더 예약(Android/iOS `expect/actual`) 구현
  - 일정/체크리스트 수정/삭제 및 체크 완료 시 기존 예약 리마인더 자동 해제
  - `새 활동`은 fetch 시 자동 읽음 처리하지 않고, 사용자가 직접 눌렀을 때만 읽음 처리
- Tools:
  - 웨딩홀 비교 결과 공유(Top 결과 텍스트 공유) 지원
  - 즐겨찾기 우선 정렬 + 추천 점수 규칙 강화
  - `웨딩홀 비교`, `가치관 설문`은 현재 진입 시 `준비 중인 기능이에요.` 안내 후 대기
- Ads:
  - Android/iOS 메인 탭 하단에 공통 AdMob 배너 슬롯 연결
  - debug 빌드는 테스트 광고 ID, release 빌드는 실제 광고 ID 사용
  - iOS 호스트 앱에도 Google Mobile Ads + User Messaging Platform SPM 연결 완료
- 테스트:
  - `commonTest` 대폭 확장 (도메인/usecase/flow + ViewModel 레이어)
  - `androidInstrumentedTest` UI smoke 테스트 유지(Auth/Home/Guests/Money/Schedule/Settings/Notifications/Onboarding/Tools)
  - 공용 화면 상태의 `sample()` 더미 데이터 제거, smoke test는 실제 최소 상태 생성 방식으로 정리

---

## 1-1) 최근 추가 변경 (2026-03-08)

- Android AGP 9 대응 구조 선반영
  - `:androidApp` 모듈 신설
  - `composeApp`를 `com.android.kotlin.multiplatform.library` 기반 shared module로 전환
  - Android launcher/icon/string 리소스를 `androidApp/src/main/res`로 이동
- Auth/Onboarding 안정성
  - Auth 에러 분기를 문자열 매칭에서 `AuthErrorType` 기반으로 전환
  - Onboarding 에러 분기를 `OnboardingErrorType` 기반으로 전환
- 공통 Supabase 지원 로직 추출
  - `WeddingIdResolver`, repository 공통 Supabase 오류 로그 함수 추가
  - `money / guests / schedule / checklist / tools / onboarding` repository에 적용
- 테스트 보강
  - `SupabaseRepositorySupportTest`, `AuthResultTest` 추가
  - `AuthViewModelTest`, `OnboardingViewModelTest`, `MoneyViewModelTest`, `MoneyUseCaseAndFlowTest`, `HomeViewModelTest` 갱신
- Android Kakao 설정 주의
  - Android/shared 쪽 Kakao native key는 루트 `local.properties`의 `KAKAO_NATIVE_APP_KEY`를 읽음
  - iOS `Config.xcconfig`만 채워져 있으면 Android Kakao SDK는 초기화되지 않음
  - 다른 PC에서 작업 시 `local.properties` 누락 여부를 먼저 확인할 것

---

## 1-2) 오늘 추가 변경 (2026-03-16)

- 가계부 탭 UX 단순화
  - 기존 통합형 화면이 처음 쓰는 사용자에게 복잡하게 느껴져 `예산 / 지출` 탭 구조로 되돌림
  - `예산` 탭을 계획 중심 화면으로, `지출` 탭을 기록 중심 화면으로 역할 분리
  - 상세 결산 정보는 기본 노출하지 않고 접힘 상태로 변경
- Android Studio / Gradle 실행 이슈 정리
  - `composeApp`는 실행용 앱 모듈이 아니라 shared Android-KMP library
  - 실제 Android 앱 빌드는 `:androidApp:assembleDebug`
  - `:composeApp:assembleDebug`, `:composeApp:testDebugUnitTest`는 현재 구조 기준 잘못된 옛 명령
  - 문서/인수인계/README의 빌드 명령도 현재 구조에 맞게 수정함
  - IDE 업데이트 후 `convertPbxprojToJson` / `plutil` 오류가 날 경우 터미널 실행으로 우회 가능함
- QA 초기화
  - Supabase에서 `public` 앱 데이터 + `auth.users` 포함 인증 데이터까지 삭제 완료
  - 완전 초기 상태 QA 가능
  - 테스트용 계정도 새로 만들어야 함

---

## 1-3) 오늘 추가 변경 (2026-03-22)

- Home UI 정리
  - 결산 카드의 사용률 칩을 금액 아래 행으로 이동
  - 큰 금액일 때 텍스트 크기를 한 단계 줄여 레이아웃 안정화
  - 중간 `지출 추가하기 / 일정 확인하기` 카드 제거
  - 홈 FAB 제거
- 파트너 활동 알림 추가
  - 지출 추가 시 같은 `wedding_id`의 상대 사용자에게 활동 기록 생성
  - 일정 추가 시 같은 방식으로 활동 기록 생성
  - 알림센터를 `새 활동 / 리마인더` 2섹션으로 확장
  - unread 표시 및 알림센터 진입 시 읽음 처리 반영
- Push 인프라 추가
  - `device_push_tokens`, `partner_activity_notifications` 테이블/RLS/인덱스 직접 반영

---

## 1-4) 오늘 추가 변경 (2026-04-01)

- Push 토큰 계정 전환 보강
  - `manage-device-push-token` Edge Function 추가/배포
  - 클라이언트가 `device_push_tokens`를 직접 만지지 않고 서버에서 `installation_id` 기준 stale row 정리 후 `sync/deactivate`
  - 남은 일은 실제 한 기기 계정 전환 QA 확인
- 저장 바텀시트 UX 개선
  - 가계부/일정/하객 저장 시트는 저장 성공 시에만 닫힘
  - 실패하면 시트를 유지하고 입력값을 그대로 보존
  - 저장 중에는 닫기/중복 저장을 막음
- 알림센터 unread 정책 변경
  - fetch 시 unread -> read 자동 업데이트 제거
  - `새 활동` 카드를 직접 눌렀을 때만 읽음 처리
  - 실패 시 optimistic update를 복구하도록 보강
- 가계부 안정화
  - `expenses` RLS를 `budget_item_id = null`인 지출도 허용하도록 조정
  - 지출 저장은 client-generated UUID + plain insert 방식으로 바꿔 `JsonDecodingException` 재발을 줄임
  - 새 지출은 `created_by`를 현재 로그인 사용자 ID로 채움
  - 지출 필터 기본 기간을 `전체 기간`으로 변경
  - 예산 화면의 `목표` 문구를 `예산`으로 통일
- Tools/샘플 데이터 정리
  - `웨딩홀 비교`, `가치관 설문`은 현재 준비 중 토스트만 노출
  - shared 코드에 남아 있던 `sample()` 기반 더미 상태/가짜 입력값 제거
  - Android smoke test도 실제 최소 상태 생성 방식으로 정리
- Build/Gradle
  - clean 후 `BuildConfig.kt`가 사라져 깨지던 문제를 `composeApp/build.gradle.kts`의 생성 태스크 수정으로 보강
  - 현재 `generateBuildConfig`는 configuration cache 경고가 남지만, Android debug / Android test assemble / iOS simulator compile은 모두 통과

## 1-5) 오늘 추가 변경 (2026-04-08)

- 회원탈퇴 v1 구현/배포
  - `설정 > 계정 관리`에 파괴적 `회원탈퇴` 섹션 추가
  - 1차 경고 다이얼로그 후 최종 탈퇴 요청 구조 적용
  - Apple 계정은 탈퇴 직전에 네이티브 `AuthenticationServices` 재인증으로 `authorizationCode`를 다시 받아 서버에 전달
  - 새 Supabase Edge Function `delete-account` 추가/배포 완료
  - 함수는 `device_push_tokens`, `partner_activity_notifications`, `profiles`, `auth.users`를 순차 삭제
  - Apple 계정 여부는 request body가 아니라 서버의 사용자 정보에서 직접 판별하도록 보강
  - `auth.admin.deleteUser()` 실패 시 먼저 지운 public row를 복구하도록 보강
  - 공용 데이터는 유지하되 탈퇴 사용자가 남긴 `expenses.created_by`, `schedule_events.created_by` 참조는 먼저 `null`로 끊도록 보강
  - Android에서 Edge Function 게이트웨이의 `Invalid JWT`가 재현돼 `delete-account`는 현재 `--no-verify-jwt` 배포 상태이며, 함수 내부 `auth.getUser(jwt)`로 인증을 검증
  - 클라이언트는 탈퇴 요청 시 `401 Invalid JWT`를 받으면 `refreshCurrentSession()` 후 한 번 자동 재시도
- AdMob 배너 iOS 적용
  - iOS 호스트 앱에 Google Mobile Ads SDK를 연결해 Android와 같은 공통 배너 슬롯이 실제로 표시되도록 구현
  - debug 빌드는 테스트 광고, release 빌드는 실제 광고 ID 사용
  - 현재 메인 탭에서만 배너가 보이고 입력/설정/온보딩 화면은 제외
- 검증
  - `./gradlew :composeApp:allTests` 성공
  - `./gradlew :androidApp:assembleDebug` 성공
  - `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 16' CODE_SIGNING_ALLOWED=NO build` 성공
  - 주의: `embedAndSignAppleFrameworkForXcode`는 Xcode 환경 밖에서 단독 실행하면 `Could not infer iOS target architectures`로 실패할 수 있음
- 빌드/테스트
  - `./gradlew :androidApp:assembleDebug` 성공
  - `./gradlew :composeApp:assembleAndroidTest` 성공
  - `./gradlew :composeApp:compileKotlinIosSimulatorArm64` 성공

---

## 1-6) 오늘 추가 변경 (2026-04-21)

- 첫 출시 인증 정책 정리
  - Android 로그인 화면: `Kakao + Email/Password`
  - iOS 로그인 화면: `Kakao + Apple + Email/Password`
  - Google 로그인 코드는 유지하지만 버튼은 주석 처리로 미노출
- Google 미노출 사유
  - Android에서 Google OAuth 진입 시 Gmail 앱 작성 화면으로 빠지는 케이스가 재현됨
  - 첫 출시 심사/스토어 스크린샷/지인 테스트에서 잘못된 외부 앱 라우팅이 보이면 사용자 신뢰도가 크게 떨어짐
  - OAuth 처리 방식(Custom Tabs 또는 네이티브 Google Sign-In/Credential Manager) 정리 전까지 화면에서 숨기는 쪽이 안전함
- 이메일/비밀번호 임시 로그인 노출 사유
  - Kakao 계정만 제공하면 리뷰어가 2FA, 전화번호, 지역, 보안 정책에 막힐 수 있음
  - Apple 로그인은 iOS 전용이라 Android 심사용 공통 계정 수단이 되지 않음
  - 이메일/비밀번호는 Android/iOS 양쪽에서 같은 리뷰 계정을 제공할 수 있고, 리뷰어가 외부 메일함에 접근할 필요가 없음
- 구현 메모
  - `AuthScreen`의 이메일 접힘/펼침 UI를 이메일/비밀번호 로그인으로 전환
  - `AuthRepositoryImpl.signIn()`을 Supabase `Email` provider 로그인으로 복구
  - 이메일/비밀번호 로그인도 약관 및 개인정보 처리방침 동의 후에만 실행
  - 심사용 이메일 계정은 Supabase Auth에 미리 생성하고, 필요하면 온보딩/샘플 데이터를 먼저 구성해야 함
- 검증
  - `./gradlew :androidApp:assembleDebug` 성공
  - 기존 deprecation/configuration cache 경고는 남지만 이번 변경으로 인한 빌드 실패는 없음

## 1-7) 오늘 추가 변경 (2026-05-01)

- 출시/스토어 설정 최신화
  - Android 실제 배포 패키지명을 `kr.jm.weddinglog`로 정리
  - Kotlin namespace와 기존 코드 패키지는 `kr.jm.weddingledger` 유지
  - Android 현재 버전은 `1.2.0 (3)`
  - iOS 현재 버전은 `1.2 (4)`
  - iOS는 App Store Connect 업로드 시 iPad까지 타겟되지 않도록 `TARGETED_DEVICE_FAMILY = 1` 확인
- 첫 출시 노출 정책 유지
  - Google 로그인은 구현을 유지하되 화면에서 미노출
  - AdMob 배너는 첫 출시 빌드에서 숨김
  - 광고를 다시 켤 때는 Compose 내부 배너 배치와 하단 내비게이션 여백을 함께 재설계 필요
- 계정 전환 온보딩 버그 수정
  - 증상: 기존 계정에서 온보딩 완료 후 로그아웃하고 새 계정으로 로그인하면 `혼자 시작하기 / 공유 코드로 시작하기` 화면 대신 홈으로 바로 이동
  - 원인: `OnboardingViewModel`이 사용자 ID와 무관하게 재사용되어 이전 계정의 `OnboardingStep.Done` 상태가 남음
  - 조치: `Authenticated.user.id`를 `OnboardingGateRoute`에 전달하고, Compose `key(userId)` 및 `koinViewModel(key = "onboarding:$userId")`를 적용
  - 검증: `./gradlew :androidApp:assembleDebug` 성공

---

## 1-5) 오늘 추가 변경 (2026-03-29)

- Android 파트너 활동 push 실수신 확인
  - FCM 토큰 fetch / Supabase 등록 / Edge Function 발송 / Android foreground 수신까지 확인
  - Android `onMessageReceived()` 로그 추가
  - foreground 상태에서도 시스템 알림이 뜨도록 `WeddingLedgerFirebaseMessagingService`에서 직접 로컬 notification 표시
  - 채널을 `IMPORTANCE_HIGH`로 분리해 heads-up 가능성을 높임
- Push 디버깅 로그 정리
  - Android/공용 푸시 경로를 `WL_SUPABASE` 태그로 추적 가능
  - 주요 확인 로그:
    - `remote_push_token_fetch_success`
    - `push_token_synced`
    - `partner_activity_dispatch_start/success/failed`
    - `onMessageReceived`
    - `notification_displayed`
- Supabase push 발송 경로 보강
  - `device_push_tokens` 중복 토큰 충돌 방지 로직 추가
  - Edge Function `send-partner-activity-push`를 caller JWT 의존에서 분리
  - 함수가 `expenses.created_by` / `schedule_events.created_by`와 `wedding_id`를 기준으로 수신자를 계산하도록 변경
  - `--no-verify-jwt`로 재배포 완료
- 일정 저장 안정성 보강
  - `schedule_events` insert 응답 파싱 의존 제거
  - 앱에서 UUID를 먼저 생성한 뒤 insert하고, 반환 row는 안전 파싱으로 읽도록 변경
  - `JsonDecodingException` 재현 케이스 회귀 테스트 추가
- 일정 추가 바텀시트 문구 정리
  - `일정 제목`, `장소 (선택)` 예시 placeholder 제거
- iOS Firebase 설정 방식 정리
  - `iosApp/Configuration/Config.local.xcconfig`를 로컬 전용 파일로 두고 `.gitignore` 처리
  - `Config.xcconfig`는 로컬 include만 담당

## 1-5) 오늘 추가 변경 (2026-03-31)

- iOS 원격 push 실수신 확인
  - Apple Developer App ID / APNs key / Firebase APNs 업로드 / Xcode capability 설정 후 실기기 push 수신 확인
  - iOS도 `push_token_synced platform=ios`까지 정상 기록
- Apple 로그인 구현 완료
  - iOS는 브라우저 OAuth 대신 네이티브 `AuthenticationServices` 로그인으로 전환
  - Supabase는 `signInWith(IDToken)` 경로를 사용
  - Apple Provider `Client IDs`는 `kr.jm.weddingledger.service, kr.jm.weddingledger.WeddingLedger` 조합으로 설정해야 정상 동작
- iOS 호스트 앱 보강
  - `AppleNativeSignInCoordinator.swift` 추가
  - `iosApp.entitlements`에 `com.apple.developer.applesignin` 추가
  - `KakaoNativeSignInCoordinator`를 앱 시작 시 안전 초기화하고, Apple 콜백에서 Kakao URL 판별이 잘못 끼어들지 않도록 방어
- 알려진 로그 메모
  - iOS에서 Firebase `GoogleService-Info.plist` 관련 경고가 남을 수 있으나, 현재 기준 FCM 토큰 발급 / Supabase 토큰 등록 / 실제 push 수신은 모두 동작 확인
  - 현재 iOS는 Firebase 값 반영까지만 끝났고, APNs entitlement/Apple Developer 계정이 준비돼야 원격 push와 Apple 로그인을 완결할 수 있음
- 약관/개인정보 동의 흐름 정리
  - 로그인 화면에 실제 약관/개인정보 링크 연결
  - 소셜 로그인 전에 동의 체크를 강제
  - 한 번 동의하면 현재 약관 버전 기준으로 로그인 화면에서 다시 보이지 않도록 DataStore에 저장
  - 실제 문서 URL은 `local.properties`의 `TERMS_OF_SERVICE_URL`, `PRIVACY_POLICY_URL` 사용
- `join_wedding` 제3자 연결 방지
  - Supabase 원격 DB 함수 `public.join_wedding`를 직접 수정해, 이미 2명이 연결된 wedding에는 제3자 합류가 불가능하도록 변경
  - 같은 변경을 repo SQL 파일로 남기지 않음
  - 현재 프로젝트에는 SQL migration 파일을 유지하지 않는 정책으로 운영

## 1-5) 현재 남은 리스크 (2026-03-29 확인)

- 로그아웃/계정 전환 시 푸시 토큰 비활성화 실패가 묵살될 수 있음
- 알림센터는 fetch 시 unread를 곧바로 read로 바꿈
- 저장 바텀시트가 결과 전에 닫혀 실패 시 입력값을 잃음
- 앱 시작 시 3초 최소 스플래시가 고정되어 있음
- iOS 원격 push/Apple 로그인은 Apple Developer 계정 준비 전까지 미완료

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
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/androidInstrumentedTest/kotlin/kr/jm/weddingledger/ui/ScreenSmokeUiTest.kt`
  - 주요 화면 액션 디스패치 검증 11건
    - Auth, Home, Guests, Money, Schedule, Settings, NotificationCenter, ProfileSetup, InviteJoin, ValuesSurvey, VenueComparison

### 2-8) 키보드 UX 보강 + 딥링크 안내 문구 (2026-02-24)

- iOS 전역 키보드 dismiss:
  - 빈 영역 탭 시 `endEditing(true)`로 키보드 닫힘
  - 적용 파일: `/Users/jm/StudioProjects/WeddingLedger/iosApp/iosApp/ContentView.swift`
- 키보드 열림 시 CTA 정책 통일(고정 CTA -> 스크롤 CTA):
  - 회원가입: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthScreen.kt`
  - 온보딩 프로필: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/ProfileSetupScreen.kt`
- 딥링크 처리 결과 메시지 개선:
  - 성공 시 안내 문구(`이메일 인증 완료`, `비밀번호 재설정 링크 확인`) 표시
  - 실패 시 오류 배너 유지
  - 적용 파일: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/App.kt`

### 2-9) 온보딩 화면 플로우 단순화 (2026-02-25)

- 신규 온보딩 단계 추가:
  - `OnboardingStep.EntryChoice`
  - `OnboardingStep.InviteJoin`
  - 파일: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/domain/model/OnboardingModels.kt`
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
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/OnboardingContract.kt`
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/OnboardingRoute.kt`
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/OnboardingViewModel.kt`
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/data/repository/OnboardingRepositoryImpl.kt`

### 2-10) 키보드 이슈 원인군 일괄 대응 (2026-02-24)

- 공통 키보드 정책 유틸 추가:
  - `rememberImeVisible()`, `keyboardAwareBottomPadding(...)`
  - 파일: `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/ui/keyboard/KeyboardAware.kt`
- 입력 화면 공통 적용:
  - 로그인/회원가입/온보딩/프로필수정 화면에서 IME 열림 기준 패딩/정렬 정책 통일
  - 적용 파일:
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/auth/presentation/AuthScreen.kt`
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/ProfileSetupScreen.kt`
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/onboarding/presentation/InviteJoinScreen.kt`
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/ProfileEditScreen.kt`
- 입력 바텀시트 공통 적용:
  - 하단 CTA 영역에 `imePadding()` 적용해 키보드 위로 버튼 노출 보장
  - 적용 파일:
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/presentation/AddGuestBottomSheet.kt`
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/AddBudgetItemBottomSheet.kt`
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/AddExpenseBottomSheet.kt`
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/AddChecklistBottomSheet.kt`
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/AddScheduleEventBottomSheet.kt`
  - iOS 호스트 보정:
    - `ComposeView().ignoresSafeArea(.keyboard, edges: .bottom)` 적용
    - 탭 dismiss 제스처를 `UIGestureRecognizerDelegate`로 제어해 첫 포커스 탭 간섭 제거
    - 파일: `/Users/jm/StudioProjects/WeddingLedger/iosApp/iosApp/ContentView.swift`

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
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/settings/data/NotificationPermissionPromptStorage.kt`
  - Android actual:
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/androidMain/kotlin/kr/jm/weddingledger/core/settings/data/NotificationPermissionPromptStorage.android.kt`
  - iOS actual:
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/iosMain/kotlin/kr/jm/weddingledger/core/settings/data/NotificationPermissionPromptStorage.ios.kt`
  - 적용 라우트:
    - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/home/presentation/HomeRoute.kt`
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

- 당시 로그인 정책:
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

### 2-15) 4개 핵심 탭 제품 개편 + 하객 DB 확장 (2026-03-15)

- Home:
  - 홈을 결산 대시보드로 재구성
  - 총 예산/총 지출/남은 금액, 분담 요약, 카테고리 결산, 다가오는 일정, 최근 지출 표시
- 가계부:
  - 탭명 `돈` -> `가계부` 변경
  - 예산/지출 통합형 화면으로 재배치
  - `계약금/중도금/잔금/일반` 단계 및 예정일 지원
  - 남은 결제, 단계별 요약, 분담 요약 추가
  - Supabase 직접 반영:
    - migration: `20260315082723 add_budget_item_stage_and_due_date`
    - `public.budget_items.stage`
    - `public.budget_items.due_date`
- Schedule:
  - 선택 날짜 일정, 다가오는 일정, 마감 임박 체크리스트를 한 화면에서 항상 확인 가능하도록 개편
- Guests:
  - `예상 하객 | 축의대` 2분할 구조로 전면 개편
  - 예상 하객 계산식:
    - `꼭 옴 = 1 x (본인 + 동반 인원)`
    - `애매 = 0.5 x (본인 + 동반 인원)`
    - `안 옴 = 0`
  - 보증인원 설정/비교 기능 추가
  - 축의대에서 실제 참석 여부와 실제 축의금 기록 가능
  - 기존 `GuestDetailBottomSheet` 제거, 편집 흐름을 통합 바텀시트로 정리
  - Supabase 직접 반영:
    - migration: `20260315091142 add_guest_expectation_and_gift_ledger_fields`
    - `public.guests.attendance_likelihood`
    - `public.guests.companion_count`
    - `public.guests.actual_attended`
    - `public.guests.actual_gift_money`
    - `public.guest_settings.guaranteed_guest_count`
- 문서/운영 원칙:
  - SQL 파일을 repo에 생성하지 않고 Supabase MCP로 원격 DB에 직접 적용
  - 통합 QA 체크리스트는 루트 `qa.md`로 별도 관리

---

## 3) Supabase 상태 (MCP 재확인, 2026-03-15)

### 적용 완료

- 이전 적용:
  - 함수 `search_path` 고정 (`set_updated_at`, `my_wedding_id`)
  - FK 인덱스 추가 (`budget_items.category_id`, `expenses.budget_item_id`, `venue_favorites.venue_id`)
- 추가 적용:
  - RLS 정책 내 `auth.uid()`를 `(select auth.uid())` 패턴으로 일괄 변경
  - migration name: `optimize_rls_auth_uid_initplan`
  - 초대 코드 6자리 숫자 통일
    - migration: `20260228021452 invite_code_six_digit_numeric`
  - 가계부 단계/예정일 컬럼 추가
    - migration: `20260315082723 add_budget_item_stage_and_due_date`
    - `public.budget_items.stage`
    - `public.budget_items.due_date`
  - 하객 예상/축의대 컬럼 및 보증인원 테이블 추가
    - migration: `20260315091142 add_guest_expectation_and_gift_ledger_fields`
    - `public.guests.attendance_likelihood`
    - `public.guests.companion_count`
    - `public.guests.actual_attended`
    - `public.guests.actual_gift_money`
    - `public.guest_settings`

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
- `Authentication > Providers > Apple`
  - Apple Developer 계정 준비 후 provider 값 입력/검증 필요

---

## 4) 최근 검증 결과

성공(2026-03-15 기준):

```bash
./gradlew :composeApp:allTests
./gradlew :androidApp:assembleDebug
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build -quiet
```

수동 확인(2026-03-15):
- Home 결산 대시보드/가계부/일정/하객 구조 변경분 로컬 빌드 통과
- 하객 Supabase migration 2종 원격 적용 확인

성공(2026-03-04 기준):

```bash
./gradlew :androidApp:assembleDebug
# iOS host
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

성공(2026-03-05 기준):

```bash
./gradlew :androidApp:assembleDebug
```

수동 QA(2026-03-04):
- iOS Kakao 로그인 성공
- Android Kakao 로그인 성공

성공(2026-02-25 기준):

```bash
./gradlew :composeApp:allTests
./gradlew :androidApp:assembleDebug
```

성공(2026-02-19 기준):

```bash
./gradlew :composeApp:allTests --rerun-tasks

./gradlew :composeApp:compileTestKotlinIosSimulatorArm64 --rerun-tasks

./gradlew :androidApp:compileDebugAndroidTestKotlin

./gradlew :androidApp:assembleDebug
```

실행 제약:

```bash
./gradlew :androidApp:connectedDebugAndroidTest
# 실패 원인: No connected devices
```

---

## 5) 남은 백로그 (우선순위)

### P1

- Apple 로그인:
  - Apple Developer 계정 준비 후 Supabase Apple provider 설정 및 실기기 로그인 검증
- 통합 QA:
  - 홈/가계부/일정/하객 전체 실사용 흐름 QA
  - 루트 `qa.md` 체크리스트 기준으로 검증
- Android UI 테스트 실기기/에뮬레이터 1회 실행 검증
  - `./gradlew :androidApp:connectedDebugAndroidTest`

### P2

- 실기기 QA(Android/iOS 알림 권한/로컬 리마인더 실제 수신 확인)
- iOS host 앱 빌드/실행 재확인(Xcode) 및 알림 권한 동작 점검
- Tools 탭 방향 재설계
  - 웨딩홀 비교 중심에서 후기/기록형 구조로 갈지 판단 필요

---

## 6) 참고 핵심 파일

- 네비게이션:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppNavHost.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/core/navigation/AppRoutes.kt`
- Home:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/home/presentation/HomeScreen.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/home/presentation/HomeViewModel.kt`
- 가계부:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/MoneyScreen.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/presentation/MoneyViewModel.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/money/data/repository/MoneyRepositoryImpl.kt`
- Schedule:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/ScheduleScreen.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/schedule/presentation/ScheduleViewModel.kt`
- Guests:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/presentation/GuestsScreen.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/presentation/GuestsRoute.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/presentation/GuestsViewModel.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/guests/data/repository/GuestRepositoryImpl.kt`
- 알림센터:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/notifications/presentation/NotificationCenterRoute.kt`
- 설정:
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/SettingsRoute.kt`
  - `/Users/jm/StudioProjects/WeddingLedger/composeApp/src/commonMain/kotlin/kr/jm/weddingledger/feature/settings/presentation/ProfileEditViewModel.kt`
- QA:
  - `/Users/jm/StudioProjects/WeddingLedger/qa.md`
