package kr.hs.dgsw.community.domain.recruitment.controller;

import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationDecisionRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentApplicationResponse;
import kr.hs.dgsw.community.domain.recruitment.service.RecruitmentApplicationService;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import kr.hs.dgsw.community.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecruitmentApplicationController.class)
@DisplayName("RecruitmentApplicationController MockMvc 테스트")
class RecruitmentApplicationControllerTest {

    private static final String BASE_URL = "/api/recruitments/10/applications";
    private static final String AUTHOR_TOKEN = "Bearer author-token";
    private static final String OTHER_TOKEN = "Bearer other-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecruitmentApplicationService applicationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUpToken() {
        // 토큰 문자열마다 로그인한 회원 id 가 정해져 있다고 가정한다.
        given(jwtTokenProvider.parseMemberId(AUTHOR_TOKEN.substring(7))).willReturn(1L);
        given(jwtTokenProvider.parseMemberId(OTHER_TOKEN.substring(7))).willReturn(2L);
    }

    private RecruitmentApplicationResponse sampleResponse(String status) {
        return new RecruitmentApplicationResponse(100L, 10L, 2L, "김코딩", "지원합니다",
                status, LocalDateTime.of(2026, 11, 4, 8, 50));
    }

    @Test
    @DisplayName("POST - 지원 성공 시 201 Created 와 Location 헤더를 반환한다")
    void apply_success() throws Exception {
        given(applicationService.apply(eq(10L), eq(2L), any(RecruitmentApplicationCreateRequest.class)))
                .willReturn(sampleResponse("PENDING"));

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", OTHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RecruitmentApplicationCreateRequest("지원합니다"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BASE_URL + "/100"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST - Bearer 형식이 아닌 토큰으로 지원하면 401 Unauthorized 를 반환한다")
    void apply_invalidHeader() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "other-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RecruitmentApplicationCreateRequest("지원합니다"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.name()));
    }

    @Test
    @DisplayName("POST - 이미 지원했으면 409 Conflict 를 반환한다")
    void apply_duplicate() throws Exception {
        willThrow(new BusinessException(ErrorCode.DUPLICATE_APPLICATION))
                .given(applicationService).apply(eq(10L), eq(2L), any(RecruitmentApplicationCreateRequest.class));

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", OTHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RecruitmentApplicationCreateRequest("지원합니다"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_APPLICATION.name()));
    }

    @Test
    @DisplayName("GET - 작성자가 조회하면 지원자 목록을 반환한다")
    void findAll_success() throws Exception {
        given(applicationService.findAll(10L, 1L)).willReturn(List.of(sampleResponse("PENDING")));

        mockMvc.perform(get(BASE_URL).header("Authorization", AUTHOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].applicantNickname").value("김코딩"));
    }

    @Test
    @DisplayName("GET - 작성자가 아니면 403 Forbidden 을 반환한다")
    void findAll_forbidden() throws Exception {
        willThrow(new BusinessException(ErrorCode.RECRUITMENT_FORBIDDEN))
                .given(applicationService).findAll(10L, 2L);

        mockMvc.perform(get(BASE_URL).header("Authorization", OTHER_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.RECRUITMENT_FORBIDDEN.name()));
    }

    @Test
    @DisplayName("PUT - 수락하면 200 과 ACCEPTED 상태를 반환한다")
    void decide_success() throws Exception {
        given(applicationService.decide(eq(10L), eq(100L), eq(1L), any(RecruitmentApplicationDecisionRequest.class)))
                .willReturn(sampleResponse("ACCEPTED"));

        mockMvc.perform(put(BASE_URL + "/100")
                        .header("Authorization", AUTHOR_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RecruitmentApplicationDecisionRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("DELETE - 지원 취소 성공 시 204 No Content 를 반환한다")
    void cancel_success() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/100").header("Authorization", OTHER_TOKEN))
                .andExpect(status().isNoContent());

        verify(applicationService).cancel(10L, 100L, 2L);
    }
}
