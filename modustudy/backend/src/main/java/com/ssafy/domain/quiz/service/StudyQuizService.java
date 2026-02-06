package com.ssafy.domain.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.gamification.event.QuizSolvedEvent;
import com.ssafy.domain.quiz.dto.response.StudyQuizSubmitResponse;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.StudyQuiz;
import com.ssafy.domain.quiz.entity.StudyQuizQuestion;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import com.ssafy.domain.quiz.repository.StudyQuizQuestionRepository;
import com.ssafy.domain.quiz.repository.StudyQuizRepository;
import com.ssafy.domain.quiz.util.QuizGradingUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;

/**
 * 스터디 퀴즈 서비스
 * 미팅 AI 처리 결과에서 퀴즈를 저장
 */
 @Service
 @RequiredArgsConstructor
 @Transactional(readOnly = true)
 public class StudyQuizService {

    private static final Logger log = LoggerFactory.getLogger(StudyQuizService.class);

    private final StudyQuizRepository studyQuizRepository;
    private final StudyQuizQuestionRepository studyQuizQuestionRepository;
    private final FsrsService fsrsService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 미팅 AI 처리 결과에서 퀴즈 저장
     *
     * @param studyId   스터디 ID
     * @param sessionId 세션(회차) ID
     * @param meetingId 미팅 ID
     * @param title     퀴즈 제목
     * @param quizRaw   AI 서버에서 반환한 퀴즈 JSON (raw string)
     * @return 저장된 StudyQuiz (null if parsing failed or no questions)
     */
    @Transactional
    public StudyQuiz saveQuizFromMeeting(Long studyId, Long sessionId, Long meetingId, String title, String quizRaw) {
        if (quizRaw == null || quizRaw.isBlank()) {
            return null;
        }

        // 이미 해당 미팅에서 생성된 퀴즈가 있는지 확인
        if (studyQuizRepository.existsBySourceTypeAndSourceId(StudyQuiz.SourceType.MEETING, meetingId)) {
            return studyQuizRepository.findBySourceTypeAndSourceId(StudyQuiz.SourceType.MEETING, meetingId)
                    .orElse(null);
        }

        try {
            // 퀴즈 파싱
            List<Map<String, Object>> questions = parseQuizJson(quizRaw);
            if (questions.isEmpty()) {
                return null;
            }

            // StudyQuiz 생성
            StudyQuiz quiz = StudyQuiz.builder()
                    .studyId(studyId)
                    .sessionId(sessionId)
                    .title(title)
                    .sourceType(StudyQuiz.SourceType.MEETING)
                    .sourceId(meetingId)
                    .status(StudyQuiz.StudyQuizStatus.ACTIVE)
                    .build();

            // 문제 추가
            for (Map<String, Object> q : questions) {
                StudyQuizQuestion question = createQuestionFromMap(q);
                quiz.addQuestion(question);
            }

            StudyQuiz saved = studyQuizRepository.save(quiz);
            return saved;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * AI 퀴즈 JSON 파싱
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseQuizJson(String quizRaw) {
        try {
            String jsonText = quizRaw;

            // markdown 코드블록 제거
            if (jsonText.contains("```json")) {
                jsonText = jsonText.split("```json")[1].split("```")[0].trim();
            } else if (jsonText.contains("```")) {
                jsonText = jsonText.split("```")[1].split("```")[0].trim();
            }

            // JSON 배열 시작 찾기
            int startIdx = jsonText.indexOf('[');
            int endIdx = jsonText.lastIndexOf(']');
            if (startIdx != -1 && endIdx > startIdx) {
                jsonText = jsonText.substring(startIdx, endIdx + 1);
            }

            return objectMapper.readValue(jsonText, new TypeReference<>() {});

        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Map에서 QuizQuestion 엔티티 생성
     */
    @SuppressWarnings("unchecked")
    private StudyQuizQuestion createQuestionFromMap(Map<String, Object> q) {
        String questionText = (String) q.get("question");
        String type = (String) q.getOrDefault("type", "객관식");
        String answer = (String) q.get("answer");
        String explanation = (String) q.get("explanation");

        // options 처리
        String optionsJson = null;
        Object optionsObj = q.get("options");
        if (optionsObj != null) {
            try {
                if (optionsObj instanceof List) {
                    optionsJson = objectMapper.writeValueAsString(optionsObj);
                } else if (optionsObj instanceof String) {
                    optionsJson = (String) optionsObj;
                }
            } catch (Exception e) {
}
        }

        // answer_keywords 처리 (서술형 채점용)
        String answerKeywordsJson = null;
        Object keywordsObj = q.get("answer_keywords");
        if (keywordsObj != null) {
            try {
                if (keywordsObj instanceof List) {
                    answerKeywordsJson = objectMapper.writeValueAsString(keywordsObj);
                } else if (keywordsObj instanceof String) {
                    answerKeywordsJson = (String) keywordsObj;
                }
            } catch (Exception e) {
}
        }

        // 타입 변환
        QuestionType questionType = QuestionType.MULTIPLE_CHOICE;
        if ("단답형".equals(type) || "SHORT_ANSWER".equalsIgnoreCase(type) || "서술형".equals(type)) {
            questionType = QuestionType.SHORT_ANSWER;
        }

        return StudyQuizQuestion.builder()
                .questionText(questionText)
                .questionType(questionType)
                .options(optionsJson)
                .correctAnswer(answer != null ? answer : "")
                .answerKeywords(answerKeywordsJson)
                .explanation(explanation)
                .build();
    }

    /**
     * 스터디 퀴즈 답안 제출 — 채점 + FSRS 복습 스케줄링
     *
     * @param userId         사용자 ID
     * @param questionId     문제 ID
     * @param userAnswer     사용자 답안
     * @param responseTimeMs 응답 시간 (밀리초)
     * @return 채점 결과 + FSRS 스케줄 응답
     */
    @Transactional
    public StudyQuizSubmitResponse submitAnswer(Long userId, Long questionId,
                                                 String userAnswer, long responseTimeMs) {
        StudyQuizQuestion question = studyQuizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다: " + questionId));

        // 채점
        boolean isCorrect = QuizGradingUtils.grade(userAnswer, question.getCorrectAnswer(),
                question.getQuestionType(), question.getOptions(), question.getAnswerKeywords());

        // FSRS 복습 스케줄링 (채점 결과 직접 전달)
        UserReviewItem item = fsrsService.processReviewResult(
                userId, ReviewContentType.STUDY_QUESTION, questionId, isCorrect, responseTimeMs);

        // 게이미피케이션 이벤트 발행
        publishQuizSolvedEvent(userId, questionId, question.getQuestionText(), isCorrect);

        return StudyQuizSubmitResponse.from(item, isCorrect,
                question.getCorrectAnswer(), question.getExplanation());
    }

    /**
     * 퀴즈 풀이 이벤트 발행 (게이미피케이션 경험치 적립용)
     */
    private void publishQuizSolvedEvent(Long userId, Long questionId, String questionText, boolean isCorrect) {
        try {
            eventPublisher.publishEvent(new QuizSolvedEvent(
                    this, userId, questionId, questionText, isCorrect, LocalDate.now()));
} catch (Exception e) {
}
    }

    /**
     * 스터디별 퀴즈 목록 조회
     */
    public List<StudyQuiz> getQuizzesByStudyId(Long studyId) {
        return studyQuizRepository.findByStudyIdOrderByCreatedAtDesc(studyId);
    }

    /**
     * 퀴즈 상세 조회
     */
    public StudyQuiz getQuizById(Long quizId) {
        return studyQuizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다: " + quizId));
    }
}

