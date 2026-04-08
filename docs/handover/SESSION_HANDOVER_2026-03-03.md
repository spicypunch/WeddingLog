# Session Handover (2026-03-03) - Kakao Native Login + Auth Refactor

업데이트: 2026-04-08

## 0) 최신 메모 (2026-04-08)

- 회원탈퇴 v1 구현/배포
  - `설정 > 계정 관리`에서 회원탈퇴를 시작할 수 있도록 UI/VM 추가
  - Apple 계정은 탈퇴 직전에 네이티브 재인증으로 `authorizationCode`를 다시 받아 서버에 전달
  - 새 Edge Function `delete-account` 추가/배포 완료
  - 함수는 `device_push_tokens`, `partner_activity_notifications`, `profiles`, `auth.users`를 순차 삭제
  - Apple provider는 서버에서 사용자 정보로 직접 판별하고, `auth.admin.deleteUser()` 실패 시 public row 복구까지 수행
- iOS AdMob 배너 연결
  - iOS 호스트 앱에 Google Mobile Ads SDK + User Messaging Platform SPM 추가
  - Android/iOS 모두 메인 탭 하단 공통 배너 슬롯 사용
  - debug 빌드는 테스트 광고, release 빌드는 실제 광고 ID 사용
- 빌드 메모
  - `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 16' CODE_SIGNING_ALLOWED=NO build` 성공
  - `embedAndSignAppleFrameworkForXcode`는 Xcode 환경 밖에서 단독 실행하면 `Could not infer iOS target architectures`로 실패할 수 있음

## 0-1) 이전 메모 (2026-04-01)

- push 토큰 계정 전환 보강
  - 새 Edge Function `manage-device-push-token` 추가/배포
  - 클라이언트가 `device_push_tokens`를 직접 수정하지 않고, 서버에서 `installation_id` 기준 stale row 정리 후 `sync/deactivate`
  - 남은 일은 한 기기에서 계정 A/B 전환 QA 확인
- 저장 바텀시트 UX 개선
  - 가계부/일정/하객 저장 시트는 저장 성공 시에만 닫힘
  - 실패 시 시트를 유지하고 입력값 보존
- 알림센터 unread 정책 수정
  - `새 활동`은 fetch 시 자동 읽음 처리하지 않음
  - 활동 카드를 눌렀을 때만 read 처리
  - 실패 시 optimistic update 복구
- 가계부 안정화
  - `expenses` RLS를 예산 항목 없는 지출도 허용하도록 조정
  - 지출 저장을 client-generated UUID + plain insert로 바꿔 `JsonDecodingException` 재발을 줄임
  - 지출 필터 기본 기간을 `전체 기간`으로 변경
  - 예산 화면의 `목표` 문구를 `예산`으로 통일
- Tools/더미 데이터 정리
  - `웨딩홀 비교`, `가치관 설문`은 현재 `준비 중인 기능이에요.` 안내만 노출
  - shared 공용 상태의 `sample()` 더미 데이터 제거
  - Android smoke test를 실제 최소 상태 생성 방식으로 보강
- Build 메모
  - clean 후 `BuildConfig.kt`가 사라져 깨지던 문제를 Gradle 생성 태스크 수정으로 보강
  - 현재 `generateBuildConfig`는 configuration cache 경고가 남지만 Android debug / Android test assemble / iOS simulator compile은 통과

## 0-2) 이전 메모 (2026-03-31)

- iOS 원격 push 실수신 확인
  - Apple Developer App ID / APNs key / Firebase APNs 업로드 / Xcode capability 설정 후 iPhone 실기기에서 push 수신 확인
  - `push_token_synced platform=ios` 이후 실제 시스템 알림 표시까지 확인
- Apple 로그인 구현 완료
  - iOS는 브라우저 OAuth code exchange 대신 네이티브 `AuthenticationServices` + Supabase `signInWith(IDToken)` 경로 사용
  - Supabase Apple Provider `Client IDs`는 `kr.jm.weddingledger.service, kr.jm.weddingledger.WeddingLedger` 조합으로 맞춰야 함
  - `iosApp.entitlements`에 `com.apple.developer.applesignin` 추가
- Kakao/iOS 호스트 안정화
  - Apple 로그인 복귀 시 Kakao URL 판별이 잘못 끼어들지 않도록 방어
  - Kakao SDK는 앱 시작 시 안전 초기화
- 현재 남은 iOS 메모
  - Firebase `GoogleService-Info.plist` 관련 경고 로그는 남을 수 있으나, 현재 기준 FCM 토큰 발급 / Supabase 토큰 등록 / 실수신은 모두 성공

## 0-3) 이전 메모 (2026-03-22)

- 홈 결산 카드 정리
  - 큰 금액에서도 사용률 칩이 세로로 찌그러지지 않도록 금액 아래 행으로 이동
  - 홈 중간 CTA 카드와 FAB 제거
- 파트너 활동 알림 추가
  - 지출 추가 / 일정 추가 시 상대방 대상 활동 기록 생성
  - 알림센터가 `새 활동 / 리마인더` 2섹션 구조로 확장됨
- 원격 푸시 인프라 추가
  - Supabase 테이블: `device_push_tokens`, `partner_activity_notifications`
  - 컬럼: `expenses.created_by`, `schedule_events.created_by`
  - Edge Function: `send-partner-activity-push`
  - Android FCM, iOS Firebase Messaging + APNs 브리지 코드 추가
- 실제 OS push까지 쓰려면 수동 설정 필요
  - Android `local.properties` Firebase 값
  - iOS `iosApp/Configuration/Config.xcconfig` Firebase 값
  - Supabase secret `FCM_SERVICE_ACCOUNT_JSON`

## 0-4) 추가 메모 (2026-03-29)

- Android 파트너 활동 push는 실제 수신까지 확인함
  - FCM 토큰 등록 성공
  - Edge Function 발송 성공
  - Android foreground `onMessageReceived` 확인
  - 시스템 알림 `notification_displayed` 확인
- Android push 디버깅은 `WL_SUPABASE` 태그 기준으로 본다
  - `remote_push_token_fetch_success`
  - `push_token_synced`
  - `partner_activity_dispatch_start/success`
  - `onMessageReceived`
  - `notification_displayed`
- `device_push_tokens` 중복 key 충돌 방지 로직 추가
- `send-partner-activity-push`는 caller JWT 의존을 제거하고, `created_by` / `wedding_id` 기반으로 상대를 찾도록 변경 후 재배포함
- 일정 저장은 client-generated UUID + 안전 파싱으로 바꿔 `JsonDecodingException` 재발을 줄임
- 일정 추가 바텀시트의 제목/장소 placeholder 예시 문구 제거
- 로그인 화면 약관/개인정보 동의 흐름 보강
  - 실제 Google Sites 문서 URL 연결
  - 소셜 로그인 전 동의 체크 강제
  - 동의 완료 시 현재 약관 버전을 DataStore에 저장해 로그인 화면에서 반복 노출되지 않도록 처리
- `join_wedding`는 원격 DB 함수에서 제3자 합류가 불가능하도록 직접 수정함
  - SQL 파일은 repo에 남기지 않고 원격 DB에만 반영
- iOS는 Firebase 값 반영 구조까지만 완료
  - Apple Developer 계정 + APNs capability가 준비돼야 원격 push / Apple 로그인까지 완결 가능

## 0-5) 이전 메모 (2026-03-16)

- Android Studio 업데이트 후 IDE에서 `convertPbxprojToJson` / `plutil` 오류가 날 수 있었음.
  - 프로젝트 자체 문제는 아니었고, 터미널에서 Android Studio를 실행하면 정상 sync/실행됨.
  - 실행 명령:

```bash
"/Applications/Android Studio.app/Contents/MacOS/studio" /Users/jm/StudioProjects/WeddingLedger
```

- 현재 구조 기준 Android 앱 모듈은 `androidApp`이다.
  - `composeApp`는 shared KMP library라 `:composeApp:assembleDebug`가 없다.
  - 올바른 Android 빌드 명령: `./gradlew :androidApp:assembleDebug`

- Supabase 데이터는 QA용으로 초기화해둔 상태다.
  - `public` 앱 데이터 + `auth.users` 포함 인증 데이터 전부 삭제 완료
  - 다음 QA는 회원가입부터 다시 시작하면 됨

- 가계부 탭은 다시 단순화했다.
  - `예산 / 지출` 탭 분리
  - 기본 탭은 `예산`
  - 상세 결산/필터는 기본 접힘
  - 내일 첫 확인 포인트는 이 가계부 UX가 실제로 쉬워졌는지다

## 1) 이번 세션 핵심 요약

- Kakao 로그인은 **네이티브 SDK 방식으로 구현 완료**됨.
  - iOS: `KakaoSDKUser`의 `loginWithKakaoTalk / loginWithKakaoAccount`
  - Android: Kakao SDK 네이티브 로그인
  - 공통: Kakao `idToken` + `accessToken`을 Supabase `signInWith(IDToken)` (`provider = Kakao`)로 교환
- 로그인 화면 소셜 버튼 정렬/동작 변경 반영됨.
  - iOS: `Kakao -> Apple -> Google`
  - Android: `Kakao -> Google -> Apple`
- 이메일 OTP 보조 로그인은 접힘/펼침 UI로 동작.
- 비밀번호 회원가입/로그인은 비활성 처리.

## 1-1) 후속 반영 사항 (2026-03-08)

- Android 구조를 장기 대응 형태로 정리함.
  - `composeApp`는 shared Android-KMP library
  - `androidApp`는 Android 앱 셸(`MainActivity`, `WeddingLedgerApp`, `AndroidManifest`, launcher 리소스`) 담당
- `composeApp`는 `com.android.kotlin.multiplatform.library` 플러그인으로 전환됨.
- Auth/Onboarding의 문자열 기반 에러 분기를 타입 기반으로 정리함.
- 공통 Supabase repository 지원 로직(`WeddingIdResolver`, 공통 오류 로그`)을 추출함.
- 회귀 테스트를 보강함.
  - `SupabaseRepositorySupportTest`
  - `AuthResultTest`
  - 관련 ViewModel 테스트 갱신
- 이메일/OTP 로그인 UI는 현재 주석 처리로 숨겨져 있음.
  - 구현은 유지되어 주석 해제만으로 복구 가능
- Android Kakao 로그인 키는 루트 `local.properties`의 `KAKAO_NATIVE_APP_KEY`를 사용함.
  - iOS `Config.xcconfig`에만 키가 있으면 Android에서 `KakaoSdk.isInitialized == false` 상태가 됨

## 2) 이슈 해결 결과 (2026-03-04)

- iOS Kakao 로그인 성공 확인.
  - 주요 원인: Kakao Developers iOS Bundle ID 불일치 및 Supabase Kakao provider 값 불일치.
  - `invalid_request: IOS bundleid validation failed` 해결 후 정상 동작 확인.
  - Supabase 단계에서 `Unacceptable audience in id_token`도 provider 설정 정합성 재확인 후 해소.
- Android Kakao 로그인 성공 확인.
  - 주요 원인: Android에서 에러가 generic 문구로 뭉개져 진단이 어려웠던 상태.
  - Android Kakao 에러 파싱(`ClientError`, `AuthError`, `ApiError`) 및 Talk 실패 시 Account 로그인 폴백 추가 후 검증 성공.
- 결과: **Kakao 로그인은 iOS/Android 모두 네이티브 SDK 경로로 동작 확인 완료**.

## 3) 다음 세션에서 바로 할 일 (우선순위)

1. 한 기기 계정 전환 시 push 토큰 누수 QA
2. 3초 강제 스플래시 완화
3. Kakao/Google/Apple 로그인 + Android/iOS push 회귀 QA 1회
4. iOS Firebase 경고(`GoogleService-Info.plist`, `FirebaseApp.configure`) 원인 정리

## 4) 필수 설정 체크리스트

### Kakao Developers
- 앱 키: 현재 사용 key 계정에서 관리되는 앱인지 확인
- OpenID Connect: ON
- 플랫폼 > iOS Bundle ID: `kr.jm.weddingledger.WeddingLedger`
- (필요 시) 동의항목: 이메일 없는 계정 허용 정책과 Supabase 설정 일치

### Supabase
- Auth > Providers > Kakao
  - Enabled = ON
  - REST API Key = Kakao REST API Key
  - Client Secret Code = Kakao Client Secret
  - Allow users without an email = 정책에 맞게 ON/OFF
- Auth > URL Configuration
  - App deep link가 기존 앱 설정과 일치하는지 확인

### 로컬 설정
- `local.properties` (커밋 금지)
  - `SUPABASE_URL`
  - `SUPABASE_ANON_KEY`
  - `KAKAO_NATIVE_APP_KEY`
  - `ENABLE_KAKAO_LOGIN=true`
  - `ENABLE_GOOGLE_LOGIN=true`
  - `ENABLE_APPLE_LOGIN=true` 또는 필요 시 기능 플래그에 맞는 값

## 5) 이번 세션 주요 코드 변경 포인트

### iOS Kakao Native
- `iosApp/iosApp/KakaoNativeSignInCoordinator.swift`
  - 네이티브 Kakao 로그인 실행
  - `SdkError` 상세 파싱/한국어 메시지 변환
  - 취소 케이스 분리
- `iosApp/iosApp/iOSApp.swift`
  - `onOpenURL`에서 Kakao URL 핸들링 추가
- `iosApp/iosApp/Info.plist`
  - Kakao URL scheme 및 key 참조
- `iosApp/Configuration/Config.xcconfig`
  - Kakao native app key 관련 빌드 설정
- `iosApp/iosApp.xcodeproj/project.pbxproj`
  - Kakao iOS SDK SPM 의존성 연결

### KMP Auth 계층
- `composeApp/src/commonMain/kotlin/.../feature/auth/data/repository/AuthRepositoryImpl.kt`
  - Kakao 로그인 실패 메시지 상세화 (`AuthRestException`, `RestException`)
  - ID Token 기반 Supabase 로그인 흐름 정리
- `composeApp/src/commonMain/kotlin/.../feature/auth/presentation/*`
  - 소셜 로그인 버튼 정렬/동작
  - 이메일 OTP 접힘/펼침 UX

### 플랫폼별 Kakao 브리지
- Android:
  - `composeApp/src/androidMain/kotlin/.../feature/auth/data/platform/AndroidAuthActivityHolder.kt`
  - `composeApp/src/androidMain/kotlin/.../feature/auth/data/platform/KakaoNativeSignInClient.android.kt`
- iOS:
  - `composeApp/src/iosMain/kotlin/.../feature/auth/data/platform/KakaoNativeSignInClient.ios.kt`
- Common bridge:
  - `composeApp/src/commonMain/kotlin/.../feature/auth/data/platform/IosKakaoNativeSignInBridge.kt`
  - `composeApp/src/commonMain/kotlin/.../feature/auth/data/platform/KakaoNativeSignInClient.kt`

## 6) 빌드 검증 상태

- Android: `./gradlew :androidApp:assembleDebug` 성공
- iOS: `xcodebuild ... build` 성공
- 참고: 간헐적으로 Xcode `build.db` lock 이슈가 있었고, 아래 방식으로 복구됨

```bash
pkill -f xcodebuild || true
pkill -f XCBBuildService || true
DERIVED="/Users/jm/Library/Developer/Xcode/DerivedData/iosApp-ecbrzyqtqitrscatkmbvqnbwzyoq/Build/Intermediates.noindex/XCBuildData"
rm -f "$DERIVED/build.db" "$DERIVED/build.db-shm" "$DERIVED/build.db-wal"
rm -rf "$DERIVED"/*.xcbuilddata
```

## 7) 내일 다른 컴퓨터에서 시작 절차

1. `main` 최신 pull
2. `local.properties` 생성/값 입력
3. iOS는 Xcode에서 package resolve 후 build 1회
4. Kakao 로그인 테스트 1회
5. 실패 문구/로그 원문 캡처 후 원인 확정
