package com.camilolopez.polizas.dto;
import java.time.Instant;
import java.util.List;
public record ApiError(Instant timestamp, int status, String code, String message,
                       String path, List<String> fieldErrors) {
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, List.of());
    }
}
