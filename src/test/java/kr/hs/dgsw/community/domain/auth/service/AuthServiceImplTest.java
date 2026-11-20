package kr.hs.dgsw.community.domain.auth.service;

import kr.hs.dgsw.community.domain.auth.dto.request.LoginRequest;
import kr.hs.dgsw.community.domain.auth.dto.request.TokenReissueRequest;
import kr.hs.dgsw.community.domain.auth.dto.response.TokenResponse;
import kr.hs.dgsw.community.domain.auth.entity.RefreshToken;
import kr.hs.dgsw.community.domain.auth.repository.RefreshTokenRepository;
import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberRole;
import kr.hs.dgsw.community.domain.member.entity.MemberStatus;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import kr.hs.dgsw.community.global.config.JwtProperties;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import kr.hs.dgsw.community.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("AuthService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String RAW_PASSWORD = "password123";

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private JwtTokenProvider jwtTokenProvider;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                new JwtProperties("dgsw-community-test-secret-key-for-unit-test-1234567890", 3600, 1209600));
        authService = new AuthServiceImpl(memberRepository, refreshTokenRepository,
                passwordEncoder, jwtTokenProvider);
    }

    private Member createMember(Long id) {
        Member member = Member.builder()
                .email("hong@dgsw.hs.kr")
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .nickname("홍길동")
                .introduction("소개")
                .role(MemberRole.USER)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("성공 - 액세스/리프레시 토큰을 발급하고 리프레시 토큰을 저장한다")
        void login_success() {
            Member member = createMember(1L);
            given(memberRepository.findActiveByEmail("hong@dgsw.hs.kr", MemberStatus.ACTIVE))
                    .willReturn(Optional.of(member));

            TokenResponse response = authService.login(new LoginRequest("hong@dgsw.hs.kr", RAW_PASSWORD));

            assertThat(response.memberId()).isEqualTo(1L);
            assertThat(response.nickname()).isEqualTo("홍길동");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(3600);
            assertThat(jwtTokenProvider.parseMemberId(response.accessToken())).isEqualTo(1L);
            assertThat(jwtTokenProvider.parseRefreshTokenMemberId(response.refreshToken())).isEqualTo(1L);
            verify(refreshTokenRepository).deleteAllByMemberId(1L);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("성공 - 같은 시각에 두 번 발급해도 토큰 값은 매번 달라진다")
        void login_issues_unique_tokens() {
            Member member = createMember(1L);
            given(memberRepository.findActiveByEmail("hong@dgsw.hs.kr", MemberStatus.ACTIVE))
                    .willReturn(Optional.of(member));
            LoginRequest request = new LoginRequest("hong@dgsw.hs.kr", RAW_PASSWORD);

            TokenResponse first = authService.login(request);
            TokenResponse second = authService.login(request);

            assertThat(first.refreshToken()).isNotEqualTo(second.refreshToken());
            assertThat(first.accessToken()).isNotEqualTo(second.accessToken());
        }

        @Test
        @DisplayName("실패 - 비밀번호가 일치하지 않으면 LOGIN_FAILED 예외가 발생한다")
        void login_fail_wrong_password() {
            given(memberRepository.findActiveByEmail("hong@dgsw.hs.kr", MemberStatus.ACTIVE))
                    .willReturn(Optional.of(createMember(1L)));

            assertThatThrownBy(() -> authService.login(new LoginRequest("hong@dgsw.hs.kr", "wrong12345")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.LOGIN_FAILED);

            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("실패 - 가입되지 않았거나 탈퇴한 이메일이면 존재 여부를 구분하지 않고 LOGIN_FAILED 로 응답한다")
        void login_fail_unknown_email() {
            given(memberRepository.findActiveByEmail(anyString(), any(MemberStatus.class)))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@dgsw.hs.kr", RAW_PASSWORD)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.LOGIN_FAILED);
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        @Test
        @DisplayName("성공 - 저장된 리프레시 토큰은 폐기되고 새 토큰이 발급된다")
        void reissue_success() {
            Member member = createMember(1L);
            String refreshToken = jwtTokenProvider.createRefreshToken(1L);
            RefreshToken stored = RefreshToken.builder()
                    .memberId(1L)
                    .token(refreshToken)
                    .expiresAt(LocalDateTime.now().plusDays(14))
                    .build();

            given(refreshTokenRepository.findByToken(refreshToken)).willReturn(Optional.of(stored));
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).willReturn(Optional.of(member));

            TokenResponse response = authService.reissue(new TokenReissueRequest(refreshToken));

            assertThat(jwtTokenProvider.parseMemberId(response.accessToken())).isEqualTo(1L);
            verify(refreshTokenRepository).delete(stored);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("실패 - 저장되지 않은(로그아웃된) 리프레시 토큰이면 REFRESH_TOKEN_NOT_FOUND 예외가 발생한다")
        void reissue_fail_not_stored() {
            String refreshToken = jwtTokenProvider.createRefreshToken(1L);
            given(refreshTokenRepository.findByToken(refreshToken)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.reissue(new TokenReissueRequest(refreshToken)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 액세스 토큰으로 재발급을 시도하면 INVALID_TOKEN 예외가 발생한다")
        void reissue_fail_access_token() {
            String accessToken = jwtTokenProvider.createAccessToken(1L, MemberRole.USER.name());

            assertThatThrownBy(() -> authService.reissue(new TokenReissueRequest(accessToken)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);

            verify(refreshTokenRepository, never()).findByToken(anyString());
        }

        @Test
        @DisplayName("실패 - 만료 시각이 지난 리프레시 토큰은 삭제되고 EXPIRED_TOKEN 예외가 발생한다")
        void reissue_fail_expired() {
            String refreshToken = jwtTokenProvider.createRefreshToken(1L);
            RefreshToken stored = RefreshToken.builder()
                    .memberId(1L)
                    .token(refreshToken)
                    .expiresAt(LocalDateTime.now().minusSeconds(1))
                    .build();
            given(refreshTokenRepository.findByToken(refreshToken)).willReturn(Optional.of(stored));

            assertThatThrownBy(() -> authService.reissue(new TokenReissueRequest(refreshToken)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EXPIRED_TOKEN);

            verify(refreshTokenRepository).delete(stored);
            verify(memberRepository, never()).findByIdAndStatus(anyLong(), any(MemberStatus.class));
        }
    }

    @Nested
    @DisplayName("로그아웃 / 내 정보")
    class LogoutAndMe {

        @Test
        @DisplayName("성공 - 로그아웃하면 해당 회원의 리프레시 토큰이 모두 삭제된다")
        void logout_success() {
            String accessToken = jwtTokenProvider.createAccessToken(1L, MemberRole.USER.name());

            authService.logout(accessToken);

            verify(refreshTokenRepository).deleteAllByMemberId(1L);
        }

        @Test
        @DisplayName("실패 - 위조된 토큰으로 내 정보를 조회하면 INVALID_TOKEN 예외가 발생한다")
        void getMe_fail_invalid_token() {
            assertThatThrownBy(() -> authService.getMe("not.a.jwt"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("성공 - 액세스 토큰의 회원 정보를 반환하며 비밀번호는 포함하지 않는다")
        void getMe_success() {
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(createMember(1L)));
            String accessToken = jwtTokenProvider.createAccessToken(1L, MemberRole.USER.name());

            var response = authService.getMe(accessToken);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("hong@dgsw.hs.kr");
            assertThat(response.role()).isEqualTo("USER");
        }
    }
}
