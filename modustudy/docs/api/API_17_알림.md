# 알림 API (Notification)

## 기본 정보
- Base URL: `/api/v1/notifications`
- 인증: Header에 `User-Id` 필요

---

## 엔드포인트 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/` | 알림 목록 조회 | O |
| GET | `/unread-count` | 읽지 않은 알림 수 | O |
| PUT | `/{notificationId}/read` | 알림 읽음 처리 | O |
| PUT | `/read-all` | 전체 읽음 처리 | O |
| GET | `/settings` | 알림 설정 조회 | O |
| PUT | `/settings` | 알림 설정 수정 | O |
| POST | `/fcm-token` | FCM 토큰 등록 (모바일) | O |
| DELETE | `/fcm-token` | FCM 토큰 삭제 | O |

---

## API 상세

### 1. 알림 목록 조회

**Request**
```
GET /api/v1/notifications?page=0&size=20&type=SCHEDULE
User-Id: {userId}
```

| Parameter | Type | 필수 | 설명 |
|-----------|------|------|------|
| page | int | X | 페이지 번호 (기본값: 0) |
| size | int | X | 페이지 크기 (기본값: 20) |
| type | string | X | 알림 타입 필터 |

**Response**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "type": "SCHEDULE",
        "title": "스터디 일정 알림",
        "content": "알고리즘 스터디가 1시간 후에 시작됩니다.",
        "referenceType": "study_session",
        "referenceId": 5,
        "isRead": false,
        "createdAt": "2025-01-17T18:00:00"
      },
      {
        "id": 2,
        "type": "ATTENDANCE",
        "title": "출석 체크 시작",
        "content": "알고리즘 스터디 출석 체크가 시작되었습니다.",
        "referenceType": "study_session",
        "referenceId": 5,
        "isRead": true,
        "createdAt": "2025-01-17T19:00:00"
      },
      {
        "id": 3,
        "type": "CHAT",
        "title": "새 메시지",
        "content": "홍길동: 안녕하세요!",
        "referenceType": "channel",
        "referenceId": 1,
        "isRead": false,
        "createdAt": "2025-01-17T19:05:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3,
    "unreadCount": 5
  }
}
```

---

### 2. 읽지 않은 알림 수

**Request**
```
GET /api/v1/notifications/unread-count
User-Id: {userId}
```

**Response**
```json
{
  "success": true,
  "data": {
    "unreadCount": 5,
    "byType": {
      "CHAT": 2,
      "SCHEDULE": 1,
      "ATTENDANCE": 1,
      "STUDY_UPDATE": 1,
      "QUIZ": 0,
      "SYSTEM": 0
    }
  }
}
```

---

### 3. 알림 읽음 처리

**Request**
```
PUT /api/v1/notifications/{notificationId}/read
User-Id: {userId}
```

**Response**
```json
{
  "success": true,
  "data": {
    "message": "알림을 읽음 처리했습니다."
  }
}
```

**Error Response**
```json
{
  "success": false,
  "error": {
    "code": "NOTIFICATION_NOT_FOUND",
    "message": "알림을 찾을 수 없습니다. ID: 999"
  }
}
```

---

### 4. 전체 읽음 처리

**Request**
```
PUT /api/v1/notifications/read-all
User-Id: {userId}
```

**Response**
```json
{
  "success": true,
  "data": {
    "readCount": 5
  }
}
```

---

### 5. 알림 설정 조회

**Request**
```
GET /api/v1/notifications/settings
User-Id: {userId}
```

**Response**
```json
{
  "success": true,
  "data": {
    "settings": [
      {
        "type": "CHAT",
        "typeName": "채팅 알림",
        "isEnabled": true
      },
      {
        "type": "SCHEDULE",
        "typeName": "일정 알림",
        "isEnabled": true
      },
      {
        "type": "ATTENDANCE",
        "typeName": "출석 알림",
        "isEnabled": true
      },
      {
        "type": "STUDY_UPDATE",
        "typeName": "스터디 업데이트",
        "isEnabled": true
      },
      {
        "type": "QUIZ",
        "typeName": "퀴즈 알림",
        "isEnabled": false
      },
      {
        "type": "SYSTEM",
        "typeName": "시스템 알림",
        "isEnabled": true
      }
    ]
  }
}
```

---

### 6. 알림 설정 수정

**Request**
```
PUT /api/v1/notifications/settings
User-Id: {userId}
Content-Type: application/json
```
```json
{
  "settings": [
    {"type": "CHAT", "isEnabled": true},
    {"type": "SCHEDULE", "isEnabled": true},
    {"type": "ATTENDANCE", "isEnabled": true},
    {"type": "STUDY_UPDATE", "isEnabled": false},
    {"type": "QUIZ", "isEnabled": false},
    {"type": "SYSTEM", "isEnabled": true}
  ]
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "message": "알림 설정이 저장되었습니다."
  }
}
```

**Error Response**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_NOTIFICATION_TYPE",
    "message": "유효하지 않은 알림 타입입니다: INVALID_TYPE"
  }
}
```

---

### 7. FCM 토큰 등록 (모바일)

**Request**
```
POST /api/v1/notifications/fcm-token
User-Id: {userId}
Content-Type: application/json
```
```json
{
  "token": "fcm_token_here",
  "deviceType": "ANDROID"
}
```

| Field | Type | 필수 | 설명 |
|-------|------|------|------|
| token | string | O | FCM 토큰 |
| deviceType | string | O | ANDROID, IOS, WEB |

**Response**
```json
{
  "success": true,
  "data": {
    "message": "FCM 토큰이 등록되었습니다."
  }
}
```

---

### 8. FCM 토큰 삭제

**Request**
```
DELETE /api/v1/notifications/fcm-token
User-Id: {userId}
Content-Type: application/json
```
```json
{
  "token": "fcm_token_here"
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "message": "FCM 토큰이 삭제되었습니다."
  }
}
```

**Error Response**
```json
{
  "success": false,
  "error": {
    "code": "FCM_TOKEN_NOT_FOUND",
    "message": "FCM 토큰을 찾을 수 없습니다."
  }
}
```

---

## 알림 타입

| Type | 설명 | displayName | 발송 시점 |
|------|------|-------------|----------|
| CHAT | 채팅 알림 | 채팅 알림 | 새 메시지 수신 시 |
| SCHEDULE | 일정 알림 | 일정 알림 | 일정 1시간 전, 10분 전 |
| ATTENDANCE | 출석 알림 | 출석 알림 | 출석 체크 시작 시 |
| STUDY_UPDATE | 스터디 업데이트 | 스터디 업데이트 | 공지/일정 변경 시 |
| QUIZ | 퀴즈 알림 | 퀴즈 알림 | 퀴즈 대회 시작 전 |
| SYSTEM | 시스템 알림 | 시스템 알림 | 추방 예고, 업데이트 등 |

---

## 디바이스 타입

| Type | 설명 |
|------|------|
| ANDROID | 안드로이드 |
| IOS | iOS |
| WEB | 웹 브라우저 |

---

## 에러 코드

| HTTP Status | 코드 | 설명 |
|-------------|------|------|
| 404 | NOTIFICATION_NOT_FOUND | 알림을 찾을 수 없음 |
| 404 | NOTIFICATION_SETTING_NOT_FOUND | 알림 설정을 찾을 수 없음 |
| 404 | FCM_TOKEN_NOT_FOUND | FCM 토큰을 찾을 수 없음 |
| 400 | INVALID_FCM_TOKEN | 유효하지 않은 FCM 토큰 |
| 400 | INVALID_NOTIFICATION_TYPE | 유효하지 않은 알림 타입 |
| 400 | INVALID_NOTIFICATION_REQUEST | 유효하지 않은 알림 요청 |
| 403 | NOT_NOTIFICATION_OWNER | 알림 소유자가 아님 |
| 409 | DUPLICATE_FCM_TOKEN | 중복된 FCM 토큰 |

---

## 공통 응답 형식

### 성공 응답
```json
{
  "success": true,
  "data": { ... }
}
```

### 실패 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```