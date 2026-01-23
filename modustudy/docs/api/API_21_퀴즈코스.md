# 퀴즈 코스 API (Quiz Course)

## 기본 정보
- Base URL: `/api/v1/quiz-courses`
- 인증: 일부 JWT 필요

### 정책
본 서비스는 섹션 문제의 노출 순서가 랜덤(셔플)될 수 있으므로, 제출/채점은 questionNumber가 아닌 questionId(문항 고유 식별자)를 기준으로 처리한다.

---

## 엔드포인트 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/` | 코스 목록 조회 | X |
| GET | `/{courseId}` | 코스 상세 조회 | X |
| GET | `/{courseId}/sections` | 섹션 목록 조회 | O |
| POST | `/{courseId}/sections/{sectionNumber}/attempts` | 섹션 시도 시작/재개 | O |
| PATCH | `/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/answers` | 단일 답안 실시간 저장 | O |
| POST | `/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/submit` | 섹션 제출 (채점) | O |
| DELETE | `/{courseId}/sections/{sectionNumber}/attempts/{attemptId}` | 시도 포기 | O |
| GET | `/my/progress` | 내 코스 진행 현황 | O |
| GET | `/my/progress/{courseId}` | 특정 코스 진행 상세 | O |
| GET | `/{courseId}/sections/{sectionNumber}` | (Deprecated) 섹션 문제 조회 | O |

---

## API 상세

### 1. 코스 목록 조회

**Request**
```
GET /api/v1/quiz-courses
```

**Response**
```json
{
  "success": true,
  "data": {
    "courses": [
      {
        "courseId": 1,
        "code": "JAVA",
        "name": "Java 마스터",
        "description": "Java 기초부터 고급까지",
        "totalSections": 5,
        "badgeCode": "JAVA_MASTER",
        "badgeName": "Java 마스터"
      },
      {
        "courseId": 2,
        "code": "PYTHON",
        "name": "Python 기초",
        "description": "Python 입문자를 위한 코스",
        "totalSections": 4,
        "badgeCode": "PYTHON_MASTER",
        "badgeName": "Python 마스터"
      },
      {
        "courseId": 3,
        "code": "CS_BASIC",
        "name": "CS 기초",
        "description": "컴퓨터 과학 기초 개념",
        "totalSections": 6,
        "badgeCode": "CS_MASTER",
        "badgeName": "CS 마스터"
      }
    ]
  }
}
```

---

### 2. 코스 상세 조회

**Request**
```
GET /api/v1/quiz-courses/{courseId}
```

**Response**
```json
{
  "success": true,
  "data": {
    "courseId": 1,
    "code": "JAVA",
    "name": "Java 마스터",
    "description": "Java 기초부터 고급까지 단계별 학습",
    "totalSections": 5,
    "badge": {
      "code": "JAVA_MASTER",
      "name": "Java 마스터",
      "description": "Java 코스 완료"
    },
    "sections": [
      {
        "sectionNumber": 1,
        "name": "기본 문법",
        "description": "변수, 자료형, 연산자",
        "totalQuestions": 10,
        "passScore": 70
      },
      {
        "sectionNumber": 2,
        "name": "객체지향",
        "description": "클래스, 상속, 다형성",
        "totalQuestions": 15,
        "passScore": 70
      },
      {
        "sectionNumber": 3,
        "name": "컬렉션",
        "description": "List, Map, Set",
        "totalQuestions": 12,
        "passScore": 70
      },
      {
        "sectionNumber": 4,
        "name": "람다 & 스트림",
        "description": "함수형 프로그래밍",
        "totalQuestions": 10,
        "passScore": 70
      },
      {
        "sectionNumber": 5,
        "name": "쓰레드",
        "description": "멀티쓰레딩, 동기화",
        "totalQuestions": 10,
        "passScore": 70
      }
    ]
  }
}
```

---

### 3. 섹션 목록 조회 (진행 상황 포함)

**Request**
```
GET /api/v1/quiz-courses/{courseId}/sections
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "courseId": 1,
    "courseName": "Java 마스터",
    "myProgress": {
      "currentSection": 3,
      "completedSections": 2,
      "isCompleted": false
    },
    "sections": [
      {
        "sectionNumber": 1,
        "name": "기본 문법",
        "totalQuestions": 10,
        "passScore": 70,
        "isUnlocked": true,
        "isPassed": true,
        "bestScore": 90,
        "attemptCount": 2
      },
      {
        "sectionNumber": 2,
        "name": "객체지향",
        "totalQuestions": 15,
        "passScore": 70,
        "isUnlocked": true,
        "isPassed": true,
        "bestScore": 80,
        "attemptCount": 1
      },
      {
        "sectionNumber": 3,
        "name": "컬렉션",
        "totalQuestions": 12,
        "passScore": 70,
        "isUnlocked": true,
        "isPassed": false,
        "bestScore": 50,
        "attemptCount": 1
      },
      {
        "sectionNumber": 4,
        "name": "람다 & 스트림",
        "totalQuestions": 10,
        "passScore": 70,
        "isUnlocked": false,
        "isPassed": false,
        "bestScore": null,
        "attemptCount": 0
      },
      {
        "sectionNumber": 5,
        "name": "쓰레드",
        "totalQuestions": 10,
        "passScore": 70,
        "isUnlocked": false,
        "isPassed": false,
        "bestScore": null,
        "attemptCount": 0
      }
    ]
  }
}
```

---

### 4. 섹션 시도 시작/재개
**기존 `GET /sections/{sectionNumber}`는 Deprecated 되었습니다.** (하단 참고)

**Request**
```
POST /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts
Authorization: Bearer {accessToken}
```

**Response - 시도 생성/재개 성공**
```json
{
  "success": true,
  "data": {
    "attemptId": 123,
    "sectionNumber": 1,
    "sectionName": "기본 문법",
    "status": "IN_PROGRESS",
    "totalQuestions": 10,
    "answeredCount": 0,
    "passScore": 70,
    "startedAt": "2025-01-20T10:00:00",
    "questions": [
      {
        "questionId": 101,
        "questionText": "Java에서 정수형 변수를 선언할 때 사용하는 키워드는?",
        "questionType": "MULTIPLE_CHOICE",
        "options": [
          {"id": "A", "text": "integer"},
          {"id": "B", "text": "int"},
          {"id": "C", "text": "num"},
          {"id": "D", "text": "number"}
        ],
        "savedAnswer": null
      },
      ...
    ]
  }
}
```

**Error Response - 잠긴 섹션**
```json
{
  "success": false,
  "error": {
    "code": "SECTION_LOCKED",
    "message": "이전 섹션을 먼저 완료해주세요."
  }
}
```

---

### 5. 단일 답안 실시간 저장

사용자가 문제를 풀고 **"다음" 버튼을 클릭할 때마다** 호출되어 해당 답안을 즉시 저장한다.
브라우저 충돌이나 네트워크 끊김 시에도 데이터 유실을 방지하는 실시간 저장 방식이다.

- **멱등성 보장**: 동일 questionId로 여러 번 호출해도 마지막 답안으로 덮어씀
- **호출 시점**: 각 문제의 "다음" 버튼 클릭 시, 또는 "이전" 버튼으로 돌아가기 전

**Request**
```
PATCH /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/answers
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "answer": {
    "questionId": 101,
    "answer": "B"
  }
}
```

**Response**
```json
{
  "success": true,
  "data": null
}
```

---

### 6. 섹션 제출 (채점)

**Request**
```
POST /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/submit
Authorization: Bearer {accessToken}
```
*(Body 없음 - 이미 저장된 답안으로 채점)*

**Response - 통과**
```json
{
  "success": true,
  "data": {
    "attemptId": 123,
    "score": 80,
    "correctCount": 8,
    "totalQuestions": 10,
    "passScore": 70,
    "isPassed": true,
    "isNextSectionUnlocked": true,
    "results": [
      {
        "orderIndex": 1,
        "questionId": 101,
        "userAnswer": ["B"],
        "correctAnswer": ["B"],
        "isCorrect": true,
        "explanation": "Java에서 정수형은 int 키워드를 사용합니다."
      },
      ...
    ]
  },
  "message": "축하합니다! 다음 섹션이 해금되었습니다."
}
```

**Response - 코스 완료**
```json
{
  "success": true,
  "data": {
    "attemptId": 125,
    "score": 90,
    "correctCount": 9,
    "totalQuestions": 10,
    "passScore": 70,
    "isPassed": true,
    "isNextSectionUnlocked": false,
    "earnedBadge": {
      "code": "JAVA_MASTER",
      "name": "Java 마스터",
      "icon": "☕",
      "description": "Java 코스 완료"
    },
    "results": [...]
  },
  "message": "축하합니다! Java 코스를 완료하고 Java 마스터 뱃지를 획득했습니다!"
}
```

**Response - 불통과**
```json
{
  "success": true,
  "data": {
    "attemptId": 124,
    "score": 50,
    "correctCount": 5,
    "totalQuestions": 10,
    "passScore": 70,
    "isPassed": false,
    "isNextSectionUnlocked": false,
    "results": [...]
  },
  "message": "아쉽습니다. 70% 이상 맞춰야 통과입니다. 다시 도전해보세요!"
}
```

---

### 7. 시도 포기

**Request**
```
DELETE /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}/attempts/{attemptId}
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": null
}
```

---

### 8. (Deprecated) 섹션 문제 조회
**주의: 더 이상 사용되지 않는 API입니다. `POST .../attempts`를 사용하세요.**

**Request**
```
GET /api/v1/quiz-courses/{courseId}/sections/{sectionNumber}
```
---

### 9. 내 코스 진행 현황

**Request**
```
GET /api/v1/quiz-courses/my/progress
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "inProgress": [
      {
        "courseId": 1,
        "courseCode": "JAVA",
        "courseName": "Java 마스터",
        "totalSections": 5,
        "completedSections": 2,
        "progressPercent": 40,
        "lastActivityAt": "2025-01-10T12:00:00Z"
      }
    ],
    "completed": [
      {
        "courseId": 2,
        "courseCode": "PYTHON",
        "courseName": "Python 기초",
        "totalSections": 4,
        "completedAt": "2025-01-05T15:00:00Z",
        "earnedBadge": {
          "code": "PYTHON_MASTER",
          "name": "Python 마스터",
          "icon": "🐍"
        }
      }
    ],
    "notStarted": [
      {
        "courseId": 3,
        "courseCode": "CS_BASIC",
        "courseName": "CS 기초",
        "totalSections": 6
      }
    ]
  }
}
```

---

### 10. 특정 코스 진행 상세

**Request**
```
GET /api/v1/quiz-courses/my/progress/{courseId}
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "courseId": 1,
    "courseName": "Java 마스터",
    "totalSections": 5,
    "completedSections": 2,
    "isCompleted": false,
    "startedAt": "2025-01-01T10:00:00Z",
    "sectionDetails": [
      {
        "sectionNumber": 1,
        "name": "기본 문법",
        "isPassed": true,
        "bestScore": 90,
        "attemptCount": 2,
        "passedAt": "2025-01-02T14:00:00Z"
      },
      {
        "sectionNumber": 2,
        "name": "객체지향",
        "isPassed": true,
        "bestScore": 80,
        "attemptCount": 1,
        "passedAt": "2025-01-05T16:00:00Z"
      },
      {
        "sectionNumber": 3,
        "name": "컬렉션",
        "isPassed": false,
        "bestScore": 50,
        "attemptCount": 1,
        "passedAt": null
      },
      {
        "sectionNumber": 4,
        "name": "람다 & 스트림",
        "isPassed": false,
        "bestScore": null,
        "attemptCount": 0,
        "passedAt": null
      },
      {
        "sectionNumber": 5,
        "name": "쓰레드",
        "isPassed": false,
        "bestScore": null,
        "attemptCount": 0,
        "passedAt": null
      }
    ]
  }
}
```

---

## 에러 코드

| 코드 | 설명 |
|------|------|
| COURSE_NOT_FOUND | 코스를 찾을 수 없음 |
| SECTION_NOT_FOUND | 섹션을 찾을 수 없음 |
| SECTION_LOCKED | 잠긴 섹션 (이전 섹션 미완료) |
| INVALID_ANSWERS | 답변 형식 오류 |
