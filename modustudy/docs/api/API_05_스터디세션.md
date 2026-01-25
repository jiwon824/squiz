# 스터디 세션 API (Study Session)

## 기본 정보
- Base URL: `/api/v1/studies/{studyId}/sessions`
- 인증: Header `User-Id` 필요 (스터디장 권한 필요한 API)
 
---

## 엔드포인트 목록

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| GET | `/` | 세션 목록 조회 | - |
| GET | `/{sessionId}` | 세션 단건 조회 | - |
| GET | `/number/{sessionNumber}` | 회차로 세션 조회 | - |
| GET | `/next` | 다음 예정 세션 조회 | - |
| GET | `/statistics` | 세션 통계 조회 | - |
| POST | `/` | 세션 생성 | 스터디장 |
| PUT | `/{sessionId}` | 세션 수정 | 스터디장 |
| DELETE | `/{sessionId}` | 세션 삭제 | 스터디장 |
| POST | `/{sessionId}/start` | 세션 시작 | 스터디장 |
| POST | `/{sessionId}/complete` | 세션 완료 | 스터디장 |
| POST | `/{sessionId}/cancel` | 세션 취소 | 스터디장 |

---

## API 상세

### 1. 세션 목록 조회

**Request**
```
GET /api/v1/studies/{studyId}/sessions
GET /api/v1/studies/{studyId}/sessions?status=SCHEDULED
```

**Query Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | String | N | 상태 필터 (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED) |

**Response**
```json
[
  {
    "id": 1,
    "studyId": 1,
    "sessionNumber": 1,
    "title": "1회차: OT",
    "description": "오리엔테이션 및 스터디 소개",
    "scheduledAt": "2026-01-27T19:00:00",
    "durationMinutes": 60,
    "location": "Zoom 링크",
    "isOnline": true,
    "status": "COMPLETED",
    "completedAt": "2026-01-27T20:05:00",
    "createdAt": "2026-01-20T10:00:00"
  },
  {
    "id": 2,
    "studyId": 1,
    "sessionNumber": 2,
    "title": "2회차: 변수와 타입",
    "description": "자바 변수와 데이터 타입 학습",
    "scheduledAt": "2026-02-03T19:00:00",
    "durationMinutes": 90,
    "location": "Zoom 링크",
    "isOnline": true,
    "status": "SCHEDULED",
    "completedAt": null,
    "createdAt": "2026-01-20T10:00:00"
  }
]
```

---

### 2. 세션 단건 조회

**Request**
```
GET /api/v1/studies/{studyId}/sessions/{sessionId}
```

**Response**
```json
{
  "id": 1,
  "studyId": 1,
  "sessionNumber": 1,
  "title": "1회차: OT",
  "description": "오리엔테이션 및 스터디 소개",
  "scheduledAt": "2026-01-27T19:00:00",
  "durationMinutes": 60,
  "location": "Zoom 링크",
  "isOnline": true,
  "status": "COMPLETED",
  "completedAt": "2026-01-27T20:05:00",
  "createdAt": "2026-01-20T10:00:00"
}
```

---

### 3. 회차로 세션 조회

**Request**
```
GET /api/v1/studies/{studyId}/sessions/number/{sessionNumber}
```

**Response**
```json
{
  "id": 3,
  "studyId": 1,
  "sessionNumber": 3,
  "title": "3회차: 연산자",
  "description": "자바 연산자 학습",
  "scheduledAt": "2026-02-10T19:00:00",
  "durationMinutes": 90,
  "location": "Zoom 링크",
  "isOnline": true,
  "status": "SCHEDULED",
  "completedAt": null,
  "createdAt": "2026-01-20T10:00:00"
}
```

---

### 4. 다음 예정 세션 조회

현재 시간 이후 가장 가까운 SCHEDULED 상태의 세션 반환

**Request**
```
GET /api/v1/studies/{studyId}/sessions/next
```

**Response**
```json
{
  "id": 3,
  "studyId": 1,
  "sessionNumber": 3,
  "title": "3회차: 연산자",
  "description": "자바 연산자 학습",
  "scheduledAt": "2026-02-10T19:00:00",
  "durationMinutes": 90,
  "location": "Zoom 링크",
  "isOnline": true,
  "status": "SCHEDULED",
  "completedAt": null,
  "createdAt": "2026-01-20T10:00:00"
}
```

---

### 5. 세션 통계 조회

**Request**
```
GET /api/v1/studies/{studyId}/sessions/statistics
```

**Response**
```json
{
  "totalCount": 12,
  "completedCount": 4,
  "scheduledCount": 7,
  "cancelledCount": 1,
  "completionRate": 33.33
}
```

---

### 6. 세션 생성 (스터디장)

회차 번호는 자동으로 증가됨

**Request**
```
POST /api/v1/studies/{studyId}/sessions
User-Id: 1
Content-Type: application/json
```
```json
{
  "title": "5회차: 반복문",
  "description": "for, while 반복문 학습",
  "scheduledAt": "2026-02-24T19:00:00",
  "durationMinutes": 90,
  "location": "Zoom 링크",
  "isOnline": true
}
```

**Request Body**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | String | N | 세션 제목 (최대 200자) |
| description | String | N | 세션 설명 |
| scheduledAt | DateTime | **Y** | 예정 일시 |
| durationMinutes | Integer | N | 진행 시간(분), 기본값: 60 |
| location | String | N | 장소 (최대 200자) |
| isOnline | Boolean | N | 온라인 여부, 기본값: true |

**Response** `201 Created`
```json
{
  "id": 5,
  "studyId": 1,
  "sessionNumber": 5,
  "title": "5회차: 반복문",
  "description": "for, while 반복문 학습",
  "scheduledAt": "2026-02-24T19:00:00",
  "durationMinutes": 90,
  "location": "Zoom 링크",
  "isOnline": true,
  "status": "SCHEDULED",
  "completedAt": null,
  "createdAt": "2026-01-26T15:30:00"
}
```

---

### 7. 세션 수정 (스터디장)

SCHEDULED 상태의 세션만 수정 가능

**Request**
```
PUT /api/v1/studies/{studyId}/sessions/{sessionId}
User-Id: 1
Content-Type: application/json
```
```json
{
  "title": "3회차: 연산자 (수정됨)",
  "description": "자바 연산자 심화 학습",
  "durationMinutes": 120
}
```

**Request Body**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | String | N | 세션 제목 |
| description | String | N | 세션 설명 |
| scheduledAt | DateTime | N | 예정 일시 |
| durationMinutes | Integer | N | 진행 시간(분) |
| location | String | N | 장소 |
| isOnline | Boolean | N | 온라인 여부 |

**Response** `200 OK`
```json
{
  "id": 3,
  "studyId": 1,
  "sessionNumber": 3,
  "title": "3회차: 연산자 (수정됨)",
  "description": "자바 연산자 심화 학습",
  "scheduledAt": "2026-02-10T19:00:00",
  "durationMinutes": 120,
  "location": "Zoom 링크",
  "isOnline": true,
  "status": "SCHEDULED",
  "completedAt": null,
  "createdAt": "2026-01-20T10:00:00"
}
```

---

### 8. 세션 삭제 (스터디장)

COMPLETED 상태의 세션은 삭제 불가

**Request**
```
DELETE /api/v1/studies/{studyId}/sessions/{sessionId}
User-Id: 1
```

**Response** `204 No Content`

---

### 9. 세션 시작 (스터디장)

SCHEDULED → IN_PROGRESS 상태 변경

**Request**
```
POST /api/v1/studies/{studyId}/sessions/{sessionId}/start
User-Id: 1
```

**Response** `200 OK`
```json
{
  "id": 3,
  "studyId": 1,
  "sessionNumber": 3,
  "title": "3회차: 연산자",
  "status": "IN_PROGRESS",
  ...
}
```

---

### 10. 세션 완료 (스터디장)

IN_PROGRESS → COMPLETED 상태 변경

**Request**
```
POST /api/v1/studies/{studyId}/sessions/{sessionId}/complete
User-Id: 1
```

**Response** `200 OK`
```json
{
  "id": 3,
  "studyId": 1,
  "sessionNumber": 3,
  "title": "3회차: 연산자",
  "status": "COMPLETED",
  "completedAt": "2026-02-10T20:30:00",
  ...
}
```

---

### 11. 세션 취소 (스터디장)

COMPLETED 상태의 세션은 취소 불가

**Request**
```
POST /api/v1/studies/{studyId}/sessions/{sessionId}/cancel
User-Id: 1
```

**Response** `200 OK`
```json
{
  "id": 4,
  "studyId": 1,
  "sessionNumber": 4,
  "title": "4회차: 조건문",
  "status": "CANCELLED",
  ...
}
```

---

## 세션 상태 (SessionStatus)

| Status | 설명 | 전환 가능 |
|--------|------|----------|
| SCHEDULED | 예정됨 | → IN_PROGRESS, CANCELLED |
| IN_PROGRESS | 진행 중 | → COMPLETED, CANCELLED |
| COMPLETED | 완료됨 | (변경 불가) |
| CANCELLED | 취소됨 | (변경 불가) |

---

## 에러 응답

**Response Format**
```json
{
  "status": 404,
  "code": "SESSION_NOT_FOUND",
  "message": "세션을 찾을 수 없습니다"
}
```

**에러 코드**

| HTTP Status | Code | Description |
|-------------|------|-------------|
| 400 | BAD_REQUEST | 잘못된 요청 (예: 완료된 세션 삭제 시도) |
| 400 | VALIDATION_ERROR | 필수 필드 누락 (scheduledAt) |
| 403 | NOT_STUDY_LEADER | 스터디장만 수행 가능한 작업 |
| 404 | STUDY_NOT_FOUND | 존재하지 않는 스터디 |
| 404 | SESSION_NOT_FOUND | 존재하지 않는 세션 |

---

## 상태 변경 규칙

```
SCHEDULED ──(start)──▶ IN_PROGRESS ──(complete)──▶ COMPLETED
    │                       │
    └───(cancel)───▶ CANCELLED ◀───(cancel)───┘
```

- **시작 (start)**: SCHEDULED 상태에서만 가능
- **완료 (complete)**: IN_PROGRESS 상태에서만 가능
- **취소 (cancel)**: COMPLETED 상태에서는 불가
- **수정 (update)**: SCHEDULED 상태에서만 가능
- **삭제 (delete)**: COMPLETED 상태에서는 불가