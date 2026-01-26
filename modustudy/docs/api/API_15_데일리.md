# 데일리 API (Daily Report)

## 기본 정보
- Base URL: `/api/v1/studies/{studyId}/daily-reports`
- 인증: JWT 필요 (삭제 시)

---

## 엔드포인트 목록

| Method | Endpoint | 설명 | 인증 | 구현 |
|--------|----------|------|------|------|
| GET | `/` | 데일리 리포트 목록 조회 | - | ✅ |
| GET | `/{reportId}` | 데일리 리포트 단건 조회 | - | ✅ |
| GET | `/date/{reportDate}` | 특정 날짜 리포트 조회 | - | ✅ |
| DELETE | `/{reportId}` | 데일리 리포트 삭제 | O (스터디장) | ✅ |
| DELETE | `/` | 데일리 리포트 전체 삭제 | O (스터디장) | ✅ |
| POST | `/today/items` | 데일리 항목 작성 | O | 🔜 |
| PUT | `/today/items/{itemId}` | 데일리 항목 수정 | O | 🔜 |
| DELETE | `/today/items/{itemId}` | 데일리 항목 삭제 | O | 🔜 |

---

## API 상세

### 1. 데일리 리포트 목록 조회

스터디별 데일리 리포트 목록을 최신순으로 조회합니다.  
기간 파라미터를 추가하면 해당 기간 내 리포트만 조회합니다.

**Request**
```
GET /api/v1/studies/{studyId}/daily-reports
GET /api/v1/studies/{studyId}/daily-reports?startDate=2025-01-20&endDate=2025-01-25
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| startDate | LocalDate | X | 조회 시작일 (YYYY-MM-DD) |
| endDate | LocalDate | X | 조회 종료일 (YYYY-MM-DD) |

**Response (200 OK)**
```json
[
  {
    "id": 6,
    "studyId": 1,
    "reportDate": "2025-01-25",
    "summary": "## 1월 25일 데일리 리포트\n\n### 어제 한 일\n- QA 테스트 완료...",
    "createdAt": "2025-01-25T09:00:00"
  },
  {
    "id": 5,
    "studyId": 1,
    "reportDate": "2025-01-24",
    "summary": "## 1월 24일 데일리 리포트\n\n### 어제 한 일\n- 성능 테스트 진행...",
    "createdAt": "2025-01-24T09:00:00"
  }
]
```

---

### 2. 데일리 리포트 단건 조회

리포트 ID로 단건 조회합니다.

**Request**
```
GET /api/v1/studies/{studyId}/daily-reports/{reportId}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "studyId": 1,
  "reportDate": "2025-01-20",
  "summary": "## 1월 20일 데일리 리포트\n\n### 어제 한 일\n- Spring Security 설정 완료\n- JWT 인증 구현\n\n### 오늘 할 일\n- API 테스트 코드 작성\n- 코드 리뷰 반영\n\n### 블로커\n- 없음",
  "createdAt": "2025-01-20T09:00:00"
}
```

**Response (404 Not Found)**
```json
{
  "status": 404,
  "code": "DAILY_REPORT_NOT_FOUND",
  "message": "데일리 리포트를 찾을 수 없습니다."
}
```

---

### 3. 특정 날짜 리포트 조회

스터디의 특정 날짜 리포트를 조회합니다.

**Request**
```
GET /api/v1/studies/{studyId}/daily-reports/date/{reportDate}
```

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| studyId | Long | O | 스터디 ID |
| reportDate | LocalDate | O | 조회할 날짜 (YYYY-MM-DD) |

**Response (200 OK)**
```json
{
  "id": 4,
  "studyId": 1,
  "reportDate": "2025-01-23",
  "summary": "## 1월 23일 데일리 리포트\n\n### 어제 한 일\n- CI/CD 파이프라인 구축 완료...",
  "createdAt": "2025-01-23T09:00:00"
}
```

**Response (404 Not Found)**
```json
{
  "status": 404,
  "code": "DAILY_REPORT_NOT_FOUND",
  "message": "해당 날짜의 데일리 리포트를 찾을 수 없습니다."
}
```

---

### 4. 데일리 리포트 삭제 (스터디장 전용)

스터디장만 데일리 리포트를 삭제할 수 있습니다.

**Request**
```
DELETE /api/v1/studies/{studyId}/daily-reports/{reportId}
User-Id: {userId}
```

**Headers**
| 헤더 | 필수 | 설명 |
|------|------|------|
| User-Id | O | 요청자 ID (스터디장 검증용) |

**Response (204 No Content)**
```
(응답 본문 없음)
```

**Response (403 Forbidden)**
```json
{
  "status": 403,
  "code": "NOT_STUDY_LEADER",
  "message": "데일리 리포트를 삭제할 권한이 없습니다."
}
```

**Response (404 Not Found)**
```json
{
  "status": 404,
  "code": "DAILY_REPORT_NOT_FOUND",
  "message": "데일리 리포트를 찾을 수 없습니다."
}
```

---

### 5. 데일리 리포트 전체 삭제 (스터디장 전용)

스터디의 모든 데일리 리포트를 삭제합니다.

**Request**
```
DELETE /api/v1/studies/{studyId}/daily-reports
User-Id: {userId}
```

**Headers**
| 헤더 | 필수 | 설명 |
|------|------|------|
| User-Id | O | 요청자 ID (스터디장 검증용) |

**Response (204 No Content)**
```
(응답 본문 없음)
```

**Response (403 Forbidden)**
```json
{
  "status": 403,
  "code": "NOT_STUDY_LEADER",
  "message": "데일리 리포트를 삭제할 권한이 없습니다."
}
```

---

## 미구현 API (DailyItem 관련)

아래 API는 DailyItem 구현 시 추가 예정입니다.

### 6. 데일리 항목 작성

**Request**
```
POST /api/v1/studies/{studyId}/daily-reports/today/items
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "category": "YESTERDAY",
  "content": "백준 1002번 문제 풀이 완료"
}
```

| category | 설명 |
|----------|------|
| YESTERDAY | 어제 한 일 |
| TODAY | 오늘 할 일 |
| BLOCKER | 막히는 점, 도움 필요 |

---

### 7. 데일리 항목 수정

**Request**
```
PUT /api/v1/studies/{studyId}/daily-reports/today/items/{itemId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "content": "백준 1002번, 1003번 문제 풀이 완료"
}
```

---

### 8. 데일리 항목 삭제

**Request**
```
DELETE /api/v1/studies/{studyId}/daily-reports/today/items/{itemId}
Authorization: Bearer {accessToken}
```

---

## 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| DAILY_REPORT_NOT_FOUND | 404 | 데일리 리포트를 찾을 수 없음 |
| STUDY_NOT_FOUND | 404 | 스터디를 찾을 수 없음 |
| NOT_STUDY_LEADER | 403 | 스터디장 권한 없음 |
| ITEM_NOT_FOUND | 404 | 데일리 항목을 찾을 수 없음 (미구현) |
| NOT_ITEM_OWNER | 403 | 본인 항목만 수정/삭제 가능 (미구현) |
| CANNOT_EDIT_PAST_DAILY | 400 | 과거 데일리는 수정 불가 (미구현) |