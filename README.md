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

## 최근 UX 반영 (2026-02-24)

- 온보딩 플로우 개편:
  - 첫 진입 화면을 `시작하기 / 공유받기` 분기형으로 변경
  - `시작하기` 경로: 프로필 입력(닉네임/역할/예식일) -> 초대 코드 공유
  - `공유받기` 경로: 닉네임 + 공유 코드 입력으로 바로 연결
  - 공유 코드 화면 하단의 코드 입력 UI 제거(입력은 `공유받기` 화면으로 분리)
- iOS 전역: 빈 영역 탭 시 키보드 닫힘(`endEditing(true)`)
- 회원가입/온보딩 프로필: 키보드 열림 시 고정 하단 CTA 대신 스크롤 영역 CTA 사용
- 딥링크 처리 성공/실패 안내 배너를 로그인 화면에 표시

## 인수인계

상세 변경 이력/백로그/검증 결과는 [HANDOVER.md](./HANDOVER.md)에서 확인할 수 있습니다.
