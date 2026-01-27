# Google Calendar 연동 기술 가이드 (Integration Guide) 🗓️

이 문서는 ModuStudy와 Google Calendar를 양방향으로 동기화하는 전체 아키텍처와 시퀀스를 설명합니다.

---

## 1. 인증 및 권한 (Authorization & Scopes) 🔐

Google Calendar API를 사용하기 위해 사용자로부터 추가 권한을 획득해야 합니다.

*   **필수 Scope**: `https://www.googleapis.com/auth/calendar` (일정 읽기/쓰기/수정/삭제 전권한)
*   **인증 흐름**:
    1.  React에서 Google 로그인 버튼 클릭
    2.  추가 권한(Scope) 동의 팝업 확인
    3.  프론트엔드에서 `code`를 받아 백엔드로 전달
    4.  백엔드는 이 `code`를 사용하여 `AccessToken`과 `RefreshToken`을 획득

---

## 2. 토큰 관리 및 오프라인 접근 (Token Management) 🔑

사용자가 앱에 접속 중이지 않을 때도 동기화를 유지하기 위해 **Refresh Token** 관리가 핵심입니다.

*   **Offline Access**: 백엔드에서 인증 요청 시 `access_type=offline` 및 `prompt=consent` 옵션을 강제하여 항상 `RefreshToken`을 발급받아야 합니다.
*   **DB 저장**: 발급받은 `RefreshToken`은 사용자 계정 정보와 매핑하여 안전하게 DB에 저장합니다.
*   **토큰 갱신**: `AccessToken` 만료 시 서버 사이드에서 `RefreshToken`을 사용해 자동으로 토큰을 갱신하고 API를 호출합니다.

---

## 3. 양방향 데이터 동기화 흐름 (Bidirectional Flow) 🔄

### ① App → Google Calendar (일정 내보내기)
사용자가 ModuStudy 앱에서 일정을 조작할 때 발생합니다.
1.  **Frontend**: 일정 생성/수정 요청 시 `syncToGoogle: true` 옵션을 포함하여 호출.
2.  **Backend**: 우리 DB에 먼저 저장 후, Google API의 `Events.insert()` 또는 `patch()` 요청.
3.  **Mapping**: Google로부터 응답받은 `eventId`를 우리 DB의 해당 필드에 저장 (이후 수정/삭제를 위한 Key).

### ② Google Calendar → App (일정 가져오기)
사용자가 Google Calendar 앱이나 웹에서 일정을 조작했을 때 발생합니다.
*   **방법 A: Webhook (권장)**
    *   Google의 `watch` API를 사용하여 특정 캘린더를 구독.
    *   변동 발생 시 Google이 우리 서버의 엔드포인트(URL)로 Noti 송신.
    *   서버는 해당 Noti를 받아 `syncToken`을 활용해 마지막 확인 이후의 변경분만 Fetch.
*   **방법 B: 폴링 (Polling)**
    *   사용자가 앱에 진입하거나 '동기화' 버튼을 눌렀을 때만 최신 데이터를 가져옴.

---

## 4. 실시간 동기화 고도화 (Sync Strategy) ⚡

*   **Sync Token**: 매번 모든 일정을 가져오는 것은 비효율적입니다. Google API 응답의 `nextSyncToken`을 보관했다가 다음 요청 시 전달하면 **Delta Sync(차이점만 전송)**가 가능합니다.
*   **낙관적 업데이트 (Optimistic UI)**: 프론트엔드에서는 서버 응답을 기다리지 않고 UI를 먼저 변경한 뒤, 백엔드와 Google Calendar 처리가 완료되면 상태를 확정합니다. 실패 시 롤백 로직이 포함됩니다.

---

## 5. 백엔드 구현 체크리스트 🛠️

1.  [ ] **Google Cloud Console**: Google Calendar API 활성화 여부 확인.
2.  [ ] **OAuth Client ID**: Web 리다이렉트 URI 설정.
3.  [ ] **Refresh Token 로직**: DB 저장 및 자동 갱신 스케줄러 구현.
4.  [ ] **Webhook Endpoint**: 외부 주소(ngrok 등)를 통해 Google 알림을 받을 수 있는 API 컨트롤러 구축.

---

> [!IMPORTANT]
> **API 라이브러리 추천 (Spring)**
> - `com.google.apis:google-api-services-calendar`를 사용하여 저수준 API 통신을 처리하는 것이 좋습니다.
