# STT 리포트 프론트엔드 API 연동 가이드

## 개요

프론트엔드에서 STT 미팅 리포트 페이지의 API 연동 준비를 완료했습니다.
현재는 **Mock 데이터로 동작**하며, 백엔드 API가 준비되면 플래그 하나로 전환 가능합니다.

---

## 프론트엔드에서 사용하는 API 엔드포인트

STT 리포트 기능에서 호출하는 엔드포인트는 아래 **3개**입니다.

| # | 메서드 | URL | 용도 |
|---|--------|-----|------|
| 1 | `GET` | `/api/v1/studies/{studyId}/meetings` | 미팅 목록 조회 |
| 2 | `GET` | `/api/v1/studies/{studyId}/meetings/{meetingId}` | 미팅 상세 조회 |
| 3 | `GET` | `/api/v1/studies/{studyId}/meetings/{meetingId}/transcripts` | STT 트랜스크립트 조회 |

---

## 각 API 상세

### 1. 미팅 목록 조회

```
GET /api/v1/studies/{studyId}/meetings
```

**쿼리 파라미터** (선택):

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `page` | number | 페이지 번호 (0부터) |
| `size` | number | 페이지 크기 |
| `meetingType` | string | 필터: `DAILY`, `WEEKLY`, `FREE`, `OTHER` |
| `startDate` | string | 필터: 시작 날짜 (YYYY-MM-DD) |
| `endDate` | string | 필터: 종료 날짜 (YYYY-MM-DD) |

**프론트에서 사용하는 응답 필드**:

```json
{
  "content": [
    {
      "id": 1,
      "title": "주간 회의 - React Hooks 심화",
      "startedAt": "2026-01-25T14:00:00",
      "durationSeconds": 5400,
      "hasTranscript": true,
      "hasSummary": true
    }
  ],
  "totalPages": 1,
  "page": 0
}
```

> `hasTranscript` 값을 보고 트랜스크립트 API 호출 여부를 결정합니다.

---

### 2. 미팅 상세 조회

```
GET /api/v1/studies/{studyId}/meetings/{meetingId}
```

**프론트에서 사용하는 응답 필드**:

```json
{
  "id": 1,
  "title": "주간 회의 - React Hooks 심화",
  "startedAt": "2026-01-25T14:00:00",
  "durationSeconds": 5400,
  "participants": [
    { "userId": 1, "nickname": "김철수" },
    { "userId": 2, "nickname": "이영희" }
  ],
  "keywords": ["React Hooks", "useEffect"],
  "summary": {
    "summary": "useState와 useEffect의 실행 순서에 대해 논의했습니다.",
    "actionItems": [
      { "id": 1, "content": "custom hook 예제 작성해오기", "status": "TODO" }
    ],
    "keywords": ["React Hooks"],
    "status": "DONE"
  }
}
```

> `participants.nickname` → 화자 이름으로 사용됩니다.
> `summary`가 `null`이면 빈 요약으로 표시됩니다.

---

### 3. STT 트랜스크립트 조회

```
GET /api/v1/studies/{studyId}/meetings/{meetingId}/transcripts
```

**프론트에서 사용하는 응답 필드**:

```json
[
  {
    "id": 1,
    "user": { "id": 1, "nickname": "김철수" },
    "content": "오늘은 React Hooks 심화 내용을 다뤄보겠습니다.",
    "timestampSeconds": 0,
    "createdAt": "2026-01-25T14:00:00"
  },
  {
    "id": 2,
    "user": { "id": 2, "nickname": "이영희" },
    "content": "useEffect의 클린업 함수가 언제 실행되는지 모르겠어요.",
    "timestampSeconds": 150,
    "createdAt": "2026-01-25T14:02:30"
  }
]
```

> `timestampSeconds` → `MM:SS` 형식으로 변환됩니다 (예: 150초 → "02:30").
> `user`가 `null`이면 "알 수 없음"으로 표시됩니다.

---

## 데이터 변환 흐름

프론트엔드에서 백엔드 응답을 아래와 같이 변환합니다:

```
백엔드 응답                    →  프론트엔드 UI 타입
─────────────────────────────────────────────────────
MeetingDetailResponse          →  MeetingReport
├─ participants[].nickname     →  participants: string[]
├─ durationSeconds             →  duration: "1시간 30분"
├─ startedAt                   →  date: "2026-01-25"
├─ summary.summary             →  summary: string
├─ summary.actionItems[].content → actionItems: string[]
└─ keywords                    →  keywords: string[]

MeetingTranscriptItemResponse  →  TranscriptItem
├─ user.nickname               →  speaker: string
├─ timestampSeconds            →  time: "02:30"
└─ content                     →  text: string
```

---

## 확인 요청 사항

아래 사항을 확인해주세요:

1. **미팅 목록 API**: `hasTranscript`, `hasSummary` 필드가 정상 반환되는지
2. **트랜스크립트 API**: `user` 객체에 `nickname`이 포함되어 있는지 (`null` 가능)
3. **요약 API**: 미팅 상세 조회 시 `summary` 필드에 `actionItems`, `keywords`가 포함되는지
4. **정렬 순서**: 트랜스크립트가 `timestampSeconds` 기준 오름차순으로 반환되는지

---

## 관련 파일

| 파일 | 역할 |
|------|------|
| `frontend/src/features/meeting/services/meetingApi.ts` | API 호출 래퍼 |
| `frontend/src/features/meeting/types.ts` | 백엔드 DTO 타입 정의 |
| `frontend/src/store/sttStore.ts` | STT 상태 관리 (Zustand) |
| `frontend/src/features/dashboard-v2/pages/STTReportPage.tsx` | STT 리포트 페이지 |
