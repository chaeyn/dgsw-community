package kr.hs.dgsw.community.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "요청 값의 타입이 올바르지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),

    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 모집글입니다."),
    RECRUITMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "모집글 작성자만 할 수 있는 작업입니다."),
    RECRUITMENT_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 마감된 모집글입니다."),
    RECRUITMENT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "진행 중인 모집글은 최대 3개까지 등록할 수 있습니다."),

    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 지원 내역입니다."),
    APPLICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 지원만 취소할 수 있습니다."),
    APPLICATION_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인이 작성한 모집글에는 지원할 수 없습니다."),
    DUPLICATE_APPLICATION(HttpStatus.CONFLICT, "이미 지원한 모집글입니다."),
    APPLICATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 수락 또는 거절된 지원입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
