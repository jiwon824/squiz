package com.ssafy.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 공통 응답")
public class ApiResponse<T> {

    @Schema(description = "성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 데이터")
    private final T data;

    @Schema(description = "에러 정보")
    private final ErrorInfo error;

    // 생성자 (private으로 외부에서 직접 생성 막기)
    private ApiResponse(boolean success, T data, ErrorInfo error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    // 성공 응답 - 데이터 있을 때
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 성공 응답 - 메시지만 있을 때
    public static ApiResponse<MessageResponse> success(String message) {
        return new ApiResponse<>(true, new MessageResponse(message), null);
    }

    // 실패 응답
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorInfo(code, message));
    }

    // ErrorInfo 내부 클래스
    @Getter
    @Schema(description = "에러 정보")
    public static class ErrorInfo {

        @Schema(description = "에러 코드", example = "INVALID_PROVIDER")
        private final String code;

        @Schema(description = "에러 메시지", example = "지원하지 않는 OAuth 제공자입니다.")
        private final String message;

        public ErrorInfo(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
    // 성공 응답 - 데이터 없이 메시지만 (Void용)
    public static ApiResponse<Void> success(Void data, String message) {
        return new ApiResponse<>(true, null, null);
    }
}