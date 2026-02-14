# WeddingLedger (KMP/CMP) 프로젝트 규칙

이 저장소는 **Kotlin Multiplatform(KMP)** + **Compose Multiplatform(CMP)** 기반으로 **Android + iOS**를 동시에 타겟합니다.
Codex(및 모든 기여자)는 변경 작업 시 아래 규칙을 반드시 준수합니다.

## 아키텍처 (필수 규칙)

- **Feature-first**: 레이어 중심이 아니라 기능(Feature) 중심으로 코드를 묶습니다.
- 각 Feature 내부는 **Clean Architecture**로 구성합니다.
- UI는 **MVVM**을 지킵니다.
- 기본 원칙은 **Stateless Screen**입니다.
  - Screen Composable은 `UiState`와 콜백을 파라미터로 받아서 그리기만 합니다.
  - 비즈니스 상태는 ViewModel 및 domain/data 레이어에만 둡니다(Composable 내부에 두지 않음).
- **단방향 데이터 흐름(UDF)**을 선호합니다. (`Action -> ViewModel -> State/Effect -> UI`)

## Source Set 및 코드 위치

- 공용(Shared): `composeApp/src/commonMain/kotlin`
- Android 전용: `composeApp/src/androidMain/kotlin`
- iOS 전용: `composeApp/src/iosMain/kotlin`
- iOS 호스트 앱(Swift): `iosApp/` (SwiftUI는 최소화하고 Compose 호스팅에 집중)

`commonMain`에서 Android/iOS API를 직접 import 하지 않습니다. 필요 시 `expect/actual`을 사용합니다.

## 패키지/폴더 컨벤션

- `kr.jm.weddingledger.core.*`: 공통 인프라/횡단 관심사(DI, config, network, persistence, design system, navigation)
- `kr.jm.weddingledger.feature.<featureName>.*`: 기능(Feature) 단위의 세로 슬라이스

### Feature 템플릿

새 Feature `foo`를 추가할 때:

- `feature/foo/presentation`
  - `FooRoute` (stateful glue): 상태 수집 + effect 처리(네비게이션 등)
  - `FooScreen` (stateless): `FooScreen(state, onAction)`
  - `FooViewModel`, `FooUiState`, `FooAction`, (선택) `FooEffect`
- `feature/foo/domain`
  - 모델, repository interface, use case
- `feature/foo/data`
  - DTO, mapper, data source, repository 구현체

### 의존성 규칙

- `presentation -> domain`
- `data -> domain`
- `domain -> (없음)` (Compose 금지, 플랫폼 import 금지, Koin 금지, Ktor 금지)

`core`는 feature에서 사용 가능하지만, `androidMain/iosMain` 하위가 아닌 이상 `core`는 가능한 한 플랫폼 중립적으로 유지합니다.

## MVVM + Stateless UI 규칙

- 기본 패턴:
  - `@Composable fun FooRoute(...)`
  - `@Composable fun FooScreen(state: FooUiState, onAction: (FooAction) -> Unit)`
- ViewModel 규칙:
  - `StateFlow<FooUiState>`를 expose
  - 입력은 `onAction(action: FooAction)`로 받기
  - 1회성 이벤트(네비게이션, 토스트, 다이얼로그)는 `FooEffect`(`Channel`/`SharedFlow`)로 처리하고 `UiState`에 인코딩하지 않기
- Composable의 로컬 상태(허용 범위):
  - 텍스트필드 입력, 포커스, 스크롤 등 “순수 UI”에 한정
  - 도메인 상태를 `remember { mutableStateOf(...) }`로 들고 있지 않기

## 네비게이션

- 네비게이션은 전용 위치에서 처리합니다(예: `core/navigation` 또는 `AppNavHost`).
- Screen은 `NavController`를 직접 호출하지 않습니다.
- Route에서 ViewModel의 `Effect`를 collect하여 네비게이션을 수행합니다.

## DI (Koin)

- Feature마다 Koin module을 `feature/<name>/di` 아래에 둡니다.
- 모듈 aggregation은 `core/di/appModules()`에서 중앙집중으로 관리합니다.
- `*Route`를 제외하고 Composable 내부에서 직접 의존성 해석(Resolve)하지 않습니다.

## 네트워킹

- HTTP는 **Ktor client**를 사용합니다(이미 포함: Android `okhttp`, iOS `darwin`).
- Supabase SDK가 포함되어 있습니다. 다만 인프라 디테일로 취급합니다.
  - Supabase/Ktor 접근은 `data` 레이어에서만
  - `domain/presentation`에는 도메인 친화적인 repository interface만 노출
- Composable에서 네트워크 클라이언트를 직접 호출하지 않습니다.

## UI/디자인 규칙

- 기본 폰트는 **Pretendard**를 사용합니다.
- 기본은 라이트 모드이며, 테마는 **미니멀/러블리** 2가지를 지원합니다(설정에서 전환).
- 사용자에게 노출되는 문구는 **한국어**를 기본으로 사용합니다.
- 금액은 **원화(KRW)** 형식으로 통일합니다(예: `₩1,234,000`).

## 설정 / 시크릿

- 시크릿은 로컬 설정(`local.properties`)에서만 주입하고 커밋하지 않습니다.
- 새 설정 키를 도입할 때:
  - `README.md`에 로컬 셋업 문서화
  - `local.properties.example` 추가/갱신(실제 시크릿 값 금지)

## 퍼시스턴스

- Room/SQLite 사용 시:
  - Entity/DAO는 전용 패키지(`core/database` 또는 feature의 `data/local`)에 둡니다.
  - schema export 설정을 유지합니다(이 저장소는 `composeApp/schemas`를 이미 사용하도록 구성됨).

## 테스트

- 의미 있는 domain/use-case 로직은 `commonTest` 커버리지를 포함합니다.
- 가능하면 mocking보다 fake를 선호합니다.

## 작업 방식 (Codex)

- 변경은 작고 리뷰 가능하게 유지합니다.
- 명시적인 요청이 없으면 광범위 리팩토링을 피합니다.
- 의미 있는 변경 후에는 최소 1회 sanity build를 수행합니다.
  - Android: `./gradlew :composeApp:assembleDebug`
  - iOS: Xcode에서 `iosApp` 열어 빌드/실행
