# QA 체크리스트

마지막 갱신: 2026-05-05

## 0. 준비

- [ ] 최신 `main` 브랜치 pull 완료
- [ ] `local.properties`의 Supabase/Kakao 키 확인
- [ ] Kakao Developers Android 패키지명 `kr.jm.weddinglog` 및 Play App Signing key hash `wpThrWMOAPe65XjIM/8p76kEG7Q=` 등록 확인
- [ ] AdMob 정본 앱 ID/광고 단위 ID 확인
  - [ ] Android `ADMOB_ANDROID_APP_ID=ca-app-pub-6140257895494497~2545168246`
  - [ ] Android `ADMOB_ANDROID_BANNER_UNIT_ID=ca-app-pub-6140257895494497/2073111096`
  - [ ] iOS `ADMOB_IOS_APP_ID_RELEASE=ca-app-pub-6140257895494497~4268029592`
  - [ ] iOS `ADMOB_IOS_BANNER_UNIT_ID_RELEASE=ca-app-pub-6140257895494497/8417431709`
- [ ] 현재 배포 버전 확인
  - [ ] Android `versionName=1.3.1`, `versionCode=5`
  - [ ] iOS `MARKETING_VERSION=1.3.1`, `CURRENT_PROJECT_VERSION=7`
- [ ] 이번 배포 산출물 확인
  - [ ] Android AAB: `build/distributions/WeddingLog-android-1.3.1-b5.aab`
  - [ ] iOS IPA: `build/distributions/WeddingLog-ios-1.3.1-b7.ipa`
- [ ] Android 실제 배포 `applicationId`가 `kr.jm.weddinglog`인지 확인
- [ ] iOS가 iPhone only인지 확인
  - [ ] `TARGETED_DEVICE_FAMILY = 1`
- [ ] Google 로그인은 첫 출시 안정화를 위해 화면에서 미노출 상태인지 확인
- [ ] `ENABLE_ADMOB_BANNER=true`에서 AdMob 배너가 바텀 내비게이션 위에 보이는지 확인
- [ ] `ENABLE_ADMOB_BANNER=false`에서 AdMob 배너와 빈 여백이 모두 사라지는지 확인
- [ ] 파트너 활동 푸시 테스트를 할 경우 Firebase 값 확인
  - [ ] Android `local.properties`
    - [ ] `FIREBASE_ANDROID_APPLICATION_ID`
    - [ ] `FIREBASE_PROJECT_ID`
    - [ ] `FIREBASE_API_KEY`
    - [ ] `FIREBASE_GCM_SENDER_ID`
  - [ ] iOS `iosApp/Configuration/Config.local.xcconfig`
    - [ ] `FIREBASE_IOS_GOOGLE_APP_ID`
    - [ ] `FIREBASE_PROJECT_ID`
    - [ ] `FIREBASE_API_KEY`
    - [ ] `FIREBASE_GCM_SENDER_ID`
  - [ ] Supabase secret `FCM_SERVICE_ACCOUNT_JSON` 설정 여부 확인
- [ ] Android Studio 업데이트 후 `plutil` / `convertPbxprojToJson` 오류가 나면 터미널 실행으로 IDE 열기
  - [ ] `"/Applications/Android Studio.app/Contents/MacOS/studio" /Users/jm/StudioProjects/WeddingLedger`
- [ ] Android debug 빌드 성공
  - [ ] `./gradlew :androidApp:assembleDebug`
- [ ] Android instrumented test assemble 성공
  - [ ] `./gradlew :composeApp:assembleAndroidTest`
- [ ] iOS simulator 빌드 성공
  - [ ] `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build -quiet`
- [ ] AdMob 테스트/실광고 설정값 확인
  - [ ] Android `ADMOB_ANDROID_APP_ID`, `ADMOB_ANDROID_BANNER_UNIT_ID`
  - [ ] iOS `Config.local.xcconfig`의 `ADMOB_IOS_APP_ID_RELEASE`, `ADMOB_IOS_BANNER_UNIT_ID_RELEASE`
  - [ ] iOS TestFlight에서 하단에 큰 빈 여백이 생기지 않는지 확인
- [ ] 소셜 로그인 테스트 계정 준비
  - [ ] Android Kakao 계정 1개 이상
  - [ ] iOS Kakao 계정 1개 이상
  - [ ] iOS Apple 로그인 테스트 가능 Apple ID
- [ ] 현재 Supabase 데이터가 초기화된 상태인지 확인
  - [ ] 회원가입부터 다시 시작하는 흐름으로 QA

## 1. 인증 / 온보딩

### 1-1. 소셜 로그인
- [ ] 로그인 화면에서 약관 동의 체크가 처음 1회만 보이는지 확인
- [ ] `이용약관`, `개인정보 처리방침` 링크가 실제 문서로 열리는지 확인
- [ ] 체크하지 않으면 소셜 로그인이 막히는지 확인
- [ ] Android Google 로그인 버튼이 보이지 않음
- [ ] Android 이메일 로그인 버튼/입력 영역이 보이지 않음
- [ ] Android Kakao 로그인 성공
- [ ] iOS Apple 로그인 성공
  - [ ] Apple 인증 후 앱으로 정상 복귀
  - [ ] 홈 진입 및 세션 생성 확인
- [ ] iOS Google 로그인 버튼이 보이지 않음
- [ ] iOS 이메일 로그인 버튼/입력 영역이 보이지 않음
- [ ] iOS Kakao 로그인 성공

### 1-4. 회원탈퇴
- [ ] Android Kakao 계정 회원탈퇴 성공
- [ ] iOS Kakao 계정 회원탈퇴 성공
- [ ] iOS Apple 계정 회원탈퇴 성공
  - [ ] Apple 재인증 시트가 다시 뜨는지 확인
  - [ ] 재인증 완료 후 탈퇴가 마무리되는지 확인
- [ ] 탈퇴 성공 후 로그인 화면으로 복귀하는지 확인
- [ ] 탈퇴한 계정 세션으로 앱 재실행 시 자동 로그인되지 않는지 확인
- [ ] 탈퇴 실패 시 로그인 상태가 유지되고 오류 메시지가 보이는지 확인

### 1-2. 배포 후 이메일 로그인 제거 확인
- [ ] 로그인 화면에 `이메일로 로그인` 텍스트가 보이지 않음
- [ ] 이메일/비밀번호 입력창이 보이지 않음
- [ ] OTP 받기/OTP 입력/인증 메일 재전송 UI가 보이지 않음
- [ ] 약관 동의 후에도 로그인 수단은 소셜 버튼만 노출됨

### 1-3. 온보딩
- [ ] `혼자 시작하기` 경로 완료
- [ ] `공유 코드로 시작하기` 경로 완료
- [ ] 초대 코드 6자리 숫자 표시 확인
- [ ] 설정 화면에서 초대 코드 복사 확인
- [ ] 계정 전환 시 온보딩 상태가 이전 계정에서 새지 않는지 확인
  - [ ] 온보딩 완료된 기존 계정으로 로그인
  - [ ] 로그아웃
  - [ ] 새로 만든 계정으로 로그인
  - [ ] 홈으로 바로 가지 않고 `혼자 시작하기 / 공유 코드로 시작하기` 화면이 표시됨
  - [ ] 앱 완전 종료 후 재실행해도 새 계정 기준 화면/상태가 유지됨
  - [ ] 이후 온보딩 완료 시에만 홈으로 이동함

## 2. 홈 탭

- [ ] D-day 정상 표시
- [ ] 총 예산 / 총 지출 / 남은 금액 표시 확인
- [ ] 큰 금액(`₩52,000,008` 이상)에서도 결산 카드 금액과 사용률 칩이 깨지지 않음
- [ ] 신랑 / 신부 / 공동 분담 카드 표시 확인
- [ ] 카테고리 결산 리스트 표시 확인
- [ ] 다가오는 일정 프리뷰 표시 확인
- [ ] 최근 지출 프리뷰 표시 확인
- [ ] `가계부 보기` 이동 동작 확인
- [ ] 중간 CTA 카드(`지출 추가하기 / 일정 확인하기`)가 없음
- [ ] 홈 FAB가 없음

## 3. 가계부 탭

### 3-1. 기본 구조
- [ ] 하단 탭명이 `가계부`로 보임
- [ ] 상단 제목 `예산과 지출` 표시 확인
- [ ] `예산` 탭이 기본 선택되어 보임
- [ ] `예산 / 지출` 탭 전환이 직관적인지 확인
- [ ] 총 예산 / 총 지출 / 남은 금액 요약 확인
- [ ] 남은 결제 섹션 표시 확인
- [ ] `+` 메뉴에서 `카테고리 관리`, `예산 항목 추가`, `지출 추가`가 둘 다 동일하게 보이는지 확인

### 3-2. 예산 항목
- [ ] 카테고리 만들기/관리 진입이 쉬운지 확인
- [ ] 예산 항목 추가 성공
- [ ] 예정일 저장/수정/삭제 확인
- [ ] 카테고리별 예산/사용/남은 금액 반영 확인
- [ ] `기타` 카테고리가 항상 마지막 순서로 유지되는지 확인

### 3-3. 지출 기록
- [ ] `지출` 탭에서 기록 흐름이 자연스러운지 확인
- [ ] 기본 기간 필터가 `전체 기간`인지 확인
- [ ] 지출 추가 성공
- [ ] 카테고리만 선택하고 예산 항목 없이 저장 가능
- [ ] 카테고리 + 예산 항목까지 선택한 저장도 가능
- [ ] 저장 실패 시 지출 추가 바텀시트가 닫히지 않고 입력값이 유지되는지 확인
- [ ] 업체명 입력 반영 확인
- [ ] 결제수단 `공동계좌` 선택 시 결제자 UI 숨김 확인
- [ ] 기본 결제자가 현재 로그인 사용자 역할(`신랑/신부`)에 맞는지 확인
- [ ] 신랑 / 신부 / 공동 분담 요약 반영 확인
- [ ] 최근 지출 목록 반영 확인
- [ ] 필터가 기본 숨김인 상태가 더 이해하기 쉬운지 확인

### 3-4. 실무 시나리오
- [ ] `웨딩홀 대관료 400만원` 예산 항목 생성
- [ ] 같은 카테고리에 예산 항목 미연결 지출 추가
- [ ] 같은 카테고리에 예산 항목 연결 지출 추가
- [ ] 카테고리 총 사용액에 두 지출이 모두 합산되는지 확인
- [ ] 예산 항목 진행률에는 연결된 지출만 반영되는지 확인
- [ ] 처음 사용자 입장에서 “무엇부터 해야 할지” 3초 안에 감이 오는지 체크

## 4. 일정 탭

### 4-1. 일정
- [ ] 일정 추가 / 수정 / 삭제 성공
- [ ] 일정 추가 시 `JsonDecodingException` 없이 저장 성공
- [ ] 일정 저장 실패 시 바텀시트가 닫히지 않고 입력값이 유지되는지 확인
- [ ] 반복 일정(`없음/매주/매월`) 저장 확인
- [ ] 선택한 날짜 일정 섹션 표시 확인
- [ ] 다가오는 일정이 날짜 선택과 무관하게 계속 보이는지 확인
- [ ] 일정 제목 / 장소 입력창에 예시 placeholder가 보이지 않는지 확인

### 4-2. 체크리스트
- [ ] 체크리스트 추가 / 수정 / 삭제 성공
- [ ] 체크 완료 반영 확인
- [ ] 마감 임박 체크리스트 프리뷰 표시 확인

### 4-3. 알림 연계
- [ ] 일정 리마인더 예약 확인
- [ ] 일정/체크리스트 수정 시 기존 리마인더 정리 확인

## 5. 하객 탭

### 5-1. 예상 하객
- [ ] 탭 `예상 하객 | 축의대` 전환 동작 확인
- [ ] 예상 총 하객 / 보증인원 / 차이 표시 확인
- [ ] 보증인원 설정 바텀시트 저장 확인
- [ ] 보증인원/하객 저장 실패 시 바텀시트가 닫히지 않고 입력값이 유지되는지 확인
- [ ] 신랑측 / 신부측 / 공통 요약 확인
- [ ] 상태별 요약(`꼭 옴/애매/안 옴`) 확인
- [ ] 검색 동작 확인
- [ ] 구분 필터(`전체/신랑측/신부측/공통`) 확인
- [ ] 상태 필터(`전체/꼭 옴/애매/안 옴/동반 있음`) 확인

### 5-2. 예상 하객 계산
- [ ] `꼭 옴 + 동반 1명` => 예상 2.0명 반영 확인
- [ ] `애매 + 동반 1명` => 예상 1.0명 반영 확인
- [ ] `안 옴` => 예상 0명 반영 확인
- [ ] 상태 칩 탭으로 빠른 상태 변경 확인

### 5-3. 축의대
- [ ] 실제 참석 여부 입력 확인
- [ ] 실제 축의금 입력 확인
- [ ] 총 축의금 합계 반영 확인
- [ ] 측별 축의금 요약 반영 확인
- [ ] `입력 완료 / 미입력` 필터 확인
- [ ] 불참 처리 시 `입력 완료`로 분류되는지 확인

## 6. 설정 / 알림센터

- [ ] 프로필 수정 저장 확인
- [ ] 계정 관리 화면 진입 확인
- [ ] 계정 관리 화면에서 `회원탈퇴` 섹션 노출 확인
- [ ] 앱 정보 화면 진입 확인
- [ ] 알림센터 진입 확인
- [ ] 알림센터가 `새 활동 / 리마인더` 두 섹션으로 보이는지 확인
- [ ] 일정/체크리스트/예산 초과 알림 피드 노출 확인
- [ ] 파트너 활동(`지출 추가`, `일정 추가`)이 `새 활동` 섹션에 나타나는지 확인
- [ ] `새 활동` unread 표시가 보이는지 확인
- [ ] 알림센터에 들어오기만 해서는 unread가 사라지지 않는지 확인
- [ ] `새 활동` 카드를 직접 눌렀을 때만 읽음 처리되는지 확인
- [ ] OS 알림 설정 화면 이동 확인

## 7. 파트너 활동 푸시

- [ ] 신랑 계정에서 지출 추가
  - [ ] 신부 계정 알림센터 `새 활동`에 기록 생성
  - [ ] Android 수신 기기 foreground에서 `onMessageReceived` + `notification_displayed` 로그 확인
  - [ ] Android에서 heads-up 또는 시스템 알림 표시 확인
  - [ ] iPhone 수신 기기에서 시스템 push 표시 확인
- [ ] 신부 계정에서 일정 추가
  - [ ] 신랑 계정 알림센터 `새 활동`에 기록 생성
  - [ ] Android 수신 기기 foreground에서 `onMessageReceived` + `notification_displayed` 로그 확인
  - [ ] Android에서 heads-up 또는 시스템 알림 표시 확인
  - [ ] iPhone 수신 기기에서 시스템 push 표시 확인
- [ ] 자기 자신에게는 같은 활동 알림이 오지 않음
- [ ] 토큰이 없는 환경에서도 지출/일정 저장이 실패하지 않음
- [ ] 동일 Android 기기에서 계정 전환 후 이전 계정 알림이 새지 않는지 확인
- [ ] `device_push_tokens` 중복 충돌 없이 `push_token_synced`가 반복 성공하는지 확인
- [ ] 로그아웃 시 `push_token_deactivated` 로그 확인
- [ ] 새 로그인 시 `push_token_synced` 로그 확인
- [ ] iOS 수신 기기에서도 `push_token_synced platform=ios` 이후 실제 수신이 유지되는지 확인

## 8. 회귀 체크

- [ ] 로그아웃 후 다른 계정 로그인 시 데이터 섞임 없음
- [ ] 로그아웃 후 다른 계정 로그인 시 이전 계정의 온보딩 완료 상태가 재사용되지 않음
- [ ] 신랑/신부 계정 각각 동일 wedding 데이터 조회 확인
- [ ] 앱 재실행 후 가계부 필터/상태 복원 확인
- [ ] 하객/가계부/일정 변경 후 홈 요약이 최신 상태로 반영되는지 확인
- [ ] 회원탈퇴 후 DB 정리 확인
  - [ ] `profiles`에서 탈퇴 사용자 row 삭제
  - [ ] `device_push_tokens`에서 탈퇴 사용자 row 삭제
  - [ ] `partner_activity_notifications`에서 `actor_user_id` / `target_user_id` 관련 row 삭제
  - [ ] `auth.users`에서 탈퇴 사용자 삭제
  - [ ] 탈퇴 사용자가 생성한 `expenses.created_by`, `schedule_events.created_by`가 남은 경우 `null` 처리되는지 확인

## 9. 미확인 / 보류

- [ ] `./gradlew :androidApp:connectedDebugAndroidTest`
  - 디바이스/에뮬레이터 연결 환경에서 1회 실행 필요
- [ ] `웨딩홀 비교`, `가치관 설문` 클릭 시 `준비 중인 기능이에요.` 안내 확인
- [ ] Android/iOS 메인 탭 하단 테스트 배너 표시 확인
- [ ] iOS Firebase 초기화 경고 정리
  - `The default Firebase app has not yet been configured`
  - `Could not locate configuration file: 'GoogleService-Info.plist'`
- [ ] `delete-account` 운영 메모 확인
  - [ ] 현재 함수는 `--no-verify-jwt` 배포 상태이며 함수 내부 `auth.getUser(jwt)`로 인증 검증
  - [ ] Android에서 탈퇴 시 `401 Invalid JWT` 재현 여부와 자동 refresh 재시도 로그 확인

## 10. 출시 전 리스크 확인

- [ ] 로그아웃/계정 전환 시 이전 계정 push 토큰이 새지 않도록 처리됐는지 검증
- [ ] 3초 강제 스플래시 정책 유지 여부 결정
- [ ] `generateBuildConfig` configuration cache 경고 정리 여부 결정
