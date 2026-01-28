package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.response.StudyRecommendDto;
import com.ssafy.domain.study.entity.StudyRecommendAction;
import com.ssafy.domain.study.entity.StudyRecommendItem;
import com.ssafy.domain.study.entity.StudyRecommendLog;
import com.ssafy.domain.study.mapper.StudyRecommendMapper;
import com.ssafy.domain.study.repository.StudyRecommendActionRepository;
import com.ssafy.domain.study.repository.StudyRecommendItemRepository;
import com.ssafy.domain.study.repository.StudyRecommendLogRepository;
import com.ssafy.domain.user.entity.Profile;
import com.ssafy.domain.user.entity.UserSchedule;
import com.ssafy.domain.user.repository.ProfileRepository;
import com.ssafy.domain.user.repository.UserScheduleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudyRecommendService {

    private final StudyRecommendMapper studyRecommendMapper;
    private final StudyRecommendLogRepository recommendLogRepository;
    private final StudyRecommendItemRepository recommendItemRepository;
    private final StudyRecommendActionRepository recommendActionRepository;
    private final ProfileRepository profileRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    /**
     * 사용자 맞춤 스터디 참여 추천 + 로그 저장
     */
    @Transactional
    public List<StudyRecommendDto> getRecommendedStudies(Long userId, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        List<StudyRecommendDto> results = studyRecommendMapper.findRecommendedStudies(userId, safeLimit);

        for (StudyRecommendDto dto : results) {
            dto.setMatchReason(buildMatchReason(dto));
        }

        // 추천 로그 저장 (향후 LLM 학습 데이터용)
        saveRecommendLog(userId, StudyRecommendLog.RecommendType.GENERAL, null, results);

        return results;
    }

    /**
     * 특정 토픽 기반 스터디 추천 + 로그 저장
     */
    @Transactional
    public List<StudyRecommendDto> getRecommendedStudiesByTopic(Long userId, Long topicId, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        List<StudyRecommendDto> results = studyRecommendMapper.findRecommendedStudiesByTopic(userId, topicId, safeLimit);

        for (StudyRecommendDto dto : results) {
            dto.setMatchReason(buildMatchReason(dto));
        }

        saveRecommendLog(userId, StudyRecommendLog.RecommendType.TOPIC, topicId, results);

        return results;
    }

    /**
     * 사용자 행동 자동 감지 + 기록
     * 기존 서비스(상세조회/지원/북마크)에서 호출됨
     * "최근 30분 내 이 사용자에게 이 스터디를 추천한 적 있는가?" 자동 체크
     */
    @Transactional
    public void tryLogAction(Long userId, Long studyId, StudyRecommendAction.ActionType actionType) {
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(30);
            recommendLogRepository.findRecentLogContainingStudy(userId, studyId, since)
                    .ifPresent(logEntity -> {
                        List<StudyRecommendItem> items = recommendItemRepository.findByLogIdAndStudyId(logEntity.getId(), studyId);
                        Long itemId = items.isEmpty() ? null : items.get(0).getId();

                        StudyRecommendAction action = StudyRecommendAction.builder()
                                .logId(logEntity.getId())
                                .itemId(itemId)
                                .studyId(studyId)
                                .actionType(actionType)
                                .build();

                        recommendActionRepository.save(action);
                        log.debug("추천 반응 자동 기록: userId={}, studyId={}, action={}", userId, studyId, actionType);
                    });
        } catch (Exception e) {
            log.warn("추천 반응 기록 실패 (무시): {}", e.getMessage());
        }
    }

    /**
     * 추천 세션 + 추천 항목 로그 저장
     */
    private void saveRecommendLog(Long userId, StudyRecommendLog.RecommendType type,
                                   Long topicId, List<StudyRecommendDto> results) {
        try {
            // 사용자 프로필 스냅샷
            String techSnapshot = null;
            String scheduleSnapshot = null;

            Profile profile = profileRepository.findByUserId(userId).orElse(null);
            if (profile != null && profile.getTech() != null) {
                techSnapshot = profile.getTech();
            }

            List<UserSchedule> schedules = userScheduleRepository.findByUserIdAndIsAvailableTrue(userId);
            if (!schedules.isEmpty()) {
                scheduleSnapshot = objectMapper.writeValueAsString(
                        schedules.stream().map(s -> s.getDayOfWeek() + " " + s.getStartTime() + "-" + s.getEndTime()).toList()
                );
            }

            // 세션 로그 저장
            StudyRecommendLog logEntity = StudyRecommendLog.builder()
                    .userId(userId)
                    .recommendType(type)
                    .topicId(topicId)
                    .resultCount(results.size())
                    .userTechSnapshot(techSnapshot)
                    .userScheduleSnapshot(scheduleSnapshot)
                    .build();

            logEntity = recommendLogRepository.save(logEntity);

            // 추천 항목 저장
            for (int i = 0; i < results.size(); i++) {
                StudyRecommendDto dto = results.get(i);
                StudyRecommendItem item = StudyRecommendItem.builder()
                        .logId(logEntity.getId())
                        .studyId(dto.getStudyId())
                        .rankPosition(i + 1)
                        .matchingScore(dto.getMatchingScore())
                        .techMatchCount(dto.getTechMatchCount() != null ? dto.getTechMatchCount() : 0)
                        .scheduleMatchCount(dto.getScheduleMatchCount() != null ? dto.getScheduleMatchCount() : 0)
                        .topicMatchCount(dto.getTopicMatchCount() != null ? dto.getTopicMatchCount() : 0)
                        .matchReason(dto.getMatchReason())
                        .build();
                recommendItemRepository.save(item);
            }

            // 응답에 logId 세팅 (프론트에서 action 로깅 시 사용)
            for (StudyRecommendDto dto : results) {
                dto.setRecommendLogId(logEntity.getId());
            }

        } catch (JsonProcessingException e) {
            log.warn("추천 로그 스냅샷 직렬화 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("추천 로그 저장 실패 (추천 결과에는 영향 없음): {}", e.getMessage());
        }
    }

    private String buildMatchReason(StudyRecommendDto dto) {
        List<String> reasons = new ArrayList<>();

        if (dto.getTechMatchCount() != null && dto.getTechMatchCount() > 0) {
            reasons.add("기술스택 " + dto.getTechMatchCount() + "개 일치");
        }
        if (dto.getTopicMatchCount() != null && dto.getTopicMatchCount() > 0) {
            reasons.add("연관 기술 분야");
        }
        if (dto.getScheduleMatchCount() != null && dto.getScheduleMatchCount() > 0) {
            reasons.add("일정 " + dto.getScheduleMatchCount() + "일 겹침");
        }
        if (dto.getLeaderAvgRating() != null && dto.getLeaderAvgRating().doubleValue() >= 4.0) {
            reasons.add("스터디장 평점 " + dto.getLeaderAvgRating() + "점");
        }

        if (reasons.isEmpty()) {
            reasons.add("모집 중인 스터디");
        }

        return String.join(", ", reasons);
    }
}
