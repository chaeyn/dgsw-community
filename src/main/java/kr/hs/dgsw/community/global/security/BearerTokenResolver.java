package kr.hs.dgsw.community.global.security;

import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import org.springframework.util.StringUtils;

public final class BearerTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private BearerTokenResolver() {
    }

    public static String resolve(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return token;
    }
}
