# API 명세서: 캘린더 시스템

## 개요
사용자의 개인 일정, 스터디 세션, Google Calendar를 통합 관리하는 캘린더 시스템 API입니다.

---

## 1. 개인 일정 (Personal Schedule) API

### 1.1 개인 일정 목록 조회
```
GET /api/v1/users/me/schedules
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| startDate | String | Y | 조회 시작일 (YYYY-MM-DD) |
| endDate | String | Y | 조회 종료일 (YYYY-MM-DD) |

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 123,
      "title": "알고리즘 공부",
      "description": "백준 골드 문제 풀이",
      "startDate": "2026-01-26",
      "startTime": "14:00",
      "endDate": "2026-01-26",
      "endTime": "16:00",
      "location": "스터디카페",
      "isOnline": false,
      "color": "#4285F4",
      "createdAt": "2026-01-20T10:00:00Z",
      "updatedAt": "2026-01-20T10:00:00Z"
    }
  ],
  "message": "일정 목록 조회 성공"
}
```

---

### 1.2 개인 일정 단건 조회
```
GET /api/v1/users/me/schedules/{scheduleId}
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}

**Path Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| scheduleId | Long | 일정 ID |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 123,
    "title": "알고리즘 공부",
    "description": "백준 골드 문제 풀이",
    "startDate": "2026-01-26",
    "startTime": "14:00",
    "endDate": "2026-01-26",
    "endTime": "16:00",
    "location": "스터디카페",
    "isOnline": false,
    "color": "#4285F4",
    "createdAt": "2026-01-20T10:00:00Z",
    "updatedAt": "2026-01-20T10:00:00Z"
  },
  "message": "일정 조회 성공"
}
```

---

### 1.3 개인 일정 생성
```
POST /api/v1/users/me/schedules
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}
- `Content-Type`: application/json

**Request Body:**
```json
{
  "title": "알고리즘 공부",
  "description": "백준 골드 문제 풀이",
  "startDate": "2026-01-26",
  "startTime": "14:00",
  "endDate": "2026-01-26",
  "endTime": "16:00",
  "location": "스터디카페",
  "isOnline": false,
  "color": "#4285F4"
}
```

**필드 설명:**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | Y | 일정 제목 (최대 200자) |
| description | String | N | 일정 설명 (최대 2000자) |
| startDate | String | Y | 시작 날짜 (YYYY-MM-DD) |
| startTime | String | N | 시작 시간 (HH:mm) |
| endDate | String | N | 종료 날짜 (YYYY-MM-DD) |
| endTime | String | N | 종료 시간 (HH:mm) |
| location | String | N | 장소 (최대 200자) |
| isOnline | Boolean | N | 온라인 여부 (기본값: false) |
| color | String | N | 색상 코드 (Hex, 기본값: #4285F4) |

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 123,
    "title": "알고리즘 공부",
    "description": "백준 골드 문제 풀이",
    "startDate": "2026-01-26",
    "startTime": "14:00",
    "endDate": "2026-01-26",
    "endTime": "16:00",
    "location": "스터디카페",
    "isOnline": false,
    "color": "#4285F4",
    "createdAt": "2026-01-26T10:00:00Z",
    "updatedAt": "2026-01-26T10:00:00Z"
  },
  "message": "일정 생성 성공"
}
```

---

### 1.4 개인 일정 수정
```
PUT /api/v1/users/me/schedules/{scheduleId}
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}
- `Content-Type`: application/json

**Path Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| scheduleId | Long | 일정 ID |

**Request Body:** (수정할 필드만 포함)
```json
{
  "title": "알고리즘 심화 공부",
  "startTime": "15:00"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 123,
    "title": "알고리즘 심화 공부",
    "description": "백준 골드 문제 풀이",
    "startDate": "2026-01-26",
    "startTime": "15:00",
    "endDate": "2026-01-26",
    "endTime": "16:00",
    "location": "스터디카페",
    "isOnline": false,
    "color": "#4285F4",
    "createdAt": "2026-01-20T10:00:00Z",
    "updatedAt": "2026-01-26T11:00:00Z"
  },
  "message": "일정 수정 성공"
}
```

---

### 1.5 개인 일정 삭제
```
DELETE /api/v1/users/me/schedules/{scheduleId}
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}

**Path Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| scheduleId | Long | 일정 ID |

**Response (200 OK):**
```json
{
  "success": true,
  "data": null,
  "message": "일정 삭제 성공"
}
```

---

## 2. 스터디 세션 조회 API

### 2.1 내가 속한 스터디의 세션 목록 조회
```
GET /api/v1/users/me/study-sessions
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| startDate | String | Y | 조회 시작일 (YYYY-MM-DD) |
| endDate | String | Y | 조회 종료일 (YYYY-MM-DD) |

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "studyId": 5,
      "sessionNumber": 3,
      "title": "알고리즘 스터디 3회차",
      "description": "백준 골드 문제 풀이",
      "scheduledAt": "2026-01-26T14:00:00Z",
      "durationMinutes": 120,
      "location": "온라인",
      "isOnline": true,
      "status": "SCHEDULED",
      "completedAt": null,
      "createdAt": "2026-01-20T10:00:00Z"
    }
  ],
  "message": "세션 목록 조회 성공"
}
```

**참고:** 
- 기존 `GET /api/v1/studies/{studyId}/sessions` API는 유지
- 내가 속한 모든 스터디의 세션을 한번에 조회하는 편의 API

---

## 3. Google Calendar 연동 API

### 3.1 Google Calendar 연동 상태 확인
```
GET /api/v1/calendar/google/status
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "connected": true,
    "email": "user@gmail.com",
    "lastSyncAt": "2026-01-26T10:00:00Z"
  },
  "message": "Google Calendar 상태 조회 성공"
}
```

---

### 3.2 Google OAuth 인증 URL 발급
```
GET /api/v1/calendar/google/auth-url
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "authUrl": "https://accounts.google.com/o/oauth2/v2/auth?client_id=..."
  },
  "message": "인증 URL 발급 성공"
}
```

**설명:**
- 프론트엔드는 이 URL을 새 창으로 열어 Google OAuth 인증 진행
- 인증 완료 후 `redirect_uri`로 authorization code 반환

---

### 3.3 Google OAuth 콜백 처리
```
POST /api/v1/calendar/google/callback
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}
- `Content-Type`: application/json

**Request Body:**
```json
{
  "code": "4/0AY0e-g7..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "success": true
  },
  "message": "Google Calendar 연동 성공"
}
```

**설명:**
- Authorization code로 Access Token 및 Refresh Token 발급
- DB에 사용자의 Google Token 저장
- 백엔드에서 Google Calendar API 사용 가능

---

### 3.4 Google Calendar 이벤트 동기화
```
POST /api/v1/calendar/google/sync
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}
- `Content-Type`: application/json

**Request Body:**
```json
{
  "startDate": "2026-01-01",
  "endDate": "2026-01-31"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "events": [
      {
        "id": "google-event-123",
        "summary": "팀 미팅",
        "description": "Q1 계획 논의",
        "start": {
          "dateTime": "2026-01-26T15:00:00+09:00"
        },
        "end": {
          "dateTime": "2026-01-26T16:00:00+09:00"
        },
        "location": "회의실 A"
      }
    ],
    "syncedAt": "2026-01-26T11:00:00Z"
  },
  "message": "Google Calendar 동기화 성공"
}
```

**설명:**
- Google Calendar API를 호출하여 사용자의 이벤트 조회
- 지정된 기간의 이벤트만 반환
- 캐싱 전략: 최근 동기화 시간 기준 중복 호출 방지 권장

---

### 3.5 Google Calendar 연동 해제
```
DELETE /api/v1/calendar/google/disconnect
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}

**Response (200 OK):**
```json
{
  "success": true,
  "data": null,
  "message": "Google Calendar 연동 해제 성공"
}
```

**설명:**
- DB에 저장된 Google Token 삭제
- Google API에 Token Revoke 요청 (선택)

---

## 4. 통합 조회 API (선택 구현)

### 4.1 모든 일정 통합 조회
```
GET /api/v1/calendar/all
```

**Request Headers:**
- `Authorization`: Bearer {accessToken}
- `User-Id`: {userId}

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| startDate | String | Y | 조회 시작일 (YYYY-MM-DD) |
| endDate | String | Y | 조회 종료일 (YYYY-MM-DD) |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "personal": [ /* PersonalSchedule[] */ ],
    "studySessions": [ /* StudySession[] */ ],
    "googleEvents": [ /* GoogleCalendarEvent[] */ ]
  },
  "message": "통합 일정 조회 성공"
}
```

**설명:**
- 개인 일정 + 스터디 세션 + Google 이벤트를 한 번에 조회
- 프론트엔드에서 개별 API를 3번 호출하는 대신 서버에서 병렬 처리
- 성능 최적화를 위해 권장

---

## 5. 데이터베이스 스키마

### 5.1 personal_schedule 테이블

```sql
CREATE TABLE personal_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    start_time TIME,
    end_date DATE,
    end_time TIME,
    location VARCHAR(200),
    is_online BOOLEAN DEFAULT FALSE,
    color VARCHAR(7) DEFAULT '#4285F4',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, start_date)
);
```

### 5.2 google_calendar_token 테이블

```sql
CREATE TABLE google_calendar_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    email VARCHAR(255),
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);
```

---

## 6. 에러 코드

| HTTP 상태 | 에러 코드 | 설명 |
|-----------|----------|------|
| 400 | INVALID_DATE_RANGE | 잘못된 날짜 범위 |
| 400 | INVALID_TIME_FORMAT | 잘못된 시간 형식 |
| 401 | UNAUTHORIZED | 인증 실패 |
| 403 | FORBIDDEN | 권한 없음 (타인의 일정 수정 시도) |
| 404 | SCHEDULE_NOT_FOUND | 일정을 찾을 수 없음 |
| 404 | GOOGLE_NOT_CONNECTED | Google Calendar 미연동 |
| 500 | GOOGLE_API_ERROR | Google API 호출 실패 |
| 500 | INTERNAL_SERVER_ERROR | 서버 오류 |

**에러 응답 예시:**
```json
{
  "success": false,
  "data": null,
  "message": "일정을 찾을 수 없습니다.",
  "errorCode": "SCHEDULE_NOT_FOUND"
}
```

---

## 7. 구현 우선순위

### Phase 1 (필수)
1. 개인 일정 CRUD (1.1 ~ 1.5)
2. 스터디 세션 조회 (2.1)

### Phase 2 (권장)
3. 통합 조회 API (4.1)
4. Google Calendar 상태 확인 (3.1)

### Phase 3 (추가 기능)
5. Google OAuth 연동 (3.2 ~ 3.5)
6. 알림 시스템 연동

---

## 8. Google Calendar API 설정

### 8.1 Google Cloud Console 설정
1. Google Cloud Console에서 프로젝트 생성
2. Google Calendar API 활성화
3. OAuth 2.0 Client ID 생성
   - Application type: Web application
   - Authorized redirect URIs: `https://yourdomain.com/api/v1/calendar/google/callback`
4. Scopes 설정:
   - `https://www.googleapis.com/auth/calendar.readonly` (읽기 전용)
   - 또는 `https://www.googleapis.com/auth/calendar` (읽기/쓰기)

### 8.2 환경 변수
```properties
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret
GOOGLE_REDIRECT_URI=https://yourdomain.com/api/v1/calendar/google/callback
```

---

## 9. 참고 사항

### 9.1 날짜/시간 형식
- 날짜: `YYYY-MM-DD` (ISO 8601)
- 시간: `HH:mm` (24시간 형식)
- DateTime: `YYYY-MM-DDTHH:mm:ssZ` (ISO 8601, UTC)

### 9.2 색상 코드
- 개인 일정: 사용자 지정 (기본값: `#4285F4`)
- 스터디 세션: 고정 (`#34A853` - Google Green)
- Google 이벤트: 고정 (`#EA4335` - Google Red)

### 9.3 타임존
- 모든 DateTime은 UTC 기준
- 프론트엔드에서 로컬 타임존으로 변환

### 9.4 페이지네이션
- 개인 일정은 날짜 범위로 필터링 (최대 3개월)
- 대용량 데이터 시 Cursor Pagination 고려

---

## 10. 테스트 데이터

### 10.1 개인 일정 생성 예시
```bash
curl -X POST https://api.modustudy.com/api/v1/users/me/schedules \
  -H "Authorization: Bearer {token}" \
  -H "User-Id: 123" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "알고리즘 공부",
    "startDate": "2026-01-26",
    "startTime": "14:00",
    "endTime": "16:00",
    "isOnline": false,
    "color": "#4285F4"
  }'
```

### 10.2 일정 조회 예시
```bash
curl -X GET "https://api.modustudy.com/api/v1/users/me/schedules?startDate=2026-01-01&endDate=2026-01-31" \
  -H "Authorization: Bearer {token}" \
  -H "User-Id: 123"
```

---

## 작성자
- 프론트엔드 팀
- 작성일: 2026-01-26
- 버전: 1.0

## 변경 이력
- 2026-01-26: 초안 작성
