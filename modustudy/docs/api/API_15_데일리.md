# 데일리 API (Daily Report)

## 기본 정보
- Base URL: `/api/v1/studies/{studyId}/daily-reports`
- DailyItem Base URL: `/api/v1/dailies/{dailyId}/items`
- 인증: JWT 필요 (일부 API)

---

## 목차
- [1. 데일리 리포트 API](#1-데일리-리포트-api)
- [2. 데일리 항목 API](#2-데일리-항목-api)
- [3. 에러 코드](#3-에러-코드)
- [4. Enum 값 정리](#4-enum-값-정리)

---

## 1. 데일리 리포트 API

### 엔드포인트 목록

| Method | Endpoint | 설명 | 인증 | 구현 |
|--------|----------|------|------|------|
| GET | `/` | 데일리 리포트 목록 조회 | - | ✅ |
| GET | `/{reportId}` | 데일리 리포트 단건 조회 | - | ✅ |
| GET | `/date/{reportDate}` | 특정 날짜 리포트 조회 | - | ✅ |
| DELETE | `/{reportId}` | 데일리 리포트 삭제 | O (스터디장) | ✅ |
| DELETE | `/` | 데일리 리포트 전체 삭제 | O (스터디장) | ✅ |

---

### 1.1 데일리 리포트 목록 조회

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

### 1.2 데일리 리포트 단건 조회

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

### 1.3 특정 날짜 리포트 조회

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

### 1.4 데일리 리포트 삭제 (스터디장 전용)

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

---

### 1.5 데일리 리포트 전체 삭제 (스터디장 전용)

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

## 2. 데일리 항목 API

AI가 생성한 데일리 리포트의 개별 항목(어제 한 일/오늘 할 일/블로커)을 조회하고 삭제하는 API입니다.

### Base URL: `/api/v1/dailies/{dailyId}/items`

### 엔드포인트 목록

| Method | Endpoint | 설명 | 인증 | 구현 |
|--------|----------|------|------|------|
| GET | `/` | 전체 항목 조회 | - | ✅ |
| GET | `/my` | 내 항목 조회 (카테고리별 그룹화) | O | ✅ |
| GET | `/my/category?category={category}` | 내 특정 카테고리 항목 조회 | O | ✅ |
| GET | `/my/count` | 내 항목 개수 조회 | O | ✅ |
| GET | `/{itemId}` | 항목 단건 조회 | - | ✅ |
| GET | `/blockers` | 블로커 항목만 조회 | - | ✅ |
| DELETE | `/{itemId}` | 항목 삭제 (본인만) | O | ✅ |

---

### 2.1 전체 항목 조회

데일리 리포트의 모든 항목을 조회합니다.

**Request**
```
GET /api/v1/dailies/{dailyId}/items
```

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "dailyReportId": 1,
    "userId": 10,
    "category": "YESTERDAY",
    "content": "API 설계 문서 작성 완료",
    "createdAt": "2025-01-26T09:00:00"
  },
  {
    "id": 2,
    "dailyReportId": 1,
    "userId": 10,
    "category": "TODAY",
    "content": "DailyItem API 구현 예정",
    "createdAt": "2025-01-26T09:01:00"
  },
  {
    "id": 3,
    "dailyReportId": 1,
    "userId": 10,
    "category": "BLOCKER",
    "content": "DB 연결 타임아웃 이슈 발생",
    "createdAt": "2025-01-26T09:02:00"
  },
  {
    "id": 4,
    "dailyReportId": 1,
    "userId": 20,
    "category": "YESTERDAY",
    "content": "프론트엔드 컴포넌트 개발",
    "createdAt": "2025-01-26T09:03:00"
  }
]
```

---

### 2.2 내 항목 조회 (카테고리별 그룹화)

현재 사용자의 데일리 항목을 카테고리별로 그룹화하여 조회합니다.

**Request**
```
GET /api/v1/dailies/{dailyId}/items/my
user-id: {userId}
```

**Headers**
| 헤더 | 필수 | 설명 |
|------|------|------|
| user-id | O | 요청자 ID |

**Response (200 OK)**
```json
{
  "dailyReportId": 1,
  "userId": 10,
  "yesterday": [
    {
      "id": 1,
      "dailyReportId": 1,
      "userId": 10,
      "category": "YESTERDAY",
      "content": "API 설계 문서 작성 완료",
      "createdAt": "2025-01-26T09:00:00"
    },
    {
      "id": 2,
      "dailyReportId": 1,
      "userId": 10,
      "category": "YESTERDAY",
      "content": "코드 리뷰 3건 진행",
      "createdAt": "2025-01-26T09:00:30"
    }
  ],
  "today": [
    {
      "id": 3,
      "dailyReportId": 1,
      "userId": 10,
      "category": "TODAY",
      "content": "DailyItem API 구현 예정",
      "createdAt": "2025-01-26T09:01:00"
    }
  ],
  "blocker": [
    {
      "id": 4,
      "dailyReportId": 1,
      "userId": 10,
      "category": "BLOCKER",
      "content": "DB 연결 타임아웃 이슈 발생",
      "createdAt": "2025-01-26T09:02:00"
    }
  ],
  "totalCount": 4
}
```

---

### 2.3 내 특정 카테고리 항목 조회

현재 사용자의 특정 카테고리 항목만 조회합니다.

**Request**
```
GET /api/v1/dailies/{dailyId}/items/my/category?category=YESTERDAY
user-id: {userId}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| category | Enum | O | YESTERDAY / TODAY / BLOCKER |

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "dailyReportId": 1,
    "userId": 10,
    "category": "YESTERDAY",
    "content": "API 설계 문서 작성 완료",
    "createdAt": "2025-01-26T09:00:00"
  },
  {
    "id": 2,
    "dailyReportId": 1,
    "userId": 10,
    "category": "YESTERDAY",
    "content": "코드 리뷰 3건 진행",
    "createdAt": "2025-01-26T09:00:30"
  }
]
```

---

### 2.4 내 항목 개수 조회

현재 사용자의 데일리 항목 개수를 조회합니다.

**Request**
```
GET /api/v1/dailies/{dailyId}/items/my/count
user-id: {userId}
```

**Response (200 OK)**
```json
5
```

---

### 2.5 항목 단건 조회

데일리 항목을 ID로 단건 조회합니다.

**Request**
```
GET /api/v1/dailies/{dailyId}/items/{itemId}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "dailyReportId": 1,
  "userId": 10,
  "category": "YESTERDAY",
  "content": "API 설계 문서 작성 완료",
  "createdAt": "2025-01-26T09:00:00"
}
```

**Response (400 Bad Request)**
```json
{
  "status": 400,
  "code": "INVALID_ARGUMENT",
  "message": "존재하지 않는 데일리 항목입니다: 999"
}
```

---

### 2.6 블로커 항목만 조회

데일리 리포트의 블로커 항목만 조회합니다. 팀의 장애물을 한눈에 확인할 때 유용합니다.

**Request**
```
GET /api/v1/dailies/{dailyId}/items/blockers
```

**Response (200 OK)**
```json
[
  {
    "id": 4,
    "dailyReportId": 1,
    "userId": 10,
    "category": "BLOCKER",
    "content": "DB 연결 타임아웃 이슈 발생",
    "createdAt": "2025-01-26T09:02:00"
  },
  {
    "id": 8,
    "dailyReportId": 1,
    "userId": 20,
    "category": "BLOCKER",
    "content": "CORS 에러 해결 필요",
    "createdAt": "2025-01-26T09:05:00"
  }
]
```

---

### 2.7 항목 삭제 (본인만)

본인이 작성한 데일리 항목을 삭제합니다.

**Request**
```
DELETE /api/v1/dailies/{dailyId}/items/{itemId}
user-id: {userId}
```

**Headers**
| 헤더 | 필수 | 설명 |
|------|------|------|
| user-id | O | 요청자 ID |

**Response (204 No Content)**
```
(응답 본문 없음)
```

**Response (400 Bad Request) - 존재하지 않는 항목**
```json
{
  "status": 400,
  "code": "INVALID_ARGUMENT",
  "message": "존재하지 않는 데일리 항목입니다: 999"
}
```

**Response (400 Bad Request) - 타인의 항목**
```json
{
  "status": 400,
  "code": "BAD_REQUEST",
  "message": "본인의 데일리 항목만 삭제할 수 있습니다"
}
```

---

## 3. 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| DAILY_REPORT_NOT_FOUND | 404 | 데일리 리포트를 찾을 수 없음 |
| STUDY_NOT_FOUND | 404 | 스터디를 찾을 수 없음 |
| NOT_STUDY_LEADER | 403 | 스터디장 권한 없음 |
| INVALID_ARGUMENT | 400 | 존재하지 않는 데일리 항목 |
| BAD_REQUEST | 400 | 본인 항목만 삭제 가능 |

---

## 4. Enum 값 정리

### DailyCategory (데일리 항목 카테고리)
| 값 | 설명 |
|-----|------|
| `YESTERDAY` | 어제 한 일 |
| `TODAY` | 오늘 할 일 |
| `BLOCKER` | 블로커 (막히는 점, 도움 필요) |