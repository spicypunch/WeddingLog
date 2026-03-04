# Session Handover (2026-03-03) - Kakao Native Login + Auth Refactor

업데이트: 2026-03-04

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

1. Apple Developer 유료 계정 준비 후 Apple 로그인 provider 설정 진행
2. Supabase Apple provider + iOS capability 설정 후 실기기 로그인 검증
3. Kakao/Google/Apple 로그인 회귀 QA 1회

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
  - `ENABLE_APPLE_LOGIN=false`

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

- Android: `./gradlew :composeApp:assembleDebug` 성공
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
