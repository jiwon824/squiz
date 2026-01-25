package com.ssafy.domain.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class ProfileSetupRequest {
    // 필수
    private String nickname;
    private String password;

    // 선택
    private List<String> interests;    // ["AI", "웹개발", "알고리즘"]
    private List<String> techStacks;   // ["Java", "Python", "React"]
}