package com.ssafy.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 연동된 소셜 계정 목록 응답
 */
@Getter
@Builder
@AllArgsConstructor
public class LinkedAccountsResponse {
    private List<SocialAccountResponse> linkedAccounts;  // 연동된 소셜 계정 목록
    private Boolean hasPassword;  // 비밀번호 설정 여부
}