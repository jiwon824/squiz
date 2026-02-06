package com.ssafy.domain.gamification.controller;

import com.ssafy.domain.gamification.event.QuizSolvedEvent;
import com.ssafy.domain.gamification.event.StudyAttendanceEvent;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.common.response.MessageResponse;  // ⭐ 추가
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/gamification/test")
@RequiredArgsConstructor
public class GamificationTestController {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 테스트: 스터디 출석 이벤트 발행
     */
    @PostMapping("/study-attendance")
    public ApiResponse<MessageResponse> testStudyAttendance(  // ⭐ String → MessageResponse
                                                              @RequestParam Long userId,
                                                              @RequestParam Long studyId
    ) {
        eventPublisher.publishEvent(new StudyAttendanceEvent(
                this,
                userId,
                studyId,
                "테스트 스터디",
                LocalDate.now()
        ));
        return ApiResponse.success("스터디 출석 이벤트 발행 완료");
    }

    /**
     * 테스트: 퀴즈 풀이 이벤트 발행
     */
    @PostMapping("/quiz-solved")
    public ApiResponse<MessageResponse> testQuizSolved(  // ⭐ String → MessageResponse
                                                         @RequestParam Long userId,
                                                         @RequestParam Long quizId,
                                                         @RequestParam boolean isCorrect
    ) {
        eventPublisher.publishEvent(new QuizSolvedEvent(
                this,
                userId,
                quizId,
                "테스트 퀴즈",
                isCorrect,
                LocalDate.now()
        ));
        return ApiResponse.success("퀴즈 풀이 이벤트 발행 완료");
    }
}
