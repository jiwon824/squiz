# 퀴즈 대회 API (Quiz Contest)

## 기본 정보
- Base URL: `/api/v1/quiz-contests`
- 인증: 일부 엔드포인트 비로그인 허용
- 실시간: WebSocket 연동

---

## 엔드포인트 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/` | 대회 목록 조회 | X |
| GET | `/{contestId}` | 대회 상세 조회 | X |
| POST | `/{contestId}/join` | 대회 참여 | △ |
| GET | `/{contestId}/questions/{questionId}` | 문제 조회 (실시간) | △ |
| POST | `/{contestId}/questions/{questionId}/answer` | 답안 제출 | △ |
| GET | `/{contestId}/result` | 대회 결과 조회 | △ |
| GET | `/{contestId}/result/detail` | 대회 결과 상세 (틀린 문제) | △ |
| GET | `/{contestId}/ranking` | 대회 순위 조회 | X |
| GET | `/my-records` | 내 대회 기록 | O |

---

## API 상세

### 1. 대회 목록 조회

**Request**
```
GET /api/v1/quiz-contests?status=IN_PROGRESS&page=0&size=20
```

| Parameter | Type | 필수 | 설명 |
|-----------|------|------|------|
| status | string | X | SCHEDULED/IN_PROGRESS/ENDED |
| page | int | X | 페이지 번호 |
| size | int | X | 페이지 크기 |

**Response**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "SSAFY 13기 알고리즘 퀴즈",
        "description": "알고리즘 기초 문제",
        "status": "SCHEDULED",
        "scheduledAt": "2025-01-20T14:00:00Z",
        "timeLimitSeconds": 30,
        "questionCount": 10,
        "participantCount": 0
      },
      {
        "id": 2,
        "title": "CS 기초 퀴즈",
        "description": "운영체제, 네트워크 기초",
        "status": "IN_PROGRESS",
        "startedAt": "2025-01-10T14:00:00Z",
        "timeLimitSeconds": 20,
        "questionCount": 15,
        "participantCount": 45
      }
    ],
    "page": 0,
    "totalElements": 10
  }
}
```

---

### 2. 대회 상세 조회

**Request**
```
GET /api/v1/quiz-contests/{contestId}
```

**Response**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "SSAFY 13기 알고리즘 퀴즈",
    "description": "알고리즘 기초 문제를 풀어보세요!",
    "status": "SCHEDULED",
    "scheduledAt": "2025-01-20T14:00:00Z",
    "timeLimitSeconds": 30,
    "questionCount": 10,
    "participantCount": 25,
    "createdBy": {
      "id": 1,
      "nickname": "관리자"
    }
  }
}
```

---

### 3. 대회 참여

**Request - 로그인 사용자**
```
POST /api/v1/quiz-contests/{contestId}/join
Authorization: Bearer {accessToken}
```

**Request - 비로그인 사용자**
```
POST /api/v1/quiz-contests/{contestId}/join
Content-Type: application/json
```
```json
{
  "nickname": "익명참가자123"
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "participantId": 100,
    "nickname": "홍길동",
    "contestStatus": "SCHEDULED",
    "startsIn": 300
  }
}
```

---

### 4. 문제 조회 (WebSocket으로 실시간 수신 권장)

**Request**
```
GET /api/v1/quiz-contests/{contestId}/questions/{questionId}
Authorization: Bearer {accessToken}  // 또는 participantId 쿠키
```

**Response**
```json
{
  "success": true,
  "data": {
    "questionNumber": 1,
    "questionText": "다음 중 시간복잡도가 O(n log n)인 정렬 알고리즘은?",
    "questionType": "MULTIPLE_CHOICE",
    "options": [
      {"id": "A", "text": "버블 정렬"},
      {"id": "B", "text": "퀵 정렬"},
      {"id": "C", "text": "삽입 정렬"},
      {"id": "D", "text": "선택 정렬"}
    ],
    "points": 10,
    "timeLimit": 30,
    "startedAt": "2025-01-20T14:00:00Z",
    "endsAt": "2025-01-20T14:00:30Z"
  }
}
```

---

### 5. 답안 제출

**Request**
```
POST /api/v1/quiz-contests/{contestId}/questions/{questionId}/answer
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "answer": "B"
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "isCorrect": true,
    "score": 10,
    "totalScore": 10,
    "currentRank": 5,
    "correctAnswer": "B",
    "explanation": "퀵 정렬의 평균 시간복잡도는 O(n log n)입니다."
  }
}
```

---

### 6. 대회 결과 조회

**Request**
```
GET /api/v1/quiz-contests/{contestId}/result
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "contestId": 1,
    "contestTitle": "SSAFY 13기 알고리즘 퀴즈",
    "myResult": {
      "rank": 5,
      "totalParticipants": 50,
      "totalScore": 80,
      "maxScore": 100,
      "correctCount": 8,
      "totalQuestions": 10,
      "accuracy": 80.0
    },
    "topRankers": [
      {"rank": 1, "nickname": "알고왕", "score": 100},
      {"rank": 2, "nickname": "퀴즈마스터", "score": 95},
      {"rank": 3, "nickname": "CS덕후", "score": 90}
    ]
  }
}
```

---

### 7. 대회 결과 상세 (틀린 문제)

**Request**
```
GET /api/v1/quiz-contests/{contestId}/result/detail
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "questions": [
      {
        "questionNumber": 3,
        "questionText": "해시 테이블의 평균 검색 시간복잡도는?",
        "questionType": "MULTIPLE_CHOICE",
        "myAnswer": "B",
        "correctAnswer": "A",
        "isCorrect": false,
        "explanation": "해시 테이블은 평균 O(1)의 검색 시간을 가집니다.",
        "accuracyRate": 45.2
      },
      {
        "questionNumber": 7,
        "questionText": "TCP와 UDP의 차이점으로 옳지 않은 것은?",
        "myAnswer": "C",
        "correctAnswer": "D",
        "isCorrect": false,
        "explanation": "TCP는 연결 지향, UDP는 비연결 지향입니다.",
        "accuracyRate": 62.5
      }
    ],
    "totalQuestions": 10,
    "wrongCount": 2
  }
}
```

---

### 8. 대회 순위 조회

**Request**
```
GET /api/v1/quiz-contests/{contestId}/ranking?page=0&size=50
```

**Response**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "rank": 1,
        "nickname": "알고왕",
        "score": 100,
        "correctCount": 10,
        "isMe": false
      },
      {
        "rank": 2,
        "nickname": "퀴즈마스터",
        "score": 95,
        "correctCount": 9,
        "isMe": false
      },
      {
        "rank": 5,
        "nickname": "홍길동",
        "score": 80,
        "correctCount": 8,
        "isMe": true
      }
    ],
    "totalParticipants": 50
  }
}
```

---

### 9. 내 대회 기록

**Request**
```
GET /api/v1/quiz-contests/my-records?page=0&size=20
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "contestId": 1,
        "contestTitle": "SSAFY 13기 알고리즘 퀴즈",
        "participatedAt": "2025-01-20T14:00:00Z",
        "rank": 5,
        "totalParticipants": 50,
        "score": 80,
        "maxScore": 100,
        "accuracy": 80.0
      }
    ],
    "totalContests": 5,
    "averageRank": 8.2,
    "averageAccuracy": 75.5
  }
}
```

---

## WebSocket 이벤트

### 연결 및 구독
```
CONNECT /ws/quiz
SUBSCRIBE /topic/quiz-contests/{contestId}
```

### 대회 시작 알림
```json
{
  "type": "CONTEST_START",
  "data": {
    "contestId": 1,
    "message": "대회가 시작되었습니다!"
  }
}
```

### 문제 출제
```json
{
  "type": "QUESTION",
  "data": {
    "questionNumber": 1,
    "questionText": "다음 중 시간복잡도가 O(n log n)인 정렬 알고리즘은?",
    "questionType": "MULTIPLE_CHOICE",
    "options": [
      {"id": "A", "text": "버블 정렬"},
      {"id": "B", "text": "퀵 정렬"}
    ],
    "timeLimit": 30,
    "endsAt": "2025-01-20T14:00:30Z"
  }
}
```

### 정답 공개
```json
{
  "type": "ANSWER_REVEAL",
  "data": {
    "questionNumber": 1,
    "correctAnswer": "B",
    "explanation": "퀵 정렬의 평균 시간복잡도는 O(n log n)입니다.",
    "yourAnswer": "B",
    "isCorrect": true,
    "score": 10
  }
}
```

### 실시간 순위
```json
{
  "type": "RANKING_UPDATE",
  "data": {
    "topRankers": [
      {"rank": 1, "nickname": "알고왕", "score": 30}
    ],
    "myRank": 5,
    "myScore": 20
  }
}
```

### 대회 종료
```json
{
  "type": "CONTEST_END",
  "data": {
    "message": "대회가 종료되었습니다!",
    "finalRank": 5,
    "finalScore": 80
  }
}
```

---

## 에러 코드

| 코드 | 설명 |
|------|------|
| CONTEST_NOT_FOUND | 대회를 찾을 수 없음 |
| CONTEST_NOT_STARTED | 대회가 아직 시작되지 않음 |
| CONTEST_ALREADY_ENDED | 이미 종료된 대회 |
| ALREADY_JOINED | 이미 참여한 대회 |
| TIME_EXPIRED | 답안 제출 시간 초과 |
| NICKNAME_REQUIRED | 닉네임 입력 필요 (비로그인) |
