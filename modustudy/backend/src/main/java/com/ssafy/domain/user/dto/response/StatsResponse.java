package com.ssafy.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private Long totalUsers;      // 전체 회원 수
    private Long todayNewUsers;   // 오늘 가입자 수
    private Long activeUsers;     // 활성 사용자 수
    private Long totalStudies;    // 전체 스터디 수 (추가)
    private Long activeStudies;   // 진행 중인 스터디 수 (추가)
}
