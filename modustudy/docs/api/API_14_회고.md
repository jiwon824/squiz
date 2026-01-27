# 회고 API (Retrospective)

## 기본 정보
- Base URL: `/api/v1/studies/{studyId}/retrospectives`
- 인증: Header에 `User-Id` 필요 (JWT 적용 전)

---

## 엔드포인트 목록

| Method | Endpoint | 설명 | 구현 상태 |
|--------|----------|------|----------|
| GET | `/` | 회고 목록 조회 | ✅ 완료 |
| GET | `/{retroId}` | 회고 상세 조회 | ✅ 완료 |
| POST | `/` | 회고 생성 (스터디 멤버) | ✅ 완료 |
| DELETE | `/{retroId}` | 회고 삭제 (스터디장/생성자) | ✅ 완료 |
| POST | `/{retroId}/items` | 회고 항목 작성 | ❌ 미구현 |
| PUT | `/{retroId}/items/{itemId}` | 회고 항목 수정 | ❌ 미구현 |
| DELETE | `/{retroId}/items/{itemId}` | 회고 항목 삭제 | ❌ 미구현 |
| GET | `/{retroId}/analysis` | AI 회고 분석 | ❌ 미구현 (제외) |

---

## API 상세

### 1. 회고 목록 조회

**Request**
```
GET /api/v1/studies/{studyId}/retrospectives?page=0&size=20
User-Id: {userId}
```

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 3,
      "title": "중간 점검 자유 회고",
      "retrospectiveType": "FREE",
      "session": null,
      "itemCount": 2,
      "participantCount": 2,
      "hasMyItem": false,
      "createdAt": "2025-01-20T15:00:00"
    },
    {
      "id": 2,
      "title": "2회차 그래프 탐색 회고",
      "retrospectiveType": "KPT",
      "session": {
        "id": 2,
        "sessionNumber": 2,
        "title": "2회차: 그래프 탐색"
      },
      "itemCount": 4,
      "participantCount": 2,
      "hasMyItem": true,
      "createdAt": "2025-01-17T21:00:00"
    },
    {
      "id": 1,
      "title": "1회차 DP 기초 회고",
      "retrospectiveType": "KPT",
      "session": {
        "id": 1,
        "sessionNumber": 1,
        "title": "1회차: DP 기초"
      },
      "itemCount": 9,
      "participantCount": 3,
      "hasMyItem": true,
      "createdAt": "2025-01-10T21:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": false }
  },
  "totalElements": 3,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

**응답 필드 설명**

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 회고 ID |
| title | String | 회고 제목 |
| retrospectiveType | String | KPT / FREE |
| session | Object | 연결된 세션 정보 (없으면 null) |
| itemCount | Integer | 전체 항목 수 |
| participantCount | Integer | 참여자 수 |
| hasMyItem | Boolean | 내 항목 존재 여부 |
| createdAt | DateTime | 생성일시 |

**정렬**: 최신순 (createdAt DESC, id DESC)

---

### 2. 회고 상세 조회

**Request**
```
GET /api/v1/studies/{studyId}/retrospectives/{retroId}
```

**Response** `200 OK`
```json
{
  "id": 1,
  "title": "1회차 DP 기초 회고",
  "retrospectiveType": "KPT",
  "session": {
    "id": 1,
    "sessionNumber": 1,
    "title": "1회차: DP 기초"
  },
  "items": {
    "KEEP": [
      {
        "id": 1,
        "user": {
          "id": 1,
          "nickname": "스터디장",
          "profileImage": null
        },
        "content": "매일 한 문제씩 꾸준히 풀기를 잘 지켰다",
        "createdAt": "2025-01-27T15:30:00"
      },
      {
        "id": 4,
        "user": {
          "id": 2,
          "nickname": "멤버1",
          "profileImage": null
        },
        "content": "서로 코드 리뷰하면서 배우는 점이 많았다",
        "createdAt": "2025-01-27T15:30:01"
      },
      {
        "id": 7,
        "user": {
          "id": 3,
          "nickname": "멤버2",
          "profileImage": null
        },
        "content": "온라인이라 참여하기 편했다",
        "createdAt": "2025-01-27T15:30:02"
      }
    ],
    "PROBLEM": [
      {
        "id": 2,
        "user": {
          "id": 1,
          "nickname": "스터디장",
          "profileImage": null
        },
        "content": "DP 점화식을 세우는 게 아직 어렵다",
        "createdAt": "2025-01-27T15:30:00"
      },
      {
        "id": 5,
        "user": {
          "id": 2,
          "nickname": "멤버1",
          "profileImage": null
        },
        "content": "시간 복잡도 계산이 헷갈린다",
        "createdAt": "2025-01-27T15:30:01"
      },
      {
        "id": 8,
        "user": {
          "id": 3,
          "nickname": "멤버2",
          "profileImage": null
        },
        "content": "문제 풀이 속도가 느리다",
        "createdAt": "2025-01-27T15:30:02"
      }
    ],
    "TRY": [
      {
        "id": 3,
        "user": {
          "id": 1,
          "nickname": "스터디장",
          "profileImage": null
        },
        "content": "다음 주는 백준 DP 태그 문제 5개 더 풀기",
        "createdAt": "2025-01-27T15:30:00"
      },
      {
        "id": 6,
        "user": {
          "id": 2,
          "nickname": "멤버1",
          "profileImage": null
        },
        "content": "풀이 전 시간 복잡도 먼저 계산하는 습관 들이기",
        "createdAt": "2025-01-27T15:30:01"
      },
      {
        "id": 9,
        "user": {
          "id": 3,
          "nickname": "멤버2",
          "profileImage": null
        },
        "content": "타이머 설정해서 제한 시간 내 풀기 연습",
        "createdAt": "2025-01-27T15:30:02"
      }
    ]
  },
  "createdAt": "2025-01-10T21:00:00"
}
```

**응답 필드 설명**

| 필드 | 타입 | 설명 |
|------|------|------|
| items | Map<Category, List> | 카테고리별 항목 (KEEP, PROBLEM, TRY) |
| items[].user | Object | 작성자 정보 |
| items[].content | String | 항목 내용 |

---

### 3. 회고 생성

**Request**
```
POST /api/v1/studies/{studyId}/retrospectives
User-Id: {userId}
Content-Type: application/json
```
```json
{
  "title": "새로운 회고",
  "retrospectiveType": "KPT",
  "sessionId": 3
}
```

| Field | Type | 필수 | 설명 |
|-------|------|------|------|
| title | String | O | 회고 제목 (최대 200자) |
| retrospectiveType | String | X | KPT / FREE (기본값: KPT) |
| sessionId | Long | X | 연결할 세션 ID |

**Response** `201 Created`
```json
{
  "id": 4,
  "title": "새로운 회고",
  "retrospectiveType": "KPT",
  "session": {
    "id": 3,
    "sessionNumber": 3,
    "title": "3회차: 이분 탐색"
  },
  "items": {},
  "createdAt": "2025-01-27T15:45:00"
}
```

**에러 응답**

| 상황 | Status | Code |
|------|--------|------|
| 스터디 없음 | 404 | STUDY_NOT_FOUND |
| 세션 없음 | 400 | INVALID_RETROSPECTIVE_REQUEST |

---

### 4. 회고 삭제

**Request**
```
DELETE /api/v1/studies/{studyId}/retrospectives/{retroId}
User-Id: {userId}
```

**권한**: 스터디장 또는 회고 생성자만 삭제 가능

**Response** `204 No Content`

**에러 응답**

| 상황 | Status | Code |
|------|--------|------|
| 스터디 없음 | 404 | STUDY_NOT_FOUND |
| 회고 없음 | 404 | RETROSPECTIVE_NOT_FOUND |
| 권한 없음 | 403 | NOT_RETROSPECTIVE_OWNER |

---

## 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| STUDY_NOT_FOUND | 404 | 스터디를 찾을 수 없음 |
| RETROSPECTIVE_NOT_FOUND | 404 | 회고를 찾을 수 없음 |
| NOT_RETROSPECTIVE_OWNER | 403 | 회고 삭제 권한 없음 |
| INVALID_RETROSPECTIVE_REQUEST | 400 | 잘못된 요청 (세션 없음 등) |

---

## 포스트맨 테스트

### 테스트 데이터 (SQL)
```sql
-- User
INSERT INTO `user` (user_id, email, nickname, name, role, is_active, is_online, is_searchable, total_exp, current_points, current_level, level_name, created_at)
VALUES 
('leader001', 'leader@test.com', '스터디장', '김리더', 'USER', true, false, true, 1000, 500, 5, 'Silver', NOW()),
('member001', 'member1@test.com', '멤버1', '이멤버', 'USER', true, false, true, 500, 200, 3, 'Bronze', NOW()),
('member002', 'member2@test.com', '멤버2', '박멤버', 'USER', true, false, true, 300, 100, 2, 'Bronze', NOW());

-- Topic & Format
INSERT INTO `topic` (name, sort_order, icon) VALUES ('알고리즘', 1, '🔢');
INSERT INTO `format` (name, sort_order, description) VALUES ('문제 풀이', 1, '알고리즘 문제를 함께 풀이');

-- Study
INSERT INTO `study` (leader_id, name, description, topic_id, format_id, study_type, meeting_type, status, max_members, start_date, end_date, extension_count, is_public, created_at)
VALUES (1, '알고리즘 마스터 스터디', '백준 골드 문제를 함께 풀어봐요!', 1, 1, 'PLANNED', 'ONLINE', 'IN_PROGRESS', 6, '2025-01-01', '2025-06-30', 0, true, NOW());

-- StudySession
INSERT INTO `study_session` (study_id, session_number, title, scheduled_at, status, created_at)
VALUES 
(1, 1, '1회차: DP 기초', '2025-01-10 19:00:00', 'COMPLETED', NOW()),
(1, 2, '2회차: 그래프 탐색', '2025-01-17 19:00:00', 'COMPLETED', NOW()),
(1, 3, '3회차: 이분 탐색', '2025-01-24 19:00:00', 'SCHEDULED', NOW());

-- Retrospective
INSERT INTO `retrospective` (study_id, session_id, created_by, title, retrospective_type, created_at)
VALUES 
(1, 1, 1, '1회차 DP 기초 회고', 'KPT', '2025-01-10 21:00:00'),
(1, 2, 1, '2회차 그래프 탐색 회고', 'KPT', '2025-01-17 21:00:00'),
(1, NULL, 2, '중간 점검 자유 회고', 'FREE', '2025-01-20 15:00:00');

-- RetrospectiveItem (회고 1번)
INSERT INTO `retrospective_item` (retrospective_id, user_id, category, content, created_at)
VALUES 
(1, 1, 'KEEP', '매일 한 문제씩 꾸준히 풀기를 잘 지켰다', NOW()),
(1, 1, 'PROBLEM', 'DP 점화식을 세우는 게 아직 어렵다', NOW()),
(1, 1, 'TRY', '다음 주는 백준 DP 태그 문제 5개 더 풀기', NOW()),
(1, 2, 'KEEP', '서로 코드 리뷰하면서 배우는 점이 많았다', NOW()),
(1, 2, 'PROBLEM', '시간 복잡도 계산이 헷갈린다', NOW()),
(1, 2, 'TRY', '풀이 전 시간 복잡도 먼저 계산하는 습관 들이기', NOW()),
(1, 3, 'KEEP', '온라인이라 참여하기 편했다', NOW()),
(1, 3, 'PROBLEM', '문제 풀이 속도가 느리다', NOW()),
(1, 3, 'TRY', '타이머 설정해서 제한 시간 내 풀기 연습', NOW());
```

### 테스트 시나리오

| # | 테스트 | Method | URL | Header | Expected |
|---|--------|--------|-----|--------|----------|
| 1 | 목록 조회 | GET | `/api/v1/studies/1/retrospectives` | User-Id: 1 | 200 OK |
| 2 | 상세 조회 | GET | `/api/v1/studies/1/retrospectives/1` | - | 200 OK |
| 3 | 생성 (세션 포함) | POST | `/api/v1/studies/1/retrospectives` | User-Id: 1 | 201 Created |
| 4 | 생성 (세션 없이) | POST | `/api/v1/studies/1/retrospectives` | User-Id: 2 | 201 Created |
| 5 | 삭제 (생성자) | DELETE | `/api/v1/studies/1/retrospectives/3` | User-Id: 2 | 204 No Content |
| 6 | 삭제 (스터디장) | DELETE | `/api/v1/studies/1/retrospectives/2` | User-Id: 1 | 204 No Content |
| 7 | 삭제 실패 | DELETE | `/api/v1/studies/1/retrospectives/1` | User-Id: 3 | 403 Forbidden |

### 생성 Request Body 예시
```json
// 세션 포함
{
  "title": "새로운 회고",
  "retrospectiveType": "KPT",
  "sessionId": 3
}

// 세션 없이
{
  "title": "자유 형식 회고",
  "retrospectiveType": "FREE"
}
```

---

## 미구현 기능

| 기능 | 설명 | 비고 |
|------|------|------|
| 회고 항목 CRUD | 개별 항목 작성/수정/삭제 | 다음 스프린트 |
| AI 회고 분석 | 패턴 분석, 추천 | 제외됨 |

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2025-01-27 | 초기 구현 완료 (목록, 상세, 생성, 삭제) |