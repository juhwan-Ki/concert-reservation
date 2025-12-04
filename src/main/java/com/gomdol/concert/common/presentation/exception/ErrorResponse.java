package com.gomdol.concert.common.presentation.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String code;                    // "BAD_REQUEST", "CONFLICT" ...
    private String message;                 // 사용자/개발자 메시지
    private String path;                    // 요청 URI (선택)
    private Instant timestamp;              // 발생 시각
    private Map<String, Object> details;    // 어떤 상황이든 담을 수 있는 확장 필드

    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse of(String code, String message, String path, Map<String, Object> details) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .details(details)
                .build();
    }
}

