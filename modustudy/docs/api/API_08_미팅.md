# 미팅 API (Meeting)

## 기본 정보
- Base URL: `/api/v1/studies/{studyId}/meetings`
- 인증: JWT 필요
- 실시간: WebSocket/WebRTC 연동

---

## 엔드포인트 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/` | 미팅 기록 목록 | O |
| GET | `/{meetingId}` | 미팅 상세 조회 | O |
| POST | `/` | 미팅 시작 (스터디장) | O |
| PUT | `/{meetingId}/end` | 미팅 종료 (스터디장) | O |
| POST | `/{meetingId}/join` | 미팅 참여 | O |
| POST | `/{meetingId}/leave` | 미팅 퇴장 | O |
| GET | `/{meetingId}/summary` | 미팅 요약 조회 | O |
| GET | `/{meetingId}/transcript` | 미팅 전문 조회 | O |
| GET | `/{meetingId}/photos` | 활동사진 목록 | O |
| POST | `/{meetingId}/photos` | 활동사진 캡처 | O |
| PUT | `/{meetingId}/keywords` | 키워드 설정 (스터디장) | O |
| PUT | `/{meetingId}/participants/{userId}/mute` | 참여자 뮤트 (스터디장) | O |

---

## API 상세

### 1. 미팅 기록 목록

**Request**
```
GET /api/v1/studies/{studyId}/meetings?page=0&size=20
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "1회차 스터디",
        "session": {
          "id": 1,
          "sessionNumber": 1
        },
        "startedAt": "2025-01-15T19:00:00Z",
        "endedAt": "2025-01-15T20:30:00Z",
        "durationSeconds": 5400,
        "participantCount": 5,
        "hasSummary": true,
        "hasTranscript": true,
        "photoCount": 3
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5
  }
}
```

---

### 2. 미팅 상세 조회

**Request**
```
GET /api/v1/studies/{studyId}/meetings/{meetingId}
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "1회차 스터디",
    "session": {
      "id": 1,
      "sessionNumber": 1,
      "title": "OT 및 환경설정"
    },
    "workspace": {
      "id": 3,
      "name": "스터디룸"
    },
    "startedAt": "2025-01-15T19:00:00Z",
    "endedAt": "2025-01-15T20:30:00Z",
    "durationSeconds": 5400,
    "status": "ENDED",
    "participants": [
      {
        "userId": 1,
        "nickname": "홍길동",
        "joinedAt": "2025-01-15T19:00:00Z",
        "leftAt": "2025-01-15T20:30:00Z"
      }
    ],
    "keywords": ["알고리즘", "DP", "그리디"],
    "summary": {
      "id": 1,
      "summary": "이번 미팅에서는...",
      "actionItems": [
        "백준 1000번 문제 풀기",
        "DP 개념 정리"
      ]
    }
  }
}
```

---

### 3. 미팅 시작 (스터디장)

**Request**
```
POST /api/v1/studies/{studyId}/meetings
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "title": "2회차 스터디",
  "sessionId": 2,
  "workspaceId": 3
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "title": "2회차 스터디",
    "roomToken": "webrtc_room_token_here",
    "status": "IN_PROGRESS"
  }
}
```

---

### 4. 미팅 종료 (스터디장)

**Request**
```
PUT /api/v1/studies/{studyId}/meetings/{meetingId}/end
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "message": "미팅이 종료되었습니다. AI 요약이 생성 중입니다.",
  "data": {
    "durationSeconds": 5400,
    "participantCount": 5,
    "summaryStatus": "PROCESSING"
  }
}
```

---

### 5. 미팅 참여

**Request**
```
POST /api/v1/studies/{studyId}/meetings/{meetingId}/join
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "roomToken": "webrtc_room_token_here",
    "iceServers": [
      {
        "urls": "stun:stun.example.com"
      },
      {
        "urls": "turn:turn.example.com",
        "username": "user",
        "credential": "pass"
      }
    ]
  }
}
```

---

### 6. 미팅 요약 조회

**Request**
```
GET /api/v1/studies/{studyId}/meetings/{meetingId}/summary
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "summary": "이번 미팅에서는 DP와 그리디 알고리즘에 대해 학습했습니다. 주요 논의 내용은...",
    "actionItems": [
      {
        "content": "백준 1000번 문제 풀기",
        "assignee": null
      },
      {
        "content": "DP 개념 정리 문서 작성",
        "assignee": "홍길동"
      }
    ],
    "keywords": ["DP", "그리디", "다이나믹프로그래밍"],
    "createdAt": "2025-01-15T20:35:00Z"
  }
}
```

---

### 7. 미팅 전문 조회

**Request**
```
GET /api/v1/studies/{studyId}/meetings/{meetingId}/transcript?page=0&size=100
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "user": {
          "id": 1,
          "nickname": "홍길동"
        },
        "content": "오늘은 DP에 대해서 알아보겠습니다.",
        "timestampSeconds": 120,
        "createdAt": "2025-01-15T19:02:00Z"
      },
      {
        "id": 2,
        "user": {
          "id": 2,
          "nickname": "김싸피"
        },
        "content": "DP가 뭔가요?",
        "timestampSeconds": 135,
        "createdAt": "2025-01-15T19:02:15Z"
      }
    ],
    "totalElements": 500,
    "hasMore": true
  }
}
```

---

### 8. 활동사진 목록

**Request**
```
GET /api/v1/studies/{studyId}/meetings/{meetingId}/photos
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "imageUrl": "https://storage.../photos/meeting1_1.jpg",
      "capturedAt": "2025-01-15T19:30:00Z",
      "isSelected": true
    },
    {
      "id": 2,
      "imageUrl": "https://storage.../photos/meeting1_2.jpg",
      "capturedAt": "2025-01-15T20:00:00Z",
      "isSelected": false
    }
  ]
}
```

---

### 9. 활동사진 캡처

**Request**
```
POST /api/v1/studies/{studyId}/meetings/{meetingId}/photos
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
```

| Field | Type | 필수 | 설명 |
|-------|------|------|------|
| image | File | O | 캡처 이미지 |

**Response**
```json
{
  "success": true,
  "data": {
    "id": 3,
    "imageUrl": "https://storage.../photos/meeting1_3.jpg",
    "capturedAt": "2025-01-15T20:15:00Z"
  }
}
```

---

### 10. 키워드 설정 (스터디장)

**Request**
```
PUT /api/v1/studies/{studyId}/meetings/{meetingId}/keywords
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "keywords": ["알고리즘", "DP", "그리디", "BFS"]
}
```

**Response**
```json
{
  "success": true,
  "message": "키워드가 설정되었습니다."
}
```

---

### 11. 참여자 뮤트 (스터디장)

**Request**
```
PUT /api/v1/studies/{studyId}/meetings/{meetingId}/participants/{userId}/mute
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "muted": true
}
```

**Response**
```json
{
  "success": true,
  "message": "참여자가 뮤트되었습니다."
}
```

---

## WebSocket 이벤트

### STT 실시간 수신
```
SUBSCRIBE /topic/studies/{studyId}/meetings/{meetingId}/stt
```
```json
{
  "type": "STT",
  "data": {
    "userId": 1,
    "nickname": "홍길동",
    "content": "실시간 음성 텍스트...",
    "isFinal": false
  }
}
```

---

## 에러 코드

| 코드 | 설명 |
|------|------|
| MEETING_NOT_FOUND | 미팅을 찾을 수 없음 |
| MEETING_ALREADY_ENDED | 이미 종료된 미팅 |
| MEETING_IN_PROGRESS | 이미 진행 중인 미팅 있음 |
| NOT_IN_MEETING | 미팅에 참여하지 않음 |
| SUMMARY_NOT_READY | 요약이 아직 생성되지 않음 |
