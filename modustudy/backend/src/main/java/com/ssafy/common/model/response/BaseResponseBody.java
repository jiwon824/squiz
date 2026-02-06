package com.ssafy.common.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "기본 응답")
public class BaseResponseBody {

    @Schema(description = "응답 코드", example = "200")
    private Integer statusCode;

    @Schema(description = "응답 메시지", example = "Success")
    private String message;

    public BaseResponseBody() {}

    public BaseResponseBody(Integer statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public static BaseResponseBody of(Integer statusCode, String message) {
        return new BaseResponseBody(statusCode, message);
    }
}
