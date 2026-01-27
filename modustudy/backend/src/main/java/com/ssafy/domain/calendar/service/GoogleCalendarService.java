package com.ssafy.domain.calendar.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.ssafy.domain.calendar.dto.CalendarEventResponse;
import com.ssafy.domain.calendar.entity.CalendarWatch;
import com.ssafy.domain.calendar.entity.StudySessionCalendarMapping;
import com.ssafy.domain.calendar.repository.CalendarWatchRepository;
import com.ssafy.domain.calendar.repository.StudySessionCalendarMappingRepository;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.user.entity.SocialProvider;
import com.ssafy.domain.user.entity.UserSocialAccount;
import com.ssafy.domain.user.repository.UserSocialAccountRepository;
import com.ssafy.domain.user.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final UserSocialAccountRepository socialAccountRepository;
    private final CalendarWatchRepository calendarWatchRepository;
    private final StudySessionCalendarMappingRepository mappingRepository;
    private final OAuth2Service oAuth2Service;

    private static final String APPLICATION_NAME = "ModuStudy";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TIMEZONE = "Asia/Seoul";

    @Value("${app.frontend.url:https://i14d106.p.ssafy.io}")
    private String frontendUrl;

    /**
     * Google Calendar API 클라이언트 생성
     */
    private Calendar getCalendarClient(Long userId) throws GeneralSecurityException, IOException {
        UserSocialAccount socialAccount = socialAccountRepository
                .findByUserIdAndProvider(userId, SocialProvider.GOOGLE)
                .orElseThrow(() -> new IllegalArgumentException("Google 계정이 연동되어 있지 않습니다."));

        if (!socialAccount.hasCalendarAccess()) {
            throw new IllegalStateException("Google Calendar 접근 권한이 없습니다. 다시 로그인해주세요.");
        }

        // 토큰 만료 확인 및 갱신
        String accessToken = socialAccount.getAccessToken();
        if (socialAccount.isTokenExpired()) {
            accessToken = oAuth2Service.refreshGoogleAccessToken(userId);
        }

        // Credentials 생성
        GoogleCredentials credentials = GoogleCredentials.create(
                new AccessToken(accessToken, null)
        );

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        return new Calendar.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * 스터디 세션을 Google Calendar에 이벤트로 생성
     */
    @Transactional
    public String createEvent(Long userId, StudySession session, String studyTitle) {
        try {
            Calendar calendarService = getCalendarClient(userId);

            Event event = buildEventFromSession(session, studyTitle);

            Event createdEvent = calendarService.events()
                    .insert("primary", event)
                    .execute();

            log.info("Google Calendar 이벤트 생성: userId={}, eventId={}", userId, createdEvent.getId());

            return createdEvent.getId();

        } catch (Exception e) {
            log.error("Google Calendar 이벤트 생성 실패: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("Google Calendar 이벤트 생성에 실패했습니다.", e);
        }
    }

    /**
     * Google Calendar 이벤트 수정
     */
    @Transactional
    public void updateEvent(Long userId, StudySession session, String googleEventId, String studyTitle) {
        try {
            Calendar calendarService = getCalendarClient(userId);

            Event event = buildEventFromSession(session, studyTitle);

            calendarService.events()
                    .update("primary", googleEventId, event)
                    .execute();

            log.info("Google Calendar 이벤트 수정: userId={}, eventId={}", userId, googleEventId);

        } catch (Exception e) {
            log.error("Google Calendar 이벤트 수정 실패: userId={}, eventId={}, error={}", userId, googleEventId, e.getMessage());
            throw new RuntimeException("Google Calendar 이벤트 수정에 실패했습니다.", e);
        }
    }

    /**
     * Google Calendar 이벤트 삭제
     */
    @Transactional
    public void deleteEvent(Long userId, String googleEventId) {
        try {
            Calendar calendarService = getCalendarClient(userId);

            calendarService.events()
                    .delete("primary", googleEventId)
                    .execute();

            log.info("Google Calendar 이벤트 삭제: userId={}, eventId={}", userId, googleEventId);

        } catch (Exception e) {
            log.error("Google Calendar 이벤트 삭제 실패: userId={}, eventId={}, error={}", userId, googleEventId, e.getMessage());
            // 이벤트가 이미 삭제되었을 수 있으므로 예외를 던지지 않음
        }
    }

    /**
     * Google Calendar에서 이벤트 목록 조회
     */
    public List<CalendarEventResponse> getEvents(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Calendar calendarService = getCalendarClient(userId);

            com.google.api.client.util.DateTime start = toGoogleDateTime(startTime);
            com.google.api.client.util.DateTime end = toGoogleDateTime(endTime);

            Events events = calendarService.events()
                    .list("primary")
                    .setTimeMin(start)
                    .setTimeMax(end)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            return events.getItems().stream()
                    .map(this::toCalendarEventResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Google Calendar 이벤트 조회 실패: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("Google Calendar 이벤트 조회에 실패했습니다.", e);
        }
    }

    /**
     * Calendar 연동 상태 확인
     */
    public boolean isCalendarLinked(Long userId) {
        return socialAccountRepository
                .findByUserIdAndProvider(userId, SocialProvider.GOOGLE)
                .map(UserSocialAccount::hasCalendarAccess)
                .orElse(false);
    }

    /**
     * Calendar Watch 등록 (Webhook)
     */
    @Transactional
    public CalendarWatch registerWatch(Long userId, String webhookUrl) {
        try {
            Calendar calendarService = getCalendarClient(userId);

            // 기존 watch가 있으면 삭제
            calendarWatchRepository.findByUserId(userId).ifPresent(existing -> {
                try {
                    stopWatch(userId, existing.getChannelId(), existing.getResourceId());
                } catch (Exception e) {
                    log.warn("기존 watch 중지 실패: {}", e.getMessage());
                }
                calendarWatchRepository.delete(existing);
            });

            // 새 watch 등록
            Channel channel = new Channel();
            channel.setId(UUID.randomUUID().toString());
            channel.setType("web_hook");
            channel.setAddress(webhookUrl);
            channel.setExpiration(System.currentTimeMillis() + 604800000L); // 7일

            Channel watchResponse = calendarService.events()
                    .watch("primary", channel)
                    .execute();

            CalendarWatch watch = CalendarWatch.builder()
                    .userId(userId)
                    .channelId(watchResponse.getId())
                    .resourceId(watchResponse.getResourceId())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            return calendarWatchRepository.save(watch);

        } catch (Exception e) {
            log.error("Calendar Watch 등록 실패: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("Calendar Watch 등록에 실패했습니다.", e);
        }
    }

    /**
     * Calendar Watch 중지
     */
    public void stopWatch(Long userId, String channelId, String resourceId) {
        try {
            Calendar calendarService = getCalendarClient(userId);

            Channel channel = new Channel();
            channel.setId(channelId);
            channel.setResourceId(resourceId);

            calendarService.channels().stop(channel).execute();

            log.info("Calendar Watch 중지: userId={}, channelId={}", userId, channelId);

        } catch (Exception e) {
            log.error("Calendar Watch 중지 실패: {}", e.getMessage());
        }
    }

    /**
     * Incremental Sync (변경분만 조회)
     */
    public List<Event> incrementalSync(Long userId, String syncToken) {
        try {
            Calendar calendarService = getCalendarClient(userId);

            Events events = calendarService.events()
                    .list("primary")
                    .setSyncToken(syncToken)
                    .execute();

            // 새 syncToken 저장
            calendarWatchRepository.findByUserId(userId).ifPresent(watch -> {
                watch.setSyncToken(events.getNextSyncToken());
                calendarWatchRepository.save(watch);
            });

            return events.getItems();

        } catch (Exception e) {
            log.error("Incremental Sync 실패: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("Calendar 동기화에 실패했습니다.", e);
        }
    }

    /**
     * 세션-이벤트 매핑 저장
     */
    @Transactional
    public void saveEventMapping(Long sessionId, Long userId, String googleEventId) {
        StudySessionCalendarMapping mapping = StudySessionCalendarMapping.builder()
                .sessionId(sessionId)
                .userId(userId)
                .googleEventId(googleEventId)
                .build();

        mappingRepository.save(mapping);
    }

    /**
     * 세션-이벤트 매핑 조회
     */
    public Optional<StudySessionCalendarMapping> getEventMapping(Long sessionId, Long userId) {
        return mappingRepository.findBySessionIdAndUserId(sessionId, userId);
    }

    /**
     * 세션의 모든 매핑 삭제
     */
    @Transactional
    public void deleteEventMappings(Long sessionId) {
        List<StudySessionCalendarMapping> mappings = mappingRepository.findBySessionId(sessionId);
        for (StudySessionCalendarMapping mapping : mappings) {
            try {
                deleteEvent(mapping.getUserId(), mapping.getGoogleEventId());
            } catch (Exception e) {
                log.warn("이벤트 삭제 실패: sessionId={}, userId={}", sessionId, mapping.getUserId());
            }
        }
        mappingRepository.deleteBySessionId(sessionId);
    }

    // ==================== Helper Methods ====================

    private Event buildEventFromSession(StudySession session, String studyTitle) {
        Event event = new Event();

        // 제목
        String title = String.format("[ModuStudy] %s - %s",
                studyTitle,
                session.getTitle() != null ? session.getTitle() : "세션 #" + session.getSessionNumber());
        event.setSummary(title);

        // 설명
        StringBuilder description = new StringBuilder();
        description.append("ModuStudy 스터디 세션\n\n");
        if (session.getDescription() != null) {
            description.append(session.getDescription()).append("\n\n");
        }
        if (session.getLocation() != null) {
            description.append("장소: ").append(session.getLocation()).append("\n");
        }
        description.append("온라인: ").append(session.getIsOnline() ? "예" : "아니오");
        event.setDescription(description.toString());

        // 시작/종료 시간
        EventDateTime start = new EventDateTime()
                .setDateTime(toGoogleDateTime(session.getScheduledAt()))
                .setTimeZone(TIMEZONE);
        event.setStart(start);

        int durationMinutes = session.getDurationMinutes() != null ? session.getDurationMinutes() : 60;
        EventDateTime end = new EventDateTime()
                .setDateTime(toGoogleDateTime(session.getScheduledAt().plusMinutes(durationMinutes)))
                .setTimeZone(TIMEZONE);
        event.setEnd(end);

        // 알림 설정 (30분 전, 10분 전)
        EventReminder[] reminders = new EventReminder[] {
                new EventReminder().setMethod("popup").setMinutes(30),
                new EventReminder().setMethod("popup").setMinutes(10)
        };
        event.setReminders(new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminders)));

        return event;
    }

    private com.google.api.client.util.DateTime toGoogleDateTime(LocalDateTime localDateTime) {
        return new com.google.api.client.util.DateTime(
                Date.from(localDateTime.atZone(ZoneId.of(TIMEZONE)).toInstant())
        );
    }

    private CalendarEventResponse toCalendarEventResponse(Event event) {
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;

        if (event.getStart() != null && event.getStart().getDateTime() != null) {
            startTime = LocalDateTime.ofInstant(
                    new Date(event.getStart().getDateTime().getValue()).toInstant(),
                    ZoneId.of(TIMEZONE)
            );
        }

        if (event.getEnd() != null && event.getEnd().getDateTime() != null) {
            endTime = LocalDateTime.ofInstant(
                    new Date(event.getEnd().getDateTime().getValue()).toInstant(),
                    ZoneId.of(TIMEZONE)
            );
        }

        return CalendarEventResponse.builder()
                .id(event.getId())
                .title(event.getSummary())
                .description(event.getDescription())
                .startTime(startTime)
                .endTime(endTime)
                .location(event.getLocation())
                .build();
    }
}
