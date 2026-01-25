# 워크스페이스 API (Workspace & Message)

## 기본 정보
- Workspace Base URL: `/api/v1/workspaces`
- Message Base URL: `/api/v1/workspaces/{workspaceId}/messages`
- 인증: User-Id 헤더 필요

---

## 워크스페이스 엔드포인트

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/study/{studyId}` | 워크스페이스 생성 (스터디 시작 시) | O |
| GET | `/{workspaceId}` | 워크스페이스 조회 (ID) | X |
| GET | `/study/{studyId}` | 워크스페이스 조회 (스터디 ID) | X |
| GET | `/study/{studyId}/exists` | 워크스페이스 존재 여부 확인 | X |
| DELETE | `/{workspaceId}` | 워크스페이스 삭제 | O |
| DELETE | `/study/{studyId}` | 워크스페이스 삭제 (스터디 ID) | O |

---

## 메시지 엔드포인트

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/` | 메시지 전송 | O |
| GET | `/` | 메시지 목록 조회 (페이징) | X |
| GET | `/recent` | 최근 메시지 조회 | X |
| GET | `/after` | 특정 시간 이후 메시지 조회 (폴링) | X |
| GET | `/search` | 메시지 검색 | X |
| GET | `/type/{messageType}` | 메시지 타입별 조회 | X |
| GET | `/{messageId}` | 메시지 상세 조회 | X |
| PUT | `/{messageId}` | 메시지 수정 (본인만) | O |
| DELETE | `/{messageId}` | 메시지 삭제 (본인만) | O |
| DELETE | `/{messageId}/admin` | 메시지 삭제 (관리자) | O |
| GET | `/count` | 메시지 수 조회 | X |

---

## 워크스페이스 API 상세

### 1. 워크스페이스 생성 (스터디 시작 시)

**Request**
```
POST /api/v1/workspaces/study/{studyId}
User-Id: {userId}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "studyId": 1,
  "createdAt": "2025-01-25T23:30:00"
}
```

---

### 2. 워크스페이스 조회 (ID)

**Request**
```
GET /api/v1/workspaces/{workspaceId}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "studyId": 1,
  "createdAt": "2025-01-25T23:30:00"
}
```

---

### 3. 워크스페이스 조회 (스터디 ID)

**Request**
```
GET /api/v1/workspaces/study/{studyId}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "studyId": 1,
  "createdAt": "2025-01-25T23:30:00"
}
```

---

### 4. 워크스페이스 존재 여부 확인

**Request**
```
GET /api/v1/workspaces/study/{studyId}/exists
```

**Response (200 OK)**
```json
true
```

---

### 5. 워크스페이스 삭제

**Request**
```
DELETE /api/v1/workspaces/{workspaceId}
User-Id: {userId}
```

**Response (204 No Content)**

---

### 6. 워크스페이스 삭제 (스터디 ID)

**Request**
```
DELETE /api/v1/workspaces/study/{studyId}
User-Id: {userId}
```

**Response (204 No Content)**

---

## 메시지 API 상세

### 1. 메시지 전송

**Request**
```
POST /api/v1/workspaces/{workspaceId}/messages
User-Id: {userId}
Content-Type: application/json
```
```json
{
  "workspaceId": 1,
  "content": "안녕하세요! 스터디 시작합니다.",
  "messageType": "TEXT"
}
```

| Field | Type | 필수 | 설명 |
|-------|------|------|------|
| workspaceId | Long | O | 워크스페이스 ID |
| content | String | O | 메시지 내용 (최대 2000자) |
| messageType | String | X | TEXT(기본) / IMAGE / FILE / SYSTEM |
| fileUrl | String | X | 파일 URL (IMAGE/FILE 타입 시) |

**Response (201 Created)**
```json
{
  "id": 1,
  "workspaceId": 1,
  "userId": 1,
  "nickname": "스터디장",
  "profileImageUrl": null,
  "content": "안녕하세요! 스터디 시작합니다.",
  "messageType": "TEXT",
  "fileUrl": null,
  "isDeleted": false,
  "createdAt": "2025-01-25T23:31:00",
  "updatedAt": "2025-01-25T23:31:00"
}
```

---

### 2. 이미지 메시지 전송

**Request**
```
POST /api/v1/workspaces/{workspaceId}/messages
User-Id: {userId}
Content-Type: application/json
```
```json
{
  "workspaceId": 1,
  "content": "오늘 학습 자료입니다",
  "messageType": "IMAGE",
  "fileUrl": "https://example.com/study-material.png"
}
```

---

### 3. 파일 메시지 전송

**Request**
```
POST /api/v1/workspaces/{workspaceId}/messages
User-Id: {userId}
Content-Type: application/json
```
```json
{
  "workspaceId": 1,
  "content": "과제 파일 공유합니다",
  "messageType": "FILE",
  "fileUrl": "https://example.com/homework.pdf"
}
```

---

### 4. 메시지 목록 조회 (페이징)

**Request**
```
GET /api/v1/workspaces/{workspaceId}/messages?page=0&size=50
```

| Parameter | Type | 필수 | 기본값 | 설명 |
|-----------|------|------|--------|------|
| page | int | X | 0 | 페이지 번호 |
| size | int | X | 50 | 페이지 크기 |

**Response (200 OK)**
```json
{
  "content": [
    {
      "id": 3,
      "workspaceId": 1,
      "userId": 3,
      "nickname": "스터디원2",
      "profileImageUrl": null,
      "content": "저도 잘 부탁드려요~",
      "messageType": "TEXT",
      "fileUrl": null,
      "isDeleted": false,
      "createdAt": "2025-01-25T23:33:00",
      "updatedAt": "2025-01-25T23:33:00"
    },
    {
      "id": 2,
      "workspaceId": 1,
      "userId": 2,
      "nickname": "스터디원1",
      "profileImageUrl": null,
      "content": "반갑습니다!",
      "messageType": "TEXT",
      "fileUrl": null,
      "isDeleted": false,
      "createdAt": "2025-01-25T23:32:00",
      "updatedAt": "2025-01-25T23:32:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 50,
  "totalElements": 2,
  "totalPages": 1,
  "last": true
}
```

---

### 5. 최근 메시지 조회

**Request**
```
GET /api/v1/workspaces/{workspaceId}/messages/recent?limit=20
```

| Parameter | Type | 필수 | 기본값 | 설명 |
|-----------|------|------|--------|------|
| limit | int | X | 20 | 조회할 메시지 수 |

**Response (200 OK)**
```json
[
  {
    "id": 3,
    "workspaceId": 1,
    "userId": 3,
    "nickname": "스터디원2",
    "content": "저도 잘 부탁드려요~",
    "messageType": "TEXT",
    "isDeleted": false,
    "createdAt": "2025-01-25T23:33:00"
  }
]
```

---

### 6. 특정 시간 이후 메시지 조회 (폴링)

**Request**
```
GET /api/v1/workspaces/{workspaceId}/messages/after?after=2025-01-25T23:30:00
```

| Parameter | Type | 필수 | 설명 |
|-----------|------|------|------|
| after | DateTime | O | ISO 8601 형식 |

**Response (200 OK)**
```json
[
  {
    "id": 4,
    "workspaceId": 1,
    "userId": 1,
    "nickname": "스터디장",
    "content": "새 메시지입니다",
    "messageType": "TEXT",
    "createdAt": "2025-01-25T23:35:00"
  }
]
```

---

### 7. 메시지 검색

**Request**
```
GET /api/v1/workspaces/{workspaceId}/messages/search?keyword=안녕&page=0&size=20
```

| Parameter | Type | 필수 | 설명 |
|-----------|------|------|------|
| keyword | String | O | 검색 키워드 |
| page | int | X | 페이지 번호 |
| size | int | X | 페이지 크기 |

**Response (200 OK)**
```json
{
  "content": [
    {
      "id": 1,
      "content": "안녕하세요! 스터디 시작합니다.",
      "messageType": "TEXT",
      "createdAt": "2025-01-25T23:31:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 8. 메시지 타입별 조회

**Request**
```
GET /api/v1/workspaces/{workspaceId}/messages/type/{messageType}?page=0&size=20
```

| Parameter | Type | 필수 | 설명 |
|-----------|------|------|------|
| messageType | String | O | TEXT / IMAGE / FILE / SYSTEM |

**Response (200 OK)**
```json
{
  "content": [
    {
      "id": 4,
      "content": "오늘 학습 자료입니다",
      "messageType": "IMAGE",
      "fileUrl": "https://example.com/study-material.png",
      "createdAt": "2025-01-25T23:34:00"
    }
  ],
  "totalElements": 1
}
```

---

### 9. 메시지 상세 조회

**Request**
```
GET /api/v1/workspaces/{workspaceId}/messages/{messageId}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "workspaceId": 1,
  "userId": 1,
  "nickname": "스터디장",
  "profileImageUrl": null,
  "content": "안녕하세요! 스터디 시작합니다.",
  "messageType": "TEXT",
  "fileUrl": null,
  "isDeleted": false,
  "createdAt": "2025-01-25T23:31:00",
  "updatedAt": "2025-01-25T23:31:00"
}
```

**삭제된 메시지 조회 시**
```json
{
  "id": 1,
  "workspaceId": 1,
  "userId": 1,
  "nickname": "스터디장",
  "content": "삭제된 메시지입니다.",
  "messageType": "TEXT",
  "isDeleted": true,
  "createdAt": "2025-01-25T23:31:00"
}
```

---

### 10. 메시지 수정 (본인만)

**Request**
```
PUT /api/v1/workspaces/{workspaceId}/messages/{messageId}
User-Id: {userId}
Content-Type: application/json
```
```json
{
  "content": "수정된 메시지입니다!"
}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "workspaceId": 1,
  "userId": 1,
  "nickname": "스터디장",
  "content": "수정된 메시지입니다!",
  "messageType": "TEXT",
  "isDeleted": false,
  "createdAt": "2025-01-25T23:31:00",
  "updatedAt": "2025-01-25T23:40:00"
}
```

**에러 - 본인이 아닌 경우 (400 Bad Request)**
```json
{
  "status": 400,
  "code": "INVALID_ARGUMENT",
  "message": "본인의 메시지만 수정할 수 있습니다."
}
```

---

### 11. 메시지 삭제 (본인만)

**Request**
```
DELETE /api/v1/workspaces/{workspaceId}/messages/{messageId}
User-Id: {userId}
```

**Response (204 No Content)**

**에러 - 본인이 아닌 경우 (400 Bad Request)**
```json
{
  "status": 400,
  "code": "INVALID_ARGUMENT",
  "message": "본인의 메시지만 삭제할 수 있습니다."
}
```

---

### 12. 메시지 삭제 (관리자)

**Request**
```
DELETE /api/v1/workspaces/{workspaceId}/messages/{messageId}/admin
User-Id: {adminUserId}
```

**Response (204 No Content)**

---

### 13. 메시지 수 조회

**Request**
```
GET /api/v1/workspaces/{workspaceId}/messages/count
```

**Response (200 OK)**
```json
42
```

---

## 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| WORKSPACE_NOT_FOUND | 400 | 워크스페이스를 찾을 수 없음 |
| WORKSPACE_ALREADY_EXISTS | 400 | 해당 스터디에 이미 워크스페이스 존재 |
| MESSAGE_NOT_FOUND | 400 | 메시지를 찾을 수 없음 |
| USER_NOT_FOUND | 400 | 사용자를 찾을 수 없음 |
| INVALID_ARGUMENT | 400 | 잘못된 요청 (본인 메시지 아님 등) |
| MESSAGE_ALREADY_DELETED | 400 | 이미 삭제된 메시지 |

---

## 메시지 타입

| Type | 설명 |
|------|------|
| TEXT | 일반 텍스트 메시지 |
| IMAGE | 이미지 첨부 메시지 |
| FILE | 파일 첨부 메시지 |
| SYSTEM | 시스템 알림 메시지 |