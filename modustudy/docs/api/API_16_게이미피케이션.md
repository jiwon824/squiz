# 게이미피케이션 API (Gamification)

## 기본 정보
- Base URL: `/api/v1/gamification`
- 인증: JWT 필요 (개발 환경에서는 일부 API 인증 제외)
- 구현 상태: ✅ **전체 구현 완료** (2026-01-26)

---

## 📋 목차
- [구현 현황](#-구현-현황)
- [이벤트 시스템](#-이벤트-시스템)
- [엔드포인트 목록](#엔드포인트-목록)
- [API 상세](#api-상세)
- [레벨 시스템](#레벨-시스템)
- [데이터 모델](#-데이터-모델)
- [에러 코드](#에러-코드)

---

## ✅ 구현 현황

### Backend 구현 완료
- ✅ 이벤트 시스템 (StudyAttendanceEvent, QuizSolvedEvent)
- ✅ 자동 데이터 기록 (DailyContribution, ContributionDetail, UserStats)
- ✅ 전체 조회 API (6개)
- ✅ 경험치 & 레벨 시스템
- ⏳ 뱃지 자동 획득 (수동 관리)
- ⏳ 패널티 자동 해소 (수동 관리)

### 테스트 필요 항목
자세한 내용은 `GAMIFICATION_TEST_GUIDE.md` 참고

---

## 🔔 이벤트 시스템

백엔드에서 특정 활동 발생 시 자동으로 게이미피케이션 데이터가 기록됩니다.

### 자동 처리되는 이벤트

#### 1. 스터디 출석 (StudyAttendanceEvent)
**발생 시점:** 사용자가 스터디에 출석 체크할 때

**자동 처리:**
- 잔디 기록 (`DailyContribution`) +1
- 활동 상세 (`ContributionDetail`) 기록
- 경험치 +10

**백엔드 코드 예시:**
```java
// 스터디 출석 처리 후
applicationEventPublisher.publishEvent(
    new StudyAttendanceEvent(userId, studyId, studyName, LocalDate.now())
);
```

---

#### 2. 퀴즈 풀이 (QuizSolvedEvent)
**발생 시점:** 사용자가 퀴즈를 제출할 때

**자동 처리:**
- 잔디 기록 (`DailyContribution`) +1
- 활동 상세 (`ContributionDetail`) 기록
- 경험치: 정답 +5, 오답 +2

**백엔드 코드 예시:**
```java
// 퀴즈 제출 처리 후
applicationEventPublisher.publishEvent(
    new QuizSolvedEvent(userId, quizId, quizTitle, isCorrect, LocalDate.now())
);
```

### 경험치 획득 규칙
| 활동 | 경험치 | 비고 |
|------|--------|------|
| 스터디 출석 | +10 | 매일 1회 |
| 퀴즈 정답 | +5 | 제한 없음 |
| 퀴즈 오답 | +2 | 참여 보상 |

> **프론트엔드 개발자 참고:**  
> 스터디 출석이나 퀴즈 제출 API 호출 후 별도로 게이미피케이션 API를 호출할 필요 없습니다.  
> 백엔드에서 자동으로 처리됩니다!

---

## 엔드포인트 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/contributions` | 잔디 그래프 조회 | O |
| GET | `/contributions/{date}` | 특정 날짜 활동 상세 | O |
| GET | `/stats` | 내 활동 통계 | O |
| GET | `/badges` | 뱃지 목록 (긍정/영구) | O |
| GET | `/penalties` | 패널티 목록 (부정/일시) | O |
| GET | `/studies/{studyId}/ranking` | 팀 내 랭킹 | O |

---

## API 상세

### 1. 잔디 그래프 조회

**Request**
```
GET /api/v1/gamification/contributions?year=2025&month=1
Authorization: Bearer {accessToken}
```

| Parameter | Type | 필수 | 설명 |
|-----------|------|------|------|
| year | int | O | 연도 |
| month | int | X | 월 (없으면 연간 조회) |

**Response - 월간 조회**
```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "year": 2026,
    "month": 1,
    "contributions": [
      {"date": "2026-01-01", "hasActivity": false},
      {"date": "2026-01-02", "hasActivity": false},
      {"date": "2026-01-03", "hasActivity": false},
      {"date": "2026-01-04", "hasActivity": true}
    ],
    "summary": {
      "totalDays": 31,
      "activeDays": 15,
      "currentStreak": 3,
      "maxStreak": 7
    }
  }
}
```

**Response - 연간 조회**
```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "year": 2026,
    "contributions": [
      {"date": "2026-01-01", "hasActivity": true},
      {"date": "2026-01-02", "hasActivity": true}
    ],
    "summary": {
      "totalDays": 365,
      "activeDays": 45,
      "currentStreak": 3,
      "maxStreak": 15
    },
    "monthlyStats": [
      {"month": 1, "activeDays": 15},
      {"month": 2, "activeDays": 12}
    ]
  }
}
```

**프론트엔드 구현 팁:**
- GitHub 스타일 잔디 그래프: `hasActivity`로 색상 표시 (있음/없음)
- 주별로 그룹화해서 표시
- 호버 시 날짜와 활동 여부 표시

---

### 2. 특정 날짜 활동 상세

**Request**
```
GET /api/v1/gamification/contributions/2025-01-15
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "date": "2026-01-26",
    "hasActivity": true,
    "activities": [
      {
        "type": "STUDY_ATTENDANCE",
        "referenceId": 1,
        "referenceName": "알고리즘 스터디 1회차",
        "createdAt": "2026-01-26T19:00:00"
      },
      {
        "type": "QUIZ_SOLVED",
        "referenceId": 5,
        "referenceName": "CS 기초 퀴즈",
        "createdAt": "2026-01-26T14:20:00"
      }
    ]
  }
}
```

**Activity Type 종류:**
- `STUDY_ATTENDANCE`: 스터디 출석
- `QUIZ_SOLVED`: 퀴즈 풀이
- `MATERIAL_UPLOAD`: 자료 업로드 (추후 구현)
- `RETROSPECTIVE`: 회고 작성 (추후 구현)

---

### 3. 내 활동 통계

**Request**
```
GET /api/v1/gamification/stats
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "level": 5,
    "levelName": "마스터",
    "levelProgress": {
      "current": 15,
      "required": 40,
      "percentage": 37.5
    },
    "nextLevel": {
      "level": 6,
      "name": "그랜드마스터"
    },
    "totalActivityDays": 75,
    "currentStreak": 7,
    "maxStreak": 15,
    "lastActivityDate": "2026-01-26",
    "totalStudiesJoined": 3,
    "totalStudiesLed": 1,
    "totalAttendance": 75,
    "totalChatCount": 150,
    "totalQuizCount": 12,
    "totalMaterialsUploaded": 8,
    "totalRetrospectives": 5,
    "joinedAt": "2025-01-01T00:00:00"
  }
}
```

**프론트엔드 구현 팁:**
- 경험치 진행률 바 표시: `(current / required) * 100`
- 연속 활동일 강조 표시 (🔥 아이콘 활용)
- 최대 연속일 기록 표시로 동기부여

---

### 4. 뱃지 목록 (긍정/영구)

> 한번 획득하면 영구 유지되는 긍정적 보상

**Request**
```
GET /api/v1/gamification/badges
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "totalBadges": 6,
    "earnedCount": 1,
    "categories": [
      {
        "category": "ACTIVITY",
        "categoryName": "활동",
        "badges": [
          {
            "id": 1,
            "code": "FIRST_ACTIVITY",
            "name": "첫 발걸음",
            "description": "첫 활동 기록",
            "icon": "👣",
            "isEarned": true,
            "earnedAt": "2026-01-20T10:30:00",
            "progress": null
          },
          {
            "id": 2,
            "code": "ACTIVITY_30",
            "name": "꾸준함의 시작",
            "description": "30일 활동 달성",
            "icon": "🌱",
            "isEarned": false,
            "earnedAt": null,
            "progress": {
              "current": 15,
              "required": 30,
              "percentage": 50.0
            }
          }
        ]
      },
      {
        "category": "STREAK",
        "categoryName": "스트릭",
        "badges": [...]
      }
    ]
  }
}
```

**Rarity (희귀도):**
- `COMMON`: 일반 (흰색/회색)
- `RARE`: 희귀 (파란색)
- `EPIC`: 영웅 (보라색)
- `LEGENDARY`: 전설 (금색)

**프론트엔드 구현 팁:**
- 획득 뱃지는 컬러로, 미획득은 그레이스케일로 표시
- 진행도 있는 뱃지는 프로그레스 바 표시
- 희귀도별로 다른 테두리 색상/효과 적용

---

### 5. 패널티 목록 (부정/일시)

> 일시적으로 부여되며, 조건 충족 시 해소 가능

**Request**
```
GET /api/v1/gamification/penalties
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "success": true,
  "data": {
    "totalActive": 2,
    "totalRemoved": 1,
    "activePenalties": [
      {
        "id": 1,
        "penaltyType": "THREE_DAY_QUIT",
        "name": "작심삼일",
        "description": "스터디 3회 연속 불참",
        "severity": "MEDIUM",
        "grantedAt": "2025-01-10T00:00:00Z",
        "expiresAt": "2025-01-17T00:00:00Z",
        "studyId": 1,
        "studyName": "알고리즘 스터디",
        "isActive": true,
        "removalCondition": "3일 연속 출석 시 해소",
        "removalProgress": {
          "current": 1,
          "required": 3
        }
      },
      {
        "id": 2,
        "penaltyType": "GHOST_MEMBER",
        "name": "유령회원",
        "description": "7일 이상 미접속",
        "severity": "LOW",
        "grantedAt": "2025-01-08T00:00:00Z",
        "expiresAt": null,
        "studyId": null,
        "studyName": null,
        "isActive": true,
        "removalCondition": "3일 연속 로그인 시 해소",
        "removalProgress": {
          "current": 0,
          "required": 3
        }
      }
    ],
    "removedPenalties": [
      {
        "id": 3,
        "penaltyType": "LATE_KING",
        "name": "지각왕",
        "description": "3회 연속 지각",
        "severity": "LOW",
        "grantedAt": "2025-01-05T00:00:00Z",
        "removedAt": "2025-01-12T00:00:00Z",
        "studyId": 1,
        "studyName": "알고리즘 스터디"
      }
    ]
  }
}
```

**Severity (심각도):**
- `LOW`: 낮음 (경고) - 노란색
- `MEDIUM`: 중간 (제재) - 주황색
- `HIGH`: 높음 (강력 제재) - 빨간색

**프론트엔드 구현 팁:**
- 활성 패널티는 눈에 띄게 표시 (애니메이션 효과)
- 해소 조건과 진행도를 명확히 표시
- 해소된 패널티는 접어두거나 별도 탭으로 분리

---

### 6. 팀 내 랭킹

**Request**
```
GET /api/v1/gamification/studies/{studyId}/ranking
Authorization: Bearer {accessToken}
```

**Response**
```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "rankings": [
      {
        "rank": 1,
        "user": {
          "id": 1,
          "nickname": "홍길동",
          "profileImage": "https://...",
          "level": 5,
          "levelName": "마스터"
        },
        "totalExperience": 150,
        "activityDays": 75,
        "attendanceRate": 100.0,
        "isMe": false
      },
      {
        "rank": 2,
        "user": {
          "id": 2,
          "nickname": "김싸피",
          "profileImage": "https://...",
          "level": 4,
          "levelName": "성실러"
        },
        "totalExperience": 120,
        "activityDays": 60,
        "attendanceRate": 90.0,
        "isMe": true
      }
    ],
    "myRank": 2,
    "totalMembers": 6
  }
}
```

**프론트엔드 구현 팁:**
- 1-3위는 메달 아이콘 표시 (🥇🥈🥉)
- 내 순위는 하이라이트 처리
- 출석률을 프로그레스 바로 시각화

---

## 레벨 시스템

> 활동일 수(totalActivityDays) 기반으로 레벨이 결정됩니다.

| 레벨 | 이름 | 필요 활동일 | 설명 |
|------|------|------------|------|
| 1 | 새싹 | 0일 | 시작 단계 |
| 2 | 학습자 | 7일 | 일주일 활동 |
| 3 | 열공러 | 15일 | 2주 이상 활동 |
| 4 | 성실러 | 30일 | 한 달 활동 |
| 5 | 마스터 | 60일 | 두 달 활동 |
| 6 | 그랜드마스터 | 100일 | 백일 달성 |

**레벨 진행도 계산:**
- `current`: 현재 레벨에서의 진행도
- `required`: 다음 레벨까지 필요한 활동일
- `percentage`: (current / required) * 100

---

## 📊 데이터 모델

### UserStats (사용자 통계)
```typescript
interface UserStats {
  level: number;
  levelName: string;
  levelProgress: {
    current: number;      // 현재 레벨에서의 진행도
    required: number;     // 다음 레벨까지 필요한 활동일
    percentage: number;   // 진행률 (%)
  };
  nextLevel: {
    level: number;
    name: string;
  };
  totalActivityDays: number;
  currentStreak: number;
  maxStreak: number;
  lastActivityDate: string | null; // yyyy-MM-dd
  totalStudiesJoined: number;
  totalStudiesLed: number;
  totalAttendance: number;
  totalChatCount: number;
  totalQuizCount: number;
  totalMaterialsUploaded: number;
  totalRetrospectives: number;
  joinedAt: string; // ISO 8601
}
```

### DailyContribution (일일 기여도)
```typescript
interface DailyContribution {
  date: string; // yyyy-MM-dd
  hasActivity: boolean;
  activityCount: number;
}
```

### ContributionDetail (활동 상세)
```typescript
interface ContributionDetail {
  type: 'STUDY_ATTENDANCE' | 'QUIZ_SOLVED' | 'MATERIAL_UPLOAD' | 'RETROSPECTIVE';
  referenceId: number;
  referenceName: string;
  createdAt: string; // ISO 8601
}
```

### Badge (뱃지)
```typescript
interface Badge {
  id: number;
  code: string;              // 뱃지 코드 (예: FIRST_ACTIVITY)
  name: string;
  description: string;
  icon: string;              // 이모지 또는 아이콘
  isEarned: boolean;
  earnedAt?: string;         // ISO 8601
  progress?: {
    current: number;
    required: number;
    percentage: number;
  };
}

interface BadgeCategory {
  category: string;          // ACTIVITY, STREAK, QUIZ 등
  categoryName: string;      // 한글명
  badges: Badge[];
}
```

### Penalty (패널티)
```typescript
interface Penalty {
  id: number;
  penaltyType: string;
  name: string;
  description: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  grantedAt: string; // ISO 8601
  expiresAt?: string; // ISO 8601
  removedAt?: string; // ISO 8601
  studyId?: number;
  studyName?: string;
  isActive: boolean;
  removalCondition?: string;
  removalProgress?: {
    current: number;
    required: number;
  };
}
```
s
---

## 🧪 테스트 가이드

상세한 테스트 가이드는 별도 문서를 참고하세요:
- **파일**: `GAMIFICATION_TEST_GUIDE.md`
- **내용**: 전체 API 테스트 시나리오, Postman 컬렉션, 샘플 데이터 생성 SQL

---

## 에러 코드

| HTTP | 코드 | 설명 | 처리 방법 |
|------|------|------|----------|
| 400 | `INVALID_DATE` | 유효하지 않은 날짜 | 날짜 형식 확인 (yyyy-MM-dd) |
| 400 | `INVALID_ARGUMENT` | 잘못된 파라미터 | 필수 파라미터 확인 |
| 403 | `FORBIDDEN` | 권한 없음 | 인증 토큰 확인 |
| 404 | `STUDY_NOT_FOUND` | 스터디를 찾을 수 없음 | studyId 확인 |
| 404 | `USER_NOT_FOUND` | 사용자를 찾을 수 없음 | userId 확인 |
| 403 | `NOT_STUDY_MEMBER` | 스터디 멤버가 아님 | 스터디 가입 상태 확인 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 | 백엔드팀에 문의 |

### 에러 응답 형식
```json
{
  "status": 400,
  "code": "INVALID_ARGUMENT",
  "message": "사용자를 찾을 수 없습니다.",
  "timestamp": "2026-01-26T21:18:35.402207900"
}
```

---

**마지막 업데이트**: 2026-01-26  