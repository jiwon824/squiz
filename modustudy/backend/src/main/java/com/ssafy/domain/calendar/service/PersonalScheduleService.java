package com.ssafy.domain.calendar.service;

import com.ssafy.domain.calendar.dto.PersonalScheduleRequest;
import com.ssafy.domain.calendar.dto.PersonalScheduleResponse;
import com.ssafy.domain.calendar.entity.PersonalSchedule;
import com.ssafy.domain.calendar.repository.PersonalScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 개인 일정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PersonalScheduleService {

    private final PersonalScheduleRepository personalScheduleRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 날짜 범위 내 개인 일정 조회
     */
    @Transactional(readOnly = true)
    public List<PersonalScheduleResponse> getSchedules(Long userId, String startDateStr, String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);

        log.info("개인 일정 조회: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        List<PersonalSchedule> schedules = personalScheduleRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        return schedules.stream()
                .map(PersonalScheduleResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 개인 일정 단건 조회
     */
    @Transactional(readOnly = true)
    public PersonalScheduleResponse getSchedule(Long userId, Long scheduleId) {
        PersonalSchedule schedule = personalScheduleRepository.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. id=" + scheduleId));

        return PersonalScheduleResponse.from(schedule);
    }

    /**
     * 개인 일정 생성
     */
    public PersonalScheduleResponse createSchedule(Long userId, PersonalScheduleRequest request) {
        log.info("개인 일정 생성: userId={}, title={}", userId, request.getTitle());

        PersonalSchedule schedule = PersonalSchedule.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate() != null ? LocalDate.parse(request.getStartDate(), DATE_FORMATTER) : null)
                .startTime(request.getStartTime() != null ? LocalTime.parse(request.getStartTime(), TIME_FORMATTER) : null)
                .endDate(request.getEndDate() != null ? LocalDate.parse(request.getEndDate(), DATE_FORMATTER) : null)
                .endTime(request.getEndTime() != null ? LocalTime.parse(request.getEndTime(), TIME_FORMATTER) : null)
                .location(request.getLocation())
                .isOnline(request.getIsOnline() != null ? request.getIsOnline() : false)
                .color(request.getColor())
                .build();

        schedule = personalScheduleRepository.save(schedule);
        log.info("개인 일정 생성 완료: id={}", schedule.getId());

        return PersonalScheduleResponse.from(schedule);
    }

    /**
     * 개인 일정 수정
     */
    public PersonalScheduleResponse updateSchedule(Long userId, Long scheduleId, PersonalScheduleRequest request) {
        PersonalSchedule schedule = personalScheduleRepository.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. id=" + scheduleId));

        log.info("개인 일정 수정: id={}, userId={}", scheduleId, userId);

        if (request.getTitle() != null) {
            schedule.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            schedule.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            schedule.setStartDate(LocalDate.parse(request.getStartDate(), DATE_FORMATTER));
        }
        if (request.getStartTime() != null) {
            schedule.setStartTime(LocalTime.parse(request.getStartTime(), TIME_FORMATTER));
        }
        if (request.getEndDate() != null) {
            schedule.setEndDate(LocalDate.parse(request.getEndDate(), DATE_FORMATTER));
        }
        if (request.getEndTime() != null) {
            schedule.setEndTime(LocalTime.parse(request.getEndTime(), TIME_FORMATTER));
        }
        if (request.getLocation() != null) {
            schedule.setLocation(request.getLocation());
        }
        if (request.getIsOnline() != null) {
            schedule.setIsOnline(request.getIsOnline());
        }
        if (request.getColor() != null) {
            schedule.setColor(request.getColor());
        }

        schedule = personalScheduleRepository.save(schedule);

        return PersonalScheduleResponse.from(schedule);
    }

    /**
     * 개인 일정 삭제
     */
    public void deleteSchedule(Long userId, Long scheduleId) {
        PersonalSchedule schedule = personalScheduleRepository.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다. id=" + scheduleId));

        log.info("개인 일정 삭제: id={}, userId={}", scheduleId, userId);

        personalScheduleRepository.delete(schedule);
    }
}
