# 연속 학습 모드 API (Continuous Learning Mode)

## 기본 정보
- Base URL: `/api/v1/continuous-quiz`
- 인증: JWT 필수
- 별칭: 말해보카 방식 (Sayvoca Style)

### 핵심 개념

**연속 학습 모드**는 기존 퀴즈 코스와 달리 **문제를 하나씩 연속으로 학습**하는 방식입니다.

- 섹션 클릭 시 즉시 첫 문제 노출
- 답변 제출과 동시에 다음 문제 반환 (Atomic API)
- FSRS 알고리즘 기반 복습 일정 자동 관리
- `user_section_attempt` 테이블 미사용 (경량 학습)

### 무한 반복 학습 (Infinite Loop)

> **섹션 완료 개념 없음** — 동일 문제가 반복 출제될 수 있습니다.

연속 학습 모드는 **반복 강화 학습**을 위해 설계되었습니다:
- 100번 푼 문제도 다시 출제될 수 있음
- 섹션에 문제가 있는 한, 다음 문제는 **항상 존재**
- 학습 종료는 사용자가 직접 결정 (뒤로가기, 다른 섹션 선택 등)

---

## 확률적 가중치 선택 (Probabilistic Weighted Selection)

문제 선택은 **엄격한 우선순위가 아닌 확률적 가중치** 기반으로 동작합니다.

### 가중치 로직

| 조건 | 가중치 | 설명 |
|------|--------|------|
| 신규 문제 (`uri.id IS NULL`) | **10.0** | 한 번도 풀지 않은 문제 (가장 높음) |
| 복습 필요 (`next_review_at <= NOW()`) | **5.0** | 복습 시점 도래 문제 (Due) |
| 학습 완료 | **1 / (reps + 1)** | 반복 횟수에 따라 감소 |

### 선택 원리

```
확률 ∝ 가중치
```

- **지수 분포 랜덤** 공식 사용: `ORDER BY -LOG(1 - RAND()) / weight`
- 가중치가 높을수록 선택 확률이 **높지만 보장되지 않음**
- 모든 문제가 선택될 수 있음 (완전 배제 없음)

### 예시

| 문제 | 상태 | 가중치 | 선택 확률 (상대적) |
|------|------|--------|-------------------|
| Q1 | 신규 | 10.0 | 높음 |
| Q2 | Due | 5.0 | 중간 |
| Q3 | 1번 품 | 0.5 | 낮음 |
| Q4 | 10번 품 | 0.09 | 매우 낮음 |

> Q4도 여전히 선택될 수 있습니다. 단지 확률이 낮을 뿐입니다.

---

## 엔드포인트 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/courses/{courseId}/sections/{sectionNumber}/next` | 다음 문제 조회 | O |
| POST | `/questions/{questionId}/submit` | 정답 제출 및 다음 문제 조회 | O |

---

## API 상세

### 1. 다음 문제 조회

특정 섹션의 다음 학습 문제를 하나 가져옵니다. 섹션 학습 시작 시 최초 호출에 사용합니다.

**Request**
```
GET /api/v1/continuous-quiz/courses/{courseId}/sections/{sectionNumber}/next
Authorization: Bearer {accessToken}
```

**Path Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| courseId | Long | O | 코스 ID |
| sectionNumber | Integer | O | 섹션 번호 |

**Response - 성공**
```json
{
  "success": true,
  "data": {
    "questionId": 101,
    "questionNumber": 1,
    "questionText": "Java에서 정수형 변수를 선언할 때 사용하는 키워드는?",
    "questionType": "MULTIPLE_CHOICE",
    "options": "[{\"id\":\"A\",\"text\":\"integer\"},{\"id\":\"B\",\"text\":\"int\"},{\"id\":\"C\",\"text\":\"num\"},{\"id\":\"D\",\"text\":\"number\"}]",
    "courseId": 1,
    "sectionNumber": 1
  }
}
```

**Response Fields**

| 필드 | 타입 | 설명 |
|------|------|------|
| questionId | Long | 문제 고유 ID |
| questionNumber | Integer | 섹션 내 문제 번호 |
| questionText | String | 문제 텍스트 |
| questionType | String | 문제 유형 (`MULTIPLE_CHOICE`, `TRUE_FALSE`, `SHORT_ANSWER`) |
| options | String | 선택지 JSON (객관식의 경우) |
| courseId | Long | 코스 ID |
| sectionNumber | Integer | 섹션 번호 |

**Response - 섹션에 문제 없음**
```json
{
  "success": false,
  "error": {
    "code": "NO_QUESTIONS_AVAILABLE",
    "message": "해당 섹션에 문제가 없습니다."
  }
}
```

---

### 2. 정답 제출 및 다음 문제 조회 (Atomic API)

현재 문제의 정답을 제출하고 FSRS 상태를 업데이트한 뒤, 다음 문제를 즉시 반환합니다.

**핵심 특징:**
- 단일 API 호출로 제출 + 다음 문제 조회 처리
- FSRS 알고리즘 기반 복습 일정 자동 계산
- 응답 시간 기반 Rating 자동 산출
- **다음 문제는 항상 존재** (무한 루프)

**Request**
```
POST /api/v1/continuous-quiz/questions/{questionId}/submit
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Path Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| questionId | Long | O | 현재 문제 ID |

**Request Body**
```json
{
  "userAnswer": "B",
  "responseTimeMs": 3500
}
```

**Request Body Fields**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userAnswer | String | O | 사용자 답변 |
| responseTimeMs | Long | O | 응답 시간 (밀리초) - FSRS Rating 산출에 사용 |

**Response - 성공**
```json
{
  "success": true,
  "data": {
    "submittedQuestionId": 101,
    "isCorrect": true,
    "userAnswer": "B",
    "correctAnswer": "B",
    "explanation": "Java에서 정수형은 int 키워드를 사용합니다.",
    "stability": 4.93,
    "difficulty": 5.0,
    "scheduledDays": 1,
    "nextReviewAt": "2025-01-21T10:00:00",
    "state": 2,
    "reps": 1,
    "lapses": 0,
    "nextQuestion": {
      "questionId": 102,
      "questionNumber": 2,
      "questionText": "Java에서 문자열을 저장하는 클래스는?",
      "questionType": "MULTIPLE_CHOICE",
      "options": "[{\"id\":\"A\",\"text\":\"string\"},{\"id\":\"B\",\"text\":\"String\"},{\"id\":\"C\",\"text\":\"char\"},{\"id\":\"D\",\"text\":\"Text\"}]",
      "courseId": 1,
      "sectionNumber": 1
    }
  }
}
```

**Response - 동일 문제 재출제 (섹션에 문제가 1개인 경우)**
```json
{
  "success": true,
  "data": {
    "submittedQuestionId": 101,
    "isCorrect": true,
    "userAnswer": "B",
    "correctAnswer": "B",
    "explanation": "Java에서 정수형은 int 키워드를 사용합니다.",
    "stability": 6.81,
    "difficulty": 4.5,
    "scheduledDays": 3,
    "nextReviewAt": "2025-01-23T10:00:00",
    "state": 2,
    "reps": 2,
    "lapses": 0,
    "nextQuestion": {
      "questionId": 101,
      "questionNumber": 1,
      "questionText": "Java에서 정수형 변수를 선언할 때 사용하는 키워드는?",
      "questionType": "MULTIPLE_CHOICE",
      "options": "[{\"id\":\"A\",\"text\":\"integer\"},{\"id\":\"B\",\"text\":\"int\"},{\"id\":\"C\",\"text\":\"num\"},{\"id\":\"D\",\"text\":\"number\"}]",
      "courseId": 1,
      "sectionNumber": 1
    }
  }
}
```

**Response Fields - Submission Result**

| 필드 | 타입 | 설명 |
|------|------|------|
| submittedQuestionId | Long | 제출된 문제 ID |
| isCorrect | Boolean | 정답 여부 |
| userAnswer | String | 사용자 제출 답변 |
| correctAnswer | String | 정답 |
| explanation | String | 문제 해설 |

**Response Fields - FSRS Update**

| 필드 | 타입 | 설명 |
|------|------|------|
| stability | Double | FSRS 안정성 (높을수록 오래 기억) |
| difficulty | Double | FSRS 난이도 (1.0 ~ 10.0) |
| scheduledDays | Integer | 다음 복습까지 남은 일수 |
| nextReviewAt | LocalDateTime | 다음 복습 예정 시각 |
| state | Integer | FSRS 상태 (0: New, 1: Learning, 2: Review, 3: Relearning) |
| reps | Integer | 총 복습 횟수 |
| lapses | Integer | 망각 횟수 (틀린 횟수) |

**Response Fields - Next Question**

| 필드 | 타입 | 설명 |
|------|------|------|
| nextQuestion | Object | 다음 문제 정보 (**항상 존재**, 섹션에 문제가 없을 때만 `null`) |
| nextQuestion.questionId | Long | 다음 문제 ID (방금 푼 문제와 동일할 수 있음) |
| nextQuestion.questionNumber | Integer | 섹션 내 문제 번호 |
| nextQuestion.questionText | String | 문제 텍스트 |
| nextQuestion.questionType | String | 문제 유형 |
| nextQuestion.options | String | 선택지 JSON |
| nextQuestion.courseId | Long | 코스 ID |
| nextQuestion.sectionNumber | Integer | 섹션 번호 |

> **Note:** `sectionCompleted` 필드는 제거되었습니다. 무한 루프 지원으로 섹션 완료 개념이 없습니다.

---

## FSRS Rating 자동 산출 로직

응답 시간(`responseTimeMs`)을 기반으로 FSRS Rating이 자동 산출됩니다.

### Rating 산출 기준

| 응답 시간 | Rating | 설명 |
|----------|--------|------|
| ≤ 2,000ms (2초 이하) | **Easy (4)** | 빠르게 정답 → 쉬웠음 |
| 2,001ms ~ 5,000ms | **Good (3)** | 적당한 시간 → 보통 |
| > 5,000ms (5초 초과) | **Hard (2)** | 오래 고민 → 어려웠음 |

### 정답/오답에 따른 적용

| 정답 여부 | Rating 적용 |
|----------|-------------|
| 정답 (Correct) | 응답 시간 기반 Rating 사용 (Easy/Good/Hard) |
| 오답 (Incorrect) | 강제 **Again (1)** 적용 (응답 시간 무관) |

### FSRS State 설명

| State | 값 | 설명 |
|-------|---|------|
| New | 0 | 신규 카드 (처음 학습) |
| Learning | 1 | 학습 중 (단기 반복) |
| Review | 2 | 복습 단계 (장기 기억) |
| Relearning | 3 | 재학습 (망각 후 복구) |

---

## 학습 플로우

```
┌─────────────────────────────────────────────────────────────┐
│              Continuous Learning Flow (Infinite Loop)        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 섹션 선택                                                │
│     └─▶ GET /courses/{courseId}/sections/{sectionNumber}/next │
│                                                             │
│  2. 문제 표시                                                │
│     └─▶ Response: questionId, questionText, options          │
│                                                             │
│  3. 사용자 답변 + 타이머 측정                                 │
│     └─▶ userAnswer, responseTimeMs 수집                      │
│                                                             │
│  4. 제출 (Atomic)                                           │
│     └─▶ POST /questions/{questionId}/submit                  │
│                                                             │
│  5. 결과 + 다음 문제 수신                                    │
│     ├─▶ 정답/오답 피드백                                     │
│     ├─▶ FSRS 업데이트 정보                                   │
│     └─▶ 다음 문제 (항상 존재, 확률적 선택)                    │
│                                                             │
│  6. 무한 반복 (4~5) - 사용자가 종료할 때까지                  │
│     └─▶ 뒤로가기, 홈 버튼, 다른 섹션 선택 등으로 종료         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 설계 철학

### 왜 확률적 선택인가?

1. **망각 곡선 대응**: FSRS는 완벽한 예측이 아닙니다. 복습 시점이 아닌 문제도 간헐적 노출이 기억 유지에 도움됩니다.

2. **학습 다양성**: 엄격한 우선순위는 특정 문제만 반복 노출시켜 학습 피로를 유발합니다.

3. **연속 학습 특성**: 짧은 세션에서 다양한 문제를 경험하는 것이 효과적입니다.

### 왜 무한 루프인가?

1. **사용자 자율성**: 학습 종료 시점은 사용자가 결정합니다.

2. **반복 강화**: 같은 문제를 여러 번 풀어도 FSRS가 기록을 누적합니다.

3. **심플한 UX**: "섹션 완료" 팝업 없이 자연스러운 학습 흐름 유지.

---

## 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| QUESTION_NOT_FOUND | 404 | 문제를 찾을 수 없음 |
| NO_QUESTIONS_AVAILABLE | 404 | 섹션에 문제가 없음 |
| COURSE_NOT_FOUND | 404 | 코스를 찾을 수 없음 |
| INVALID_ANSWER | 400 | 답변 형식 오류 |
| UNAUTHORIZED | 401 | 인증 필요 |

---

## 프론트엔드 연동 예시

### React Hook 예시

```tsx
// useContinuousQuiz.ts
import { useState, useCallback, useRef } from 'react';
import { api } from '@/api/axios';

interface Question {
  questionId: number;
  questionNumber: number;
  questionText: string;
  questionType: string;
  options: string;
  courseId: number;
  sectionNumber: number;
}

interface SubmitResult {
  submittedQuestionId: number;
  isCorrect: boolean;
  userAnswer: string;
  correctAnswer: string;
  explanation: string;
  stability: number;
  difficulty: number;
  scheduledDays: number;
  nextReviewAt: string;
  state: number;
  reps: number;
  lapses: number;
  nextQuestion: Question | null;
}

export const useContinuousQuiz = (courseId: number, sectionNumber: number) => {
  const [question, setQuestion] = useState<Question | null>(null);
  const [result, setResult] = useState<SubmitResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const startTimeRef = useRef<number>(0);

  // 첫 문제 로드
  const startSection = useCallback(async () => {
    setIsLoading(true);
    try {
      const { data } = await api.get(
        `/continuous-quiz/courses/${courseId}/sections/${sectionNumber}/next`
      );
      setQuestion(data.data);
      startTimeRef.current = Date.now();
    } finally {
      setIsLoading(false);
    }
  }, [courseId, sectionNumber]);

  // 답변 제출 (무한 루프 - 항상 다음 문제 존재)
  const submitAnswer = useCallback(async (userAnswer: string) => {
    if (!question) return;

    setIsLoading(true);
    const responseTimeMs = Date.now() - startTimeRef.current;

    try {
      const { data } = await api.post(
        `/continuous-quiz/questions/${question.questionId}/submit`,
        { userAnswer, responseTimeMs }
      );

      setResult(data.data);

      // 다음 문제로 자동 전환 (항상 존재)
      if (data.data.nextQuestion) {
        setQuestion(data.data.nextQuestion);
        startTimeRef.current = Date.now();
      }
    } finally {
      setIsLoading(false);
    }
  }, [question]);

  // 결과 확인 후 다음 문제로 진행
  const continueToNext = useCallback(() => {
    setResult(null); // 결과 팝업 닫기
  }, []);

  return {
    question,
    result,
    isLoading,
    startSection,
    submitAnswer,
    continueToNext,
  };
};
```

### 컴포넌트 사용 예시

```tsx
// ContinuousQuizPage.tsx
const ContinuousQuizPage = ({ courseId, sectionNumber }) => {
  const {
    question,
    result,
    isLoading,
    startSection,
    submitAnswer,
    continueToNext,
  } = useContinuousQuiz(courseId, sectionNumber);

  useEffect(() => {
    startSection();
  }, [startSection]);

  // 결과 팝업 표시
  if (result) {
    return (
      <ResultPopup
        isCorrect={result.isCorrect}
        explanation={result.explanation}
        onContinue={continueToNext}
      />
    );
  }

  // 문제 표시
  if (question) {
    return (
      <QuestionCard
        question={question}
        onSubmit={submitAnswer}
        isLoading={isLoading}
      />
    );
  }

  return <Loading />;
};
```

---

## 관련 문서

- [퀴즈 코스 API](./API_21_퀴즈코스.md) - 섹션 기반 퀴즈 시스템 (시도/채점 방식)
- FSRS Algorithm - [open-spaced-repetition/fsrs4anki](https://github.com/open-spaced-repetition/fsrs4anki)
