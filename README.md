# WeddingLedger

Kotlin Multiplatform + Compose Multiplatform 기반의 웨딩 준비 앱입니다.  
Android/iOS 공용 코드로 일정, 예산, 하객, 온보딩/연결 코드, 알림센터 기능을 제공합니다.

## 기술 스택

- Kotlin Multiplatform (KMP)
- Compose Multiplatform (CMP)
- Koin (DI)
- Ktor Client
- Supabase (Auth + Postgrest + Realtime)
- Room/SQLite + DataStore

## 프로젝트 구조

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
```

## 빌드/테스트

```bash
cd /Users/mackim/AndroidStudioProjects/WeddingLedger

# Android debug 빌드
./gradlew :composeApp:assembleDebug

# Android unit test
./gradlew :composeApp:testDebugUnitTest

# iOS 공용 코드 컴파일 확인
./gradlew :composeApp:compileTestKotlinIosSimulatorArm64
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
  - 온보딩의 초대 코드 공유 단계를 필수 경로에서 제거(코드 공유는 설정 화면에서 선택적으로 수행)
- iOS 키보드 대응 보강:
  - SwiftUI 호스트의 키보드 자동 safe-area 회피를 비활성화해 이중 보정 제거
  - 전역 탭 dismiss는 키보드가 보일 때만 동작하도록 제한(첫 포커스 탭 간섭 방지)
- 회원가입/온보딩 프로필: 키보드 열림 시 고정 하단 CTA 대신 스크롤 영역 CTA 사용
- 입력 중심 화면 공통 정책:
  - `core/ui/keyboard/KeyboardAware.kt`로 IME 표시 상태/하단 패딩 계산 통일
  - 주요 입력 바텀시트 하단 액션 영역에 `imePadding()` 적용
- 딥링크 처리 성공/실패 안내 배너를 로그인 화면에 표시

## 인수인계

상세 변경 이력/백로그/검증 결과는 [HANDOVER.md](./HANDOVER.md)에서 확인할 수 있습니다.
