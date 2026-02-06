package com.ssafy.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RecentUserDto {
    private Long id;
    private String nickname;
    private String email;
    private String loginMethod;
    private LocalDateTime createdAt;
}
