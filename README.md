# WeddingLog

결혼 준비에 필요한 예산, 일정, 하객 정보를 한곳에서 정리하는 모바일 앱입니다.

예비부부가 결혼 준비를 하다 보면 예산표, 일정표, 하객 명단이 여러 곳에 흩어지기 쉽습니다. WeddingLog는 이 정보를 하나의 앱에서 관리하고, 파트너와 같은 데이터를 함께 확인할 수 있도록 만든 Android/iOS 앱입니다.

## 주요 기능

### 예산과 지출 관리

- 전체 예산, 총 지출, 잔액을 한눈에 확인할 수 있어요.
- 예식장, 스드메, 예물, 혼수처럼 결혼 준비 항목별 예산을 나눠 관리할 수 있어요.
- 계약금, 중도금, 잔금처럼 결제 단계를 기록할 수 있어요.
- 신랑, 신부, 공동 지출을 구분해서 볼 수 있어요.

### 일정과 체크리스트

- 결혼 준비 일정을 날짜별로 정리할 수 있어요.
- 다가오는 일정과 마감이 가까운 체크리스트를 함께 확인할 수 있어요.
- 홈 화면에서 예산과 일정을 같이 볼 수 있어 준비 상황을 빠르게 파악할 수 있어요.

### 하객 관리

- 신랑측, 신부측, 공통 하객을 나눠서 관리할 수 있어요.
- 참석 가능성과 동반 인원을 기준으로 예상 하객 수를 확인할 수 있어요.
- 실제 참석 여부와 축의금 기록도 함께 정리할 수 있어요.

### 파트너와 함께 준비

- 초대 코드를 통해 파트너와 같은 결혼 준비 데이터를 공유할 수 있어요.
- 예산, 일정, 하객 정보를 함께 확인하면서 준비할 수 있어요.
- 파트너가 지출이나 일정을 추가했을 때 알림으로 확인할 수 있어요.

## 기술 스택

- Kotlin Multiplatform
- Compose Multiplatform
- Android / iOS
- Koin
- Supabase Kotlin SDK
- Supabase Auth, Postgrest, Realtime, Edge Functions
- Ktor Client for Supabase Edge Function calls
- Room / SQLite
- DataStore
- Firebase Cloud Messaging
- Google AdMob

## 개발 포인트

### KMP + CMP 기반의 양 플랫폼 앱

비즈니스 로직과 대부분의 UI를 `commonMain`에 두고 Android/iOS에서 함께 사용합니다. 플랫폼별로 달라야 하는 로그인, 푸시, 광고, 키보드 처리 같은 영역은 `expect/actual`과 네이티브 호스트 코드로 분리했습니다.

### Feature-first 구조

기능 단위로 코드를 나누고, 각 기능 안에서 `presentation`, `domain`, `data` 레이어를 분리했습니다. 화면은 최대한 stateless하게 유지하고, 상태와 이벤트 처리는 ViewModel에서 관리합니다.

### 실제 배포까지 고려한 앱

단순 토이 프로젝트가 아니라 스토어 심사, 소셜 로그인, 회원탈퇴, 푸시 알림, 광고, 앱 아이콘, 스토어 메타데이터까지 실제 출시 과정에서 필요한 요소를 함께 다뤘습니다.

## 화면

앱은 홈, 가계부, 일정, 하객, 도구 탭을 중심으로 구성되어 있습니다.

- 홈: 결산 대시보드, 다가오는 일정, 카테고리별 결산
- 가계부: 예산 관리, 지출 기록, 결제 단계 관리
- 일정: 날짜별 일정, 체크리스트, 마감 임박 항목
- 하객: 예상 하객, 실제 참석, 축의금 기록
- 도구: 결혼 준비에 필요한 보조 기능

## 프로젝트 구조

```text
androidApp/                       # Android 앱 셸
iosApp/                           # iOS 호스트 앱
composeApp/src/commonMain/kotlin  # 공용 UI/비즈니스 로직
composeApp/src/androidMain/kotlin # Android 전용 구현
composeApp/src/iosMain/kotlin     # iOS 전용 구현
```

개발 환경과 내부 설정 메모는 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)에 따로 정리했습니다.
