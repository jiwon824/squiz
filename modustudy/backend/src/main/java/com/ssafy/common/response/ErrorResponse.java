package com.ssafy.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "에러 응답")
public class ErrorResponse {

    @Schema(description = "응답 코드", example = "400")
    private final int status;

    @Schema(description = "에러 코드", example = "USER_NOT_FOUND")
    private final String code;

    @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다.")
    private final String message;

    @Schema(description = "필드 에러 목록")
    private final List<FieldError> errors;

    @Schema(description = "에러 발생 시간")
    private final LocalDateTime timestamp;

    public static ErrorResponse of(int status, String code, String message) {
        return ErrorResponse.builder()
                .status(status)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(int status, String code, String message, List<FieldError> errors) {
        return ErrorResponse.builder()
                .status(status)
                .code(code)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "필드 에러")
    public static class FieldError {

        @Schema(description = "필드명", example = "email")
        private final String field;

        @Schema(description = "입력값", example = "invalid-email")
        private final String value;

        @Schema(description = "에러 메시지", example = "올바른 이메일 형식이 아닙니다.")
        private final String reason;
    }
}
