package kr.hs.dgsw.community.domain.recruitment.controller;

import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationDecisionRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentApplicationResponse;
import kr.hs.dgsw.community.domain.recruitment.service.RecruitmentApplicationService;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecruitmentApplicationService applicationService;

    private RecruitmentApplicationResponse sampleResponse(String status) {
        return new RecruitmentApplicationResponse(100L, 10L, 2L, "김코딩", "지원합니다",
                status, LocalDateTime.of(2026, 11, 4, 8, 50));
    }

    @Test
    @DisplayName("POST - 지원 성공 시 201 Created 와 Location 헤더를 반환한다")
    void apply_success() throws Exception {
        given(applicationService.apply(eq(10L), any(RecruitmentApplicationCreateRequest.class)))
                .willReturn(sampleResponse("PENDING"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RecruitmentApplicationCreateRequest(2L, "지원합니다"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BASE_URL + "/100"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST - 지원자 id 가 없으면 400 Bad Request 를 반환한다")
    void apply_validationFail() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RecruitmentApplicationCreateRequest(null, "지원합니다"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.name()))
                .andExpect(jsonPath("$.errors[0].field").value("memberId"));
    }

    @Test
    @DisplayName("POST - 이미 지원했으면 409 Conflict 를 반환한다")
    void apply_duplicate() throws Exception {
        willThrow(new BusinessException(ErrorCode.DUPLICATE_APPLICATION))
                .given(applicationService).apply(eq(10L), any(RecruitmentApplicationCreateRequest.class));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RecruitmentApplicationCreateRequest(2L, "지원합니다"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_APPLICATION.name()));
    }

    @Test
    @DisplayName("GET - 작성자가 조회하면 지원자 목록을 반환한다")
    void findAll_success() throws Exception {
        given(applicationService.findAll(10L, 1L)).willReturn(List.of(sampleResponse("PENDING")));

        mockMvc.perform(get(BASE_URL).param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].applicantNickname").value("김코딩"));
    }

    @Test
    @DisplayName("GET - 작성자가 아니면 403 Forbidden 을 반환한다")
    void findAll_forbidden() throws Exception {
        willThrow(new BusinessException(ErrorCode.RECRUITMENT_FORBIDDEN))
                .given(applicationService).findAll(10L, 2L);

        mockMvc.perform(get(BASE_URL).param("memberId", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.RECRUITMENT_FORBIDDEN.name()));
    }

    @Test
    @DisplayName("PUT - 수락하면 200 과 ACCEPTED 상태를 반환한다")
    void decide_success() throws Exception {
        given(applicationService.decide(eq(10L), eq(100L), any(RecruitmentApplicationDecisionRequest.class)))
                .willReturn(sampleResponse("ACCEPTED"));

        mockMvc.perform(put(BASE_URL + "/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RecruitmentApplicationDecisionRequest(1L, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("DELETE - 지원 취소 성공 시 204 No Content 를 반환한다")
    void cancel_success() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/100").param("memberId", "2"))
                .andExpect(status().isNoContent());

        verify(applicationService).cancel(10L, 100L, 2L);
    }
}
