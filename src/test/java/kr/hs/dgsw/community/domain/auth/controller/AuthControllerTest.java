package kr.hs.dgsw.community.domain.auth.controller;

import kr.hs.dgsw.community.domain.auth.dto.request.LoginRequest;
import kr.hs.dgsw.community.domain.auth.dto.request.TokenReissueRequest;
import kr.hs.dgsw.community.domain.auth.dto.response.TokenResponse;
import kr.hs.dgsw.community.domain.auth.service.AuthService;
import kr.hs.dgsw.community.domain.member.dto.response.MemberResponse;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController MockMvc 테스트")
class AuthControllerTest {

    private static final String ACCESS_TOKEN = "header.payload.signature";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private TokenResponse sampleToken() {
        return TokenResponse.of(1L, "홍길동", ACCESS_TOKEN, "refresh.token.value", 3600);
    }

    @Test
    @DisplayName("POST /api/auth/login - 로그인 성공 시 200 과 토큰을 반환한다")
    void login_success() throws Exception {
        given(authService.login(any(LoginRequest.class))).willReturn(sampleToken());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("hong@dgsw.hs.kr", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    @DisplayName("POST /api/auth/login - 비밀번호가 틀리면 401 과 통일된 에러 응답을 반환한다")
    void login_fail_unauthorized() throws Exception {
        willThrow(new BusinessException(ErrorCode.LOGIN_FAILED))
                .given(authService).login(any(LoginRequest.class));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("hong@dgsw.hs.kr", "wrongpassword1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    @DisplayName("POST /api/auth/login - 이메일 형식이 아니면 400 과 필드 에러를 반환한다")
    void login_fail_validation() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("not-an-email", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/reissue - 리프레시 토큰으로 새 토큰을 발급받는다")
    void reissue_success() throws Exception {
        given(authService.reissue(any(TokenReissueRequest.class))).willReturn(sampleToken());

        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenReissueRequest("refresh.token.value"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value(ACCESS_TOKEN));
    }

    @Test
    @DisplayName("GET /api/auth/me - Authorization 헤더의 토큰으로 내 정보를 조회한다")
    void me_success() throws Exception {
        given(authService.getMe(eq(ACCESS_TOKEN))).willReturn(new MemberResponse(
                1L, "hong@dgsw.hs.kr", "홍길동", "안녕하세요", "USER", "ACTIVE",
                LocalDateTime.of(2025, 11, 2, 10, 10)));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("hong@dgsw.hs.kr"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/auth/me - Authorization 헤더가 없으면 401 을 반환한다")
    void me_fail_without_header() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(authService, never()).getMe(any());
    }

    @Test
    @DisplayName("DELETE /api/auth/logout - 로그아웃에 성공하면 200 을 반환한다")
    void logout_success() throws Exception {
        doNothing().when(authService).logout(ACCESS_TOKEN);

        mockMvc.perform(delete("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."));

        verify(authService).logout(ACCESS_TOKEN);
    }
}
