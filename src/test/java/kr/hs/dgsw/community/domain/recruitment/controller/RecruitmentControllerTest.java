package kr.hs.dgsw.community.domain.recruitment.controller;

import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentUpdateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentPageResponse;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentResponse;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentStatus;
import kr.hs.dgsw.community.domain.recruitment.service.RecruitmentService;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
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

@WebMvcTest(RecruitmentController.class)
@DisplayName("RecruitmentController MockMvc 테스트")
class RecruitmentControllerTest {

    private static final LocalDate DEADLINE = LocalDate.now().plusDays(7);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecruitmentService recruitmentService;

    private RecruitmentResponse sampleResponse() {
        return new RecruitmentResponse(10L, 1L, "홍길동", "백엔드 팀원 구합니다", "같이 하실 분",
                "BACKEND", 2, DEADLINE, "OPEN", LocalDateTime.of(2026, 11, 3, 9, 30));
    }

    private RecruitmentCreateRequest createRequest() {
        return new RecruitmentCreateRequest(
                1L, "백엔드 팀원 구합니다", "같이 하실 분", RecruitmentPosition.BACKEND, 2, DEADLINE);
    }

    private RecruitmentUpdateRequest updateRequest() {
        return new RecruitmentUpdateRequest(
                1L, "제목 수정", "내용 수정", RecruitmentPosition.BACKEND, 2, DEADLINE);
    }

    @Test
    @DisplayName("POST /api/recruitments - 등록 성공 시 201 Created 와 Location 헤더를 반환한다")
    void create_success() throws Exception {
        given(recruitmentService.create(any(RecruitmentCreateRequest.class))).willReturn(sampleResponse());

        mockMvc.perform(post("/api/recruitments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/recruitments/10"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authorNickname").value("홍길동"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /api/recruitments - 제목이 비었거나 마감일이 과거면 400 과 필드 오류를 반환한다")
    void create_validationFail() throws Exception {
        RecruitmentCreateRequest request = new RecruitmentCreateRequest(
                1L, " ", "같이 하실 분", RecruitmentPosition.BACKEND, 0, LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/recruitments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.name()))
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    @DisplayName("GET /api/recruitments/{id} - 없는 모집글이면 404 Not Found 를 반환한다")
    void findById_notFound() throws Exception {
        willThrow(new BusinessException(ErrorCode.RECRUITMENT_NOT_FOUND))
                .given(recruitmentService).findById(999L);

        mockMvc.perform(get("/api/recruitments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.RECRUITMENT_NOT_FOUND.name()));
    }

    @Test
    @DisplayName("GET /api/recruitments - 검색 조건이 서비스로 전달되고 페이지 응답을 반환한다")
    void search_success() throws Exception {
        RecruitmentPageResponse page =
                new RecruitmentPageResponse(List.of(sampleResponse()), 0, 10, 1, 1, true);
        given(recruitmentService.search(eq("백엔드"), eq(RecruitmentPosition.BACKEND),
                eq(RecruitmentStatus.OPEN), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/recruitments")
                        .param("keyword", "백엔드")
                        .param("position", "BACKEND")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("백엔드 팀원 구합니다"));
    }

    @Test
    @DisplayName("PUT /api/recruitments/{id} - 작성자가 아니면 403 Forbidden 을 반환한다")
    void update_forbidden() throws Exception {
        willThrow(new BusinessException(ErrorCode.RECRUITMENT_FORBIDDEN))
                .given(recruitmentService).update(eq(10L), any(RecruitmentUpdateRequest.class));

        mockMvc.perform(put("/api/recruitments/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.RECRUITMENT_FORBIDDEN.name()));
    }

    @Test
    @DisplayName("PUT /api/recruitments/{id}/close - 이미 마감된 글이면 409 Conflict 를 반환한다")
    void close_alreadyClosed() throws Exception {
        willThrow(new BusinessException(ErrorCode.RECRUITMENT_ALREADY_CLOSED))
                .given(recruitmentService).close(10L, 1L);

        mockMvc.perform(put("/api/recruitments/10/close").param("memberId", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.RECRUITMENT_ALREADY_CLOSED.name()));
    }

    @Test
    @DisplayName("DELETE /api/recruitments/{id} - 삭제 성공 시 204 No Content 를 반환한다")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/recruitments/10").param("memberId", "1"))
                .andExpect(status().isNoContent());

        verify(recruitmentService).delete(10L, 1L);
    }
}
