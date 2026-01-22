# 스터디 멤버 API (Study Member)

## 기본 정보
- Base URL: 
  - 멤버 조회: `/api/v1/study/{studyId}/members`
  - 신청 관리: `/api/v1/study/{studyId}/applications`
  - 내 신청: `/api/v1/my/applications`
- 인증: JWT 필요
 
---

## 엔드포인트 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/v1/study/{studyId}/members` | 멤버 목록 조회 | O |
| GET | `/api/v1/study/{studyId}/members/count` | 멤버 수 조회 | O |
| GET | `/api/v1/study/{studyId}/members/{userId}/check` | 멤버 여부 확인 | O |
| POST | `/api/v1/study/{studyId}/applications` | 가입 신청 | O |
| GET | `/api/v1/study/{studyId}/applications` | 신청 목록 조회 (스터디장) | O |
| GET | `/api/v1/my/applications` | 내 신청 현황 | O |
| GET | `/api/v1/applications/{applicationId}` | 신청 상세 조회 | O |
| PATCH | `/api/v1/study/{studyId}/applications/{applicationId}/approve` | 신청 승인 (스터디장) | O |
| PATCH | `/api/v1/study/{studyId}/applications/{applicationId}/reject` | 신청 거절 (스터디장) | O |
| GET | `/api/v1/user/{userId}/applications` | 사용자별 신청 내역 조회 | O |
| PUT | `/{memberId}/role` | 역할 변경 (스터디장) - 미구현 | O |
| DELETE | `/{memberId}` | 멤버 추방 (스터디장) - 미구현 | O |
| DELETE | `/leave` | 스터디 탈퇴 - 미구현 | O |
| POST | `/{memberId}/review` | 스터디장 평가 - 미구현 | O |
| GET | `/expulsion-risk` | 자동 추방 위험 멤버 조회 (스터디장) - 미구현 | O |
| GET | `/my/expulsion-status` | 내 추방 위험 상태 조회 - 미구현 | O |

---

## API 상세

### 1. 멤버 목록 조회

**Request**
```
GET /api/v1/study/{studyId}/members
user-id: {userId}
```

**Response**
```json
{
  "content": [
    {
      "memberId": 1,
      "studyId": 1,
      "userId": 1,
      "userName": "홍길동",
      "userNickname": "hong",
      "userEmail": "hong@example.com",
      "role": "LEADER",
      "status": "APPROVED",
      "isProbation": false,
      "joinedAt": "2025-01-01T00:00:00"
    },
    {
      "memberId": 2,
      "studyId": 1,
      "userId": 2,
      "userName": "김싸피",
      "userNickname": "kim",
      "userEmail": "kim@example.com",
      "role": "MEMBER",
      "status": "APPROVED",
      "isProbation": true,
      "joinedAt": "2025-01-10T00:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true
}
```

---

### 2. 가입 신청

**Request**
```
POST /api/v1/study/{studyId}/applications
user-id: {userId}
Content-Type: application/json
```
```json
{
  "message": "안녕하세요! 알고리즘 공부를 열심히 하고 싶어서 지원합니다."
}
```

**Response**
```json
{
  "applicationId": 1,
  "studyId": 1,
  "studyName": "알고리즘 스터디",
  "userId": 3,
  "userName": "이싸피",
  "userNickname": "lee",
  "userEmail": "lee@example.com",
  "message": "안녕하세요! 알고리즘 공부를 열심히 하고 싶어서 지원합니다.",
  "matchingScore": null,
  "status": "PENDING",
  "rejectedReason": null,
  "createdAt": "2025-01-10T00:00:00",
  "processedAt": null
}
```

---

### 3. 신청 목록 조회 (스터디장)

**Request**
```
GET /api/v1/study/{studyId}/applications?status=PENDING
user-id: {userId}
```

| Parameter | Type | 필수 | 설명 |
|-----------|------|------|------|
| status | string | X | PENDING/APPROVED/REJECTED |

**Response**
```json
{
  "content": [
    {
      "applicationId": 1,
      "studyId": 1,
      "studyName": "알고리즘 스터디",
      "userId": 3,
      "userName": "이싸피",
      "userNickname": "lee",
      "userEmail": "lee@example.com",
      "message": "안녕하세요! 알고리즘 공부를 열심히 하고 싶어서 지원합니다.",
      "matchingScore": 85.5,
      "status": "PENDING",
      "rejectedReason": null,
      "createdAt": "2025-01-10T00:00:00",
      "processedAt": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 4. 내 신청 현황

**Request**
```
GET /api/v1/my/applications
user-id: {userId}
```

**Response**
```json
{
  "content": [
    {
      "applicationId": 1,
      "studyId": 1,
      "studyName": "알고리즘 스터디",
      "userId": 1,
      "userName": "홍길동",
      "userNickname": "hong",
      "userEmail": "hong@example.com",
      "message": "열심히 하겠습니다",
      "matchingScore": null,
      "status": "PENDING",
      "rejectedReason": null,
      "createdAt": "2025-01-10T00:00:00",
      "processedAt": null
    },
    {
      "applicationId": 2,
      "studyId": 2,
      "studyName": "CS 스터디",
      "userId": 1,
      "userName": "홍길동",
      "userNickname": "hong",
      "userEmail": "hong@example.com",
      "message": "CS 공부 함께 하고 싶습니다",
      "matchingScore": null,
      "status": "APPROVED",
      "rejectedReason": null,
      "createdAt": "2025-01-08T00:00:00",
      "processedAt": "2025-01-09T00:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true
}
```

---

### 5. 신청 승인/거절 (스터디장)

**Request**
```
PATCH /api/v1/study/{studyId}/applications/{applicationId}/approve
user-id: {userId}
```

**Response**
```json
{
  "applicationId": 1,
  "studyId": 1,
  "studyName": "알고리즘 스터디",
  "userId": 3,
  "userName": "이싸피",
  "userNickname": "lee",
  "userEmail": "lee@example.com",
  "message": "안녕하세요! 알고리즘 공부를 열심히 하고 싶어서 지원합니다.",
  "matchingScore": 85.5,
  "status": "APPROVED",
  "rejectedReason": null,
  "createdAt": "2025-01-10T00:00:00",
  "processedAt": "2025-01-11T00:00:00"
}
```

---

### 5-2. 신청 거절 (스터디장)

**Request**
```
PATCH /api/v1/study/{studyId}/applications/{applicationId}/reject
user-id: {userId}
Content-Type: application/json
```
```json
{
  "rejectedReason": "죄송합니다. 현재 정원이 마감되었습니다."
}
```

**Response**
```json
{
  "applicationId": 1,
  "studyId": 1,
  "studyName": "알고리즘 스터디",
  "userId": 3,
  "userName": "이싸피",
  "userNickname": "lee",
  "userEmail": "lee@example.com",
  "message": "안녕하세요! 알고리즘 공부를 열심히 하고 싶어서 지원합니다.",
  "matchingScore": 85.5,
  "status": "REJECTED",
  "rejectedReason": "죄송합니다. 현재 정원이 마감되었습니다.",
  "createdAt": "2025-01-10T00:00:00",
  "processedAt": "2025-01-11T00:00:00"
}
```

---

---

### 5-3. 멤버 수 조회

**Request**
```
GET /api/v1/study/{studyId}/members/count
user-id: {userId}
```

**Response**
```json
5
```

---

### 5-4. 멤버 여부 확인

**Request**
```
GET /api/v1/study/{studyId}/members/{userId}/check
user-id: {requestUserId}
```

**Response**
```json
true
```

---

### 5-5. 신청 상세 조회

**Request**
```
GET /api/v1/applications/{applicationId}
user-id: {userId}
```

**Response**
```json
{
  "applicationId": 1,
  "studyId": 1,
  "studyName": "알고리즘 스터디",
  "userId": 3,
  "userName": "이싸피",
  "userNickname": "lee",
  "userEmail": "lee@example.com",
  "message": "안녕하세요! 알고리즘 공부를 열심히 하고 싶어서 지원합니다.",
  "matchingScore": 85.5,
  "status": "PENDING",
  "rejectedReason": null,
  "createdAt": "2025-01-10T00:00:00",
  "processedAt": null
}
```

---

### 5-6. 사용자별 신청 내역 조회

**Request**
```
GET /api/v1/user/{userId}/applications?status=PENDING
user-id: {requestUserId}
```

| Parameter | Type | 필수 | 설명 |
|-----------|------|------|------|
| status | string | X | PENDING/APPROVED/REJECTED |

**Response**
```json
{
  "content": [
    {
      "applicationId": 1,
      "studyId": 1,
      "studyName": "알고리즘 스터디",
      "userId": 3,
      "userName": "이싸피",
      "userNickname": "lee",
      "userEmail": "lee@example.com",
      "message": "안녕하세요! 알고리즘 공부를 열심히 하고 싶어서 지원합니다.",
      "matchingScore": 85.5,
      "status": "PENDING",
      "rejectedReason": null,
      "createdAt": "2025-01-10T00:00:00",
      "processedAt": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 6. 역할 변경 (스터디장) - 미구현

**Request**
```
PUT /api/v1/study/{studyId}/members/{memberId}/role
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "role": "LEADER"
}
```

**Response**
```json
{
  "success": true,
  "message": "역할이 변경되었습니다."
}
```

---

### 7. 멤버 추방 (스터디장) - 미구현

**Request**
```
DELETE /api/v1/study/{studyId}/members/{memberId}
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "message": "멤버가 추방되었습니다."
}
```

---

### 8. 스터디 탈퇴 - 미구현

**Request**
```
DELETE /api/v1/study/{studyId}/members/leave
Authorization: Bearer {accessToken}
```

**Response - 수습기간 내 탈퇴**
```json
{
  "success": true,
  "message": "스터디에서 탈퇴했습니다.",
  "data": {
    "penaltyApplied": false
  }
}
```

**Response - 수습기간 후 탈퇴**
```json
{
  "success": true,
  "message": "스터디에서 탈퇴했습니다.",
  "data": {
    "penaltyApplied": true,
    "penaltyType": "NORMAL",
    "penaltyDescription": "중도 탈퇴로 인한 페널티가 적용되었습니다."
  }
}
```

---

### 9. 스터디장 평가 - 미구현

**Request**
```
POST /api/v1/study/{studyId}/members/{leaderId}/review
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "rating": 4.5,
  "comment": "체계적으로 스터디를 이끌어주셔서 좋았습니다."
}
```

**Response**
```json
{
  "success": true,
  "message": "평가가 등록되었습니다."
}
```

---

### 10. 자동 추방 위험 멤버 조회 (스터디장) - 미구현

남은 세션 모두 출석해도 50% 이하 출석률이 되는 멤버 목록

**Request**
```
GET /api/v1/study/{studyId}/members/expulsion-risk
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": [
    {
      "memberId": 3,
      "userId": 5,
      "nickname": "김탈퇴",
      "profileImage": "https://...",
      "currentAttendance": 2,
      "totalSessions": 8,
      "remainingSessions": 4,
      "currentRate": 33.3,
      "maxPossibleRate": 50.0,
      "willBeExpelled": true,
      "expulsionDate": "2025-02-01",
      "warningNotifiedAt": "2025-01-29T09:00:00Z"
    }
  ]
}
```

---

### 11. 내 추방 위험 상태 조회 - 미구현

**Request**
```
GET /api/v1/study/{studyId}/members/my/expulsion-status
Authorization: Bearer {accessToken}
```

**Response - 위험 없음**
```json
{
  "success": true,
  "data": {
    "atRisk": false,
    "currentAttendance": 5,
    "totalSessions": 8,
    "remainingSessions": 2,
    "currentRate": 83.3,
    "maxPossibleRate": 100.0,
    "minRequiredAttendance": 0
  }
}
```

**Response - 추방 위험**
```json
{
  "success": true,
  "data": {
    "atRisk": true,
    "currentAttendance": 1,
    "totalSessions": 8,
    "remainingSessions": 3,
    "currentRate": 20.0,
    "maxPossibleRate": 50.0,
    "willBeExpelled": true,
    "expulsionDate": "2025-02-01",
    "daysUntilExpulsion": 3,
    "warningMessage": "남은 세션을 모두 출석해도 출석률 50% 이하로 자동 추방 예정입니다."
  }
}
```

---

## 에러 코드

| 코드 | 설명 |
|------|------|
| ALREADY_APPLIED | 이미 신청한 스터디 |
| APPLICATION_NOT_FOUND | 신청을 찾을 수 없음 |
| NOT_STUDY_MEMBER | 스터디 멤버가 아님 |
| CANNOT_LEAVE_AS_LEADER | 스터디장은 탈퇴 불가 (양도 필요) |
| ALREADY_REVIEWED | 이미 평가 완료 |
| STUDY_NOT_COMPLETED | 스터디 완료 후 평가 가능 |
