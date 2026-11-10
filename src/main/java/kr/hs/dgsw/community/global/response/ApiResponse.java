package kr.hs.dgsw.community.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {
    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> of(String message) {
        return new ApiResponse<>(true, message, null);
    }
}
