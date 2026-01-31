package com.ssafy.domain.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.quiz.entity.StudyQuiz;
import com.ssafy.domain.quiz.entity.StudyQuizQuestion;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import com.ssafy.domain.quiz.repository.StudyQuizRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ObjectMapper objectMapper;

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
            log.warn("퀴즈 데이터가 비어있음 - meetingId: {}", meetingId);
            return null;
        }

        // 이미 해당 미팅에서 생성된 퀴즈가 있는지 확인
        if (studyQuizRepository.existsBySourceTypeAndSourceId(StudyQuiz.SourceType.MEETING, meetingId)) {
            log.info("이미 해당 미팅에서 생성된 퀴즈가 존재함 - meetingId: {}", meetingId);
            return studyQuizRepository.findBySourceTypeAndSourceId(StudyQuiz.SourceType.MEETING, meetingId)
                    .orElse(null);
        }

        try {
            // 퀴즈 파싱
            List<Map<String, Object>> questions = parseQuizJson(quizRaw);
            if (questions.isEmpty()) {
                log.warn("파싱된 퀴즈 문제가 없음 - meetingId: {}", meetingId);
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
            log.info("미팅 기반 퀴즈 저장 완료 - quizId: {}, meetingId: {}, questions: {}",
                    saved.getId(), meetingId, questions.size());

            return saved;

        } catch (Exception e) {
            log.error("퀴즈 저장 실패 - meetingId: {}, error: {}", meetingId, e.getMessage());
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
            log.warn("퀴즈 JSON 파싱 실패: {}", e.getMessage());
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
                log.warn("options JSON 변환 실패: {}", e.getMessage());
            }
        }

        // 타입 변환
        QuestionType questionType = QuestionType.MULTIPLE_CHOICE;
        if ("단답형".equals(type) || "SHORT_ANSWER".equalsIgnoreCase(type)) {
            questionType = QuestionType.SHORT_ANSWER;
        }

        return StudyQuizQuestion.builder()
                .questionText(questionText)
                .questionType(questionType)
                .options(optionsJson)
                .correctAnswer(answer != null ? answer : "")
                .explanation(explanation)
                .build();
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
