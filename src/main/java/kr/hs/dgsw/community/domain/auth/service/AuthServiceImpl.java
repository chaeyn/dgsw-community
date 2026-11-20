package kr.hs.dgsw.community.domain.auth.service;

import kr.hs.dgsw.community.domain.auth.dto.request.LoginRequest;
import kr.hs.dgsw.community.domain.auth.dto.request.TokenReissueRequest;
import kr.hs.dgsw.community.domain.auth.dto.response.TokenResponse;
import kr.hs.dgsw.community.domain.auth.entity.RefreshToken;
import kr.hs.dgsw.community.domain.auth.repository.RefreshTokenRepository;
import kr.hs.dgsw.community.domain.member.dto.response.MemberResponse;
import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberStatus;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import kr.hs.dgsw.community.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findActiveByEmail(request.email(), MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            log.warn("로그인 실패 - 비밀번호 불일치: email={}", request.email());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        refreshTokenRepository.deleteAllByMemberId(member.getId());
        log.info("로그인 성공: id={}, email={}", member.getId(), member.getEmail());
        return issueTokens(member);
    }

    @Override
    @Transactional
    public TokenResponse reissue(TokenReissueRequest request) {
        Long memberId = jwtTokenProvider.parseRefreshTokenMemberId(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!stored.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if (stored.isExpired(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        Member member = getActiveMember(memberId);
        refreshTokenRepository.delete(stored);
        log.info("토큰 재발급: id={}", memberId);
        return issueTokens(member);
    }

    @Override
    @Transactional
    public void logout(String accessToken) {
        Long memberId = jwtTokenProvider.parseMemberId(accessToken);
        int deleted = refreshTokenRepository.deleteAllByMemberId(memberId);
        log.info("로그아웃 처리: id={}, 삭제된 리프레시 토큰={}", memberId, deleted);
    }

    @Override
    public MemberResponse getMe(String accessToken) {
        Long memberId = jwtTokenProvider.parseMemberId(accessToken);
        return MemberResponse.from(getActiveMember(memberId));
    }

    private TokenResponse issueTokens(Member member) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .memberId(member.getId())
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plus(jwtTokenProvider.getRefreshTokenValidity()))
                .build());

        return TokenResponse.of(member.getId(), member.getNickname(), accessToken, refreshToken,
                jwtTokenProvider.getAccessTokenValiditySeconds());
    }

    private Member getActiveMember(Long id) {
        return memberRepository.findByIdAndStatus(id, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
