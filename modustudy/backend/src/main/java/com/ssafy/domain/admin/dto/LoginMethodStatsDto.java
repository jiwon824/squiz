package com.ssafy.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginMethodStatsDto {
    private String method;
    private int count;
}
