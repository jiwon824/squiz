# 스터디 API (Study)

## 기본 정보
- Base URL: `/api/v1/study`
- 인증: JWT 필요 (일부 조회 제외)

## 목차
- [1. 스터디 조회 API](#1-스터디-조회-api)
- [2. 스터디 CRUD API](#2-스터디-crud-api)
- [3. 에러 코드](#3-에러-코드)
- [4. Enum 값 정리](#4-enum-값-정리)

---

## 1. 스터디 조회 API

### 1.1 스터디 목록 조회 (탐색)

**Endpoint:** `GET /api/v1/study`

**설명:** 다양한 조건으로 스터디 목록을 필터링하고 페이징하여 조회합니다.

**인증:** 불필요

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | Integer | N | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | N | 20 | 페이지당 항목 수 |
| topic | String | N | - | 주제 필터 (예: 알고리즘, CS, 백엔드) |
| format | String | N | - | 형식 필터 (예: 문제풀이, 독서, 프로젝트) |
| studyType | Enum | N | - | PLANNED/LIGHTNING |
| meetingType | Enum | N | - | ONLINE/OFFLINE/HYBRID |
| status | Enum | N | - | RECRUITING/IN_PROGRESS 등 |
| keyword | String | N | - | 검색어 (스터디명/설명) |
| regionId | Long | N | - | 지역 ID (오프라인 필터) |
| scheduleDays | String | N | - | 요일 필터 (쉼표 구분: MON,WED,FRI) |
| difficulty | Enum | N | - | 난이도 (BEGINNER, INTERMEDIATE 등) |
| targetOrgType | String | N | - | 대상 소속 타입 (SSAFY/NBC/WTC 등) |
| sort | String | N | createdAt,desc | 정렬 기준 |

**Request Example:**
```
GET /api/v1/study?page=0&size=20&topic=알고리즘&meetingType=ONLINE&status=RECRUITING&sort=createdAt,desc
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "알고리즘 마스터",
      "description": "백준 골드 문제 집중 풀이 스터디입니다",
      "topic": "알고리즘",
      "format": "문제풀이",
      "studyType": "PLANNED",
      "meetingType": "ONLINE",
      "status": "RECRUITING",
      "maxMembers": 6,
      "difficulty": "INTERMEDIATE",
      "regionId": null,
      "locationDetail": null,
      "scheduleDays": "MON,WED,FRI",
      "scheduleTime": "19:00:00",
      "startDate": "2025-02-01",
      "endDate": "2025-05-01",
      "recruitStartDate": "2025-01-15",
      "recruitEndDate": "2025-01-31",
      "createdAt": "2025-01-18T17:00:00",
      "updatedAt": "2025-01-18T17:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 8,
  "totalPages": 1,
  "last": true
}
```

---

### 1.2 모집중인 스터디 목록 조회

**Endpoint:** `GET /api/v1/study/recruiting`

**설명:** 현재 모집 중인 스터디만 조회합니다.

**인증:** 불필요

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | Integer | N | 0 | 페이지 번호 |
| size | Integer | N | 20 | 페이지당 항목 수 |

**Request Example:**
```
GET /api/v1/study/recruiting?page=0&size=20
```

**Response:** (1.1과 동일한 형식, status가 RECRUITING인 항목만)

---

### 1.3 내 스터디 목록 조회

**Endpoint:** `GET /api/v1/study/my`

**설명:** 현재 사용자가 참여 중인 모든 스터디를 조회합니다.

**인증:** 필요

**Request Headers:**
```
user-id: 1
```

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | Integer | N | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | N | 20 | 페이지당 항목 수 |

**Request Example:**
```
GET /api/v1/study/my?page=0&size=20
```

**Response:** (1.1과 동일한 형식)

---

### 1.4 스터디 검색/필터링

**Endpoint:** `GET /api/v1/study/search`

**설명:** 다양한 조건으로 스터디를 검색합니다.

**인증:** 불필요

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| keyword | String | N | 스터디명/설명에서 검색할 키워드 |
| topic | String | N | 주제 (예: 알고리즘, CS, 백엔드) |
| meetingType | Enum | N | 미팅 타입 (ONLINE, OFFLINE, HYBRID) |
| difficulty | Enum | N | 난이도 (BEGINNER, ELEMENTARY, INTERMEDIATE, ADVANCED) |
| regionId | Long | N | 지역 ID |
| status | Enum | N | 상태 (RECRUITING, IN_PROGRESS 등) |
| page | Integer | N | 페이지 번호 (기본값: 0) |
| size | Integer | N | 페이지당 항목 수 (기본값: 20) |

**Request Examples:**
```
# 키워드로 검색
GET /api/v1/study/search?keyword=알고리즘&page=0&size=20

# 오프라인 스터디만 필터링
GET /api/v1/study/search?meetingType=OFFLINE&page=0&size=20

# 초급 난이도 스터디 검색
GET /api/v1/study/search?difficulty=BEGINNER&page=0&size=20

# 복합 조건 검색
GET /api/v1/study/search?keyword=자바&meetingType=OFFLINE&difficulty=BEGINNER&page=0&size=20
```

**Response:** (1.1과 동일한 형식)

---

### 1.5 스터디장별 스터디 목록 조회

**Endpoint:** `GET /api/v1/study/leader/{leaderId}`

**설명:** 특정 스터디장이 운영하는 모든 스터디를 조회합니다.

**인증:** 불필요

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| leaderId | Long | Y | 스터디장 ID |

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | Integer | N | 0 | 페이지 번호 |
| size | Integer | N | 20 | 페이지당 항목 수 |

**Request Example:**
```
GET /api/v1/study/leader/1?page=0&size=20
```

**Response:** (1.1과 동일한 형식)

---

### 1.6 스터디장의 특정 상태 스터디 목록 조회

**Endpoint:** `GET /api/v1/study/leader/{leaderId}/status/{status}`

**설명:** 특정 스터디장의 특정 상태 스터디를 조회합니다.

**인증:** 불필요

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| leaderId | Long | Y | 스터디장 ID |
| status | Enum | Y | 스터디 상태 |

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | Integer | N | 0 | 페이지 번호 |
| size | Integer | N | 20 | 페이지당 항목 수 |

**Request Example:**
```
GET /api/v1/study/leader/1/status/RECRUITING?page=0&size=20
```

**Response:** (1.1과 동일한 형식)

---

### 1.7 스터디 상세 조회

**Endpoint:** `GET /api/v1/study/{studyId}`

**설명:** 특정 스터디의 상세 정보를 조회합니다.

**인증:** 불필요 (선택적)

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studyId | Long | Y | 스터디 ID |

**Request Example:**
```
GET /api/v1/study/1
```

**Response:**
```json
{
  "id": 1,
  "leaderId": 1,
  "name": "알고리즘 마스터",
  "description": "백준 골드 문제 집중 풀이 스터디입니다",
  "topic": "알고리즘",
  "format": "문제풀이",
  "studyType": "PLANNED",
  "meetingType": "ONLINE",
  "regionId": null,
  "locationDetail": null,
  "scheduleSummary": null,
  "scheduleDays": "MON,WED,FRI",
  "scheduleTime": "19:00:00",
  "maxMembers": 6,
  "isPublic": true,
  "status": "RECRUITING",
  "penaltyPolicy": "NORMAL",
  "startDate": "2025-02-01",
  "endDate": "2025-05-01",
  "totalSessions": null,
  "recruitStartDate": "2025-01-15",
  "recruitEndDate": "2025-01-31",
  "extensionCount": 0,
  "textbook": "백준 온라인 저지",
  "goal": "골드 티어 달성",
  "difficulty": "INTERMEDIATE",
  "prerequisites": null,
  "processDetail": null,
  "targetOrgType": null,
  "targetOrgCriteria": null,
  "createdAt": "2025-01-18T17:00:00",
  "updatedAt": "2025-01-18T17:00:00"
}
```

---

### 1.8 특정 상태의 스터디 개수 조회

**Endpoint:** `GET /api/v1/study/count`

**설명:** 특정 상태의 스터디 개수를 조회합니다.

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| status | Enum | Y | 스터디 상태 (RECRUITING, IN_PROGRESS 등) |

**Request Example:**
```
GET /api/v1/study/count?status=RECRUITING
```

**Response:**
```json
5
```

---

### 1.9 스터디 존재 여부 확인

**Endpoint:** `GET /api/v1/study/{studyId}/exists`

**설명:** 특정 ID의 스터디가 존재하는지 확인합니다.

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studyId | Long | Y | 스터디 ID |

**Request Example:**
```
GET /api/v1/study/1/exists
```

**Response:**
```json
true
```

---

## 2. 스터디 CRUD API

### 2.1 스터디 생성

**Endpoint:** `POST /api/v1/study`

**설명:** 새로운 스터디를 생성합니다.

**인증:** 필요 (JWT)

**Request Headers:**
```
Content-Type: application/json
User-Id: 1  (임시 인증 방식, 추후 JWT로 변경 예정)
```

**Request Body:**
```json
{
  "name": "알고리즘 스터디",
  "description": "매주 백준 문제를 풀고 코드 리뷰를 진행합니다.",
  "topic": "알고리즘",
  "format": "문제풀이",
  "studyType": "PLANNED",
  "meetingType": "OFFLINE",
  "maxMembers": 6,
  "isPublic": true,
  "penaltyPolicy": "NORMAL",
  "regionId": 1,
  "locationDetail": "강남역 스터디카페",
  "scheduleDays": "MON,WED,FRI",
  "scheduleTime": "19:00:00",
  "startDate": "2025-01-15",
  "endDate": "2025-03-15",
  "totalSessions": 8,
  "recruitStartDate": "2025-01-01",
  "recruitEndDate": "2025-01-14",
  "textbook": "백준 온라인 저지",
  "goal": "골드 티어 달성",
  "difficulty": "INTERMEDIATE",
  "prerequisites": "Python 또는 Java 기초",
  "processDetail": "매주 월/수/금 19:00-21:00, 문제 풀이 후 코드 리뷰",
  "targetOrgType": "SSAFY",
  "targetOrgCriteria": {"generation": 14}
}
```

**Required Fields:**
- `name` - 스터디명 (최대 100자)
- `topic` - 주제 (최대 50자)
- `studyType` - 스터디 타입 (PLANNED, LIGHTNING)
- `meetingType` - 미팅 타입 (ONLINE, OFFLINE, HYBRID)
- `startDate` - 시작일
- `endDate` - 종료일

**Response:** (201 Created)
```json
{
  "id": 9,
  "leaderId": 1,
  "name": "알고리즘 스터디",
  "description": "매주 백준 문제를 풀고 코드 리뷰를 진행합니다.",
  "topic": "알고리즘",
  "format": "문제풀이",
  "studyType": "PLANNED",
  "meetingType": "OFFLINE",
  "status": "DRAFT",
  "maxMembers": 6,
  "difficulty": "INTERMEDIATE",
  "startDate": "2025-01-15",
  "endDate": "2025-03-15",
  "createdAt": "2025-01-19T10:30:00",
  "updatedAt": "2025-01-19T10:30:00"
}
```

---

### 2.2 스터디 수정

**Endpoint:** `PUT /api/v1/study/{studyId}`

**설명:** 기존 스터디 정보를 수정합니다. (스터디장만 가능)

**인증:** 필요

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studyId | Long | Y | 스터디 ID |

**Request Headers:**
```
Content-Type: application/json
User-Id: 1  (임시 인증 방식, 추후 JWT로 변경 예정)
```

**Request Body:** (수정할 필드만 포함)
```json
{
  "name": "스프링 부트 심화 프로젝트",
  "maxMembers": 10,
  "description": "Spring Boot 심화 내용 추가"
}
```

**Response:** (200 OK)
```json
{
  "id": 9,
  "leaderId": 1,
  "name": "스프링 부트 심화 프로젝트",
  "maxMembers": 10,
  "description": "Spring Boot 심화 내용 추가",
  "updatedAt": "2025-01-19T11:00:00"
}
```

**Error Response:** (403 Forbidden)
```json
{
  "error": "스터디를 수정할 권한이 없습니다"
}
```

---

### 2.3 스터디 삭제

**Endpoint:** `DELETE /api/v1/study/{studyId}`

**설명:** 스터디를 삭제합니다. (스터디장만 가능, 진행중/완료 스터디는 삭제 불가)

**인증:** 필요

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studyId | Long | Y | 스터디 ID |

**Request Headers:**
```
User-Id: 1  (임시 인증 방식, 추후 JWT로 변경 예정)
```

**Request Example:**
```
DELETE /api/v1/study/9
```

**Response:** (204 No Content)

**Error Responses:**

403 Forbidden:
```json
{
  "error": "스터디를 삭제할 권한이 없습니다"
}
```

400 Bad Request:
```json
{
  "error": "진행 중이거나 완료된 스터디는 삭제할 수 없습니다"
}
```

---

### 2.4 스터디 상태 변경

**Endpoint:** `PATCH /api/v1/study/{studyId}/status`

**설명:** 스터디 상태를 변경합니다. (스터디장만 가능)

**인증:** 필요

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studyId | Long | Y | 스터디 ID |

**Request Headers:**
```
Content-Type: application/json
User-Id: 1  (임시 인증 방식, 추후 JWT로 변경 예정)
```

**Request Body:**
```json
{
  "status": "RECRUITING"
}
```

**Request Example:**
```
PATCH /api/v1/study/9/status
```

**Response:** (200 OK)
```json
{
  "id": 9,
  "status": "RECRUITING",
  "updatedAt": "2025-01-19T12:00:00"
}
```

---

### 2.5 모집 기간 연장

**Endpoint:** `PATCH /api/v1/study/{studyId}/extend-recruitment`

**설명:** 스터디 모집 기간을 연장합니다. (최대 1회, 스터디장만 가능)

**인증:** 필요

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studyId | Long | Y | 스터디 ID |

**Request Headers:**
```
Content-Type: application/json
User-Id: 1  (임시 인증 방식, 추후 JWT로 변경 예정)
```

**Request Body:**
```json
{
  "newEndDate": "2025-02-15"
}
```

**Request Example:**
```
PATCH /api/v1/study/9/extend-recruitment
```

**Response:** (200 OK)
```json
{
  "id": 9,
  "recruitEndDate": "2025-02-15",
  "extensionCount": 1,
  "updatedAt": "2025-01-19T13:00:00"
}
```

**Error Response:** (400 Bad Request)
```json
{
  "error": "모집 기간은 최대 1회만 연장 가능합니다."
}
```

---

## 3. 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| STUDY_NOT_FOUND | 404 | 스터디를 찾을 수 없음 |
| NOT_STUDY_LEADER | 403 | 스터디장 권한 필요 |
| STUDY_FULL | 400 | 스터디 정원 초과 |
| RECRUIT_CLOSED | 400 | 모집 기간 종료 |
| ALREADY_MEMBER | 400 | 이미 스터디 멤버 |
| INVALID_INVITE_CODE | 400 | 유효하지 않은 초대 코드 |
| ORG_VERIFICATION_REQUIRED | 403 | 소속 인증 필요 (해당 소속 전용 스터디) |
| ORG_CRITERIA_NOT_MET | 403 | 소속 조건 미충족 (기수 등) |
| INVALID_DATE_RANGE | 400 | 종료일이 시작일보다 앞섬 |
| INVALID_LOCATION | 400 | 오프라인/혼합 스터디는 지역 정보 필수 |
| CANNOT_DELETE_ACTIVE_STUDY | 400 | 진행 중이거나 완료된 스터디는 삭제 불가 |
| INVALID_STATUS_TRANSITION | 400 | 유효하지 않은 상태 전환 |
| MAX_EXTENSION_REACHED | 400 | 모집 기간 연장 횟수 초과 (최대 1회) |

---

## 4. Enum 값 정리

### StudyType (스터디 타입)
- `PLANNED` - 계획형
- `LIGHTNING` - 번개형

### MeetingType (미팅 타입)
- `ONLINE` - 온라인
- `OFFLINE` - 오프라인
- `HYBRID` - 혼합

### Status (스터디 상태)
- `DRAFT` - 임시저장
- `SCHEDULED` - 예정
- `RECRUITING` - 모집중
- `RECRUIT_CLOSED` - 모집마감
- `PENDING` - 대기중
- `IN_PROGRESS` - 진행중
- `COMPLETED` - 완료
- `CANCELLED` - 취소

### Difficulty (난이도)
- `BEGINNER` - 입문
- `ELEMENTARY` - 초급
- `INTERMEDIATE` - 중급
- `ADVANCED` - 고급

### PenaltyPolicy (패널티 정책)
- `STRICT` - 엄격
- `NORMAL` - 보통
- `LENIENT` - 관대
- `RATIO` - 비율
- `NONE` - 없음