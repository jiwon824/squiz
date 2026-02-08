package com.ssafy.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReadAllResponse {

    private int readCount;

    public static ReadAllResponse of(int readCount) {
        return ReadAllResponse.builder()
                .readCount(readCount)
                .build();
    }
}
