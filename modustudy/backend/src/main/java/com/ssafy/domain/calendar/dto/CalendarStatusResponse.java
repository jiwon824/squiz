package com.ssafy.domain.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarStatusResponse {

    private boolean connected;
    private String email;
    private LocalDateTime tokenExpiresAt;
    private boolean hasValidToken;
    private String calendarId;
}
