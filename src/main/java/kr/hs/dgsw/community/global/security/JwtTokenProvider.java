package kr.hs.dgsw.community.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kr.hs.dgsw.community.global.config.JwtProperties;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ROLE_CLAIM = "role";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";

    private final SecretKey secretKey;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;

    public JwtTokenProvider(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = Duration.ofSeconds(properties.accessTokenValiditySeconds());
        this.refreshTokenValidity = Duration.ofSeconds(properties.refreshTokenValiditySeconds());
    }

    public String createAccessToken(Long memberId, String role) {
        return createToken(memberId, ACCESS_TOKEN, role, accessTokenValidity);
    }

    public String createRefreshToken(Long memberId) {
        return createToken(memberId, REFRESH_TOKEN, null, refreshTokenValidity);
    }

    public Long parseMemberId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public Long parseRefreshTokenMemberId(String token) {
        Claims claims = parseClaims(token);
        if (!REFRESH_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return Long.valueOf(claims.getSubject());
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValidity.toSeconds();
    }

    public Duration getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    private String createToken(Long memberId, String type, String role, Duration validity) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .claim(TOKEN_TYPE_CLAIM, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)));

        if (role != null) {
            builder.claim(ROLE_CLAIM, role);
        }
        return builder.signWith(secretKey).compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 토큰 수신: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
