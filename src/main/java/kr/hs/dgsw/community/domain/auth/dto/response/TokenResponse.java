package kr.hs.dgsw.community.domain.auth.dto.response;

public record TokenResponse(
        Long memberId,
        String nickname,
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
    private static final String BEARER = "Bearer";

    public static TokenResponse of(Long memberId, String nickname,
                                   String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(memberId, nickname, BEARER, accessToken, refreshToken, expiresIn);
    }
}
