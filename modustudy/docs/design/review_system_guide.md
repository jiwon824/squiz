# 복습 시스템 (FSRS) 통합 가이드

본 문서는 ModuStudy 프로젝트의 복습 시스템 로직과 스터디 퀴즈(Study Quiz) 연동을 위한 가이드라인을 정의합니다.

## 1. 개요
본 시스템은 **FSRS v14 (Free Spaced Repetition Scheduler)** 알고리즘을 기반으로 사용자의 학습 상태(안정도, 난이도)를 분석하여 최적의 복습 시점을 제안합니다.
모든 학습 기록은 `FsrsService`를 통해 중앙에서 관리되며, 각 도메인(코스 퀴즈, 스터디 퀴즈)은 해당 서비스를 호출하여 학습 이력을 저장해야 합니다.

## 2. 데이터 모델

### 2.1 UserReviewItem (복습 상태)
사용자별, 콘텐츠별 현재 학습 상태를 저장하는 테이블입니다.
- **pk**: `id`
- **uk**: `user_id`, `content_type`, `content_id`
- **핵심 필드**:
  - `stability` (안정도): 기억이 유지되는 기간 (일 단위, 내부 계산용)
  - `difficulty` (난이도): 1(EASY) ~ 10(HARD)
  - `scheduled_minutes` (다음 복습 간격): 분 단위
  - `next_review_at` (다음 복습 시점)
  - `state` (학습 단계): New(0), Learning(1), Review(2), Relearning(3)

### 2.2 UserReviewLog (복습 이력)
복습이 발생할 때마다 생성되는 불변(Immutable) 로그입니다.
- **fk**: `review_item_id`
- **핵심 필드**:
  - `is_correct`: 정답 여부
  - `response_time_ms`: 응답 시간
  - `reviewed_at`: 복습 시점

## 3. FSRS 로직 상세

### 3.1 등급(Rating) 산정 로직 상세

본 시스템은 학습자의 주관적인 판단(버튼 클릭)을 배제하고, 객관적인 지표인 **정답 여부**와 **응답 소요 시간**을 기반으로 FSRS 등급을 자동 산출합니다.

#### 자동 산정 기준
응답 시간 임계값은 인간의 인지 처리 속도와 단기 기억 인출 시간을 고려하여 설정되었습니다.

| 등급 (Rating) | FSRS 의미 | 부여 조건 | 비고 |
|:---:|:---:|:---|:---|
| **1 (Again)** | **완전 망각** | **오답인 경우** | 즉시 재학습(Relearning) 단계로 진입 |
| **2 (Hard)** | **어려움** | 정답이면서 **5초(5000ms) 초과** 소요 | 기억 인출에 상당한 인지 부하가 발생함 |
| **3 (Good)** | **적절함** | 정답이면서 **2초 초과 ~ 5초 이하** 소요 | 일반적인 기억 인출 속도 |
| **4 (Easy)** | **쉬움** | 정답이면서 **2초(2000ms) 이하** 소요 | 즉각적인 인출(Recall)이 일어남 (반사적 기억) |

> **참고**: 해당 임계값(5s, 2s)은 `FsrsConstants.java`에 정의되어 있으며, 향후 학습 데이터가 축적됨에 따라 최적화될 수 있습니다.

#### 등급별 처리 흐름
1. **사용자 답안 제출** → 정답 여부 판별 (`isCorrect`)
2. `isCorrect == false` → 무조건 **Rating 1 (Again)** 부여
3. `isCorrect == true` → 응답 시간(`responseTimeMs`) 측정
   - `> 5000ms` → **Rating 2 (Hard)**
   - `> 2000ms` → **Rating 3 (Good)**
   - `<= 2000ms` → **Rating 4 (Easy)**
4. 산출된 Rating을 FSRS 알고리즘에 입력하여 다음 복습 시점 계산

### 3.2 상태 갱신 프로세스
1. 학습 결과(정답 여부, 응답 시간) 수신
2. 등급(Rating) 산출
3. FSRS 알고리즘에 따른 Stability, Difficulty 재계산
4. `scheduled_minutes` 계산 (Stability 기반, 최소 5분)
5. `next_review_at` 갱신 (현재 시간 + scheduled_minutes)

## 4. 스터디 퀴즈 연동 가이드

스터디 퀴즈 기능 구현 시 아래 절차를 준수하여 구현 바랍니다.

### 4.1 연동 규격
`FsrsService.processReview` 메서드를 호출하여 복습 처리를 위임합니다.

```java
// FsrsService.java

public UserReviewItem processReview(
    Long userId, 
    ReviewContentType contentType, // ReviewContentType.STUDY_QUESTION
    Long contentId,                // StudyQuizQuestion ID
    String userAnswer,             // 사용자 제출 답안
    long responseTimeMs            // 응답 소요 시간 (밀리초)
);
```

### 4.2 필수 구현 사항 (담당자)

1. **Repository 생성**
   - `StudyQuizQuestion` 엔티티 조회를 위한 `StudyQuizQuestionRepository` 인터페이스를 생성해야 합니다.

2. **FsrsService 로직 보완**
   - 현재 `FsrsService` 내 `STUDY_QUESTION` 처리 부분이 미구현 상태입니다.
   - `processReview` 메서드 내에서 `StudyQuizQuestionRepository`를 주입받아 정답을 조회하고 채점하는 로직을 추가해야 합니다.

   ```java
   // 예시 코드 (FsrsService.java)
   if (contentType == ReviewContentType.STUDY_QUESTION) {
       StudyQuizQuestion question = studyQuizQuestionRepository.findById(contentId)
               .orElseThrow(() -> new BusinessException(errorCode));
       
       isCorrect = QuizGradingUtils.grade(
           userAnswer, 
           question.getCorrectAnswer(), 
           question.getQuestionType(), 
           question.getOptions()
       );
   }
   ```

3. **API 엔드포인트**
   - 스터디 퀴즈 제출 API(`StudyQuizController`)에서 위 서비스 메서드를 호출하도록 구성하거나, 기존 `ReviewController`를 재사용할 수 있습니다.
   - 단, `ReviewController`는 범용 엔드포인트이므로 스터디 도메인 특화 로직(예: 스터디 내 랭킹 갱신 등)이 필요한 경우 전용 컨트롤러 사용을 권장합니다.
