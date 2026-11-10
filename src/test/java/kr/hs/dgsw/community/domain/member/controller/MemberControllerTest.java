package kr.hs.dgsw.community.domain.member.controller;

import tools.jackson.databind.ObjectMapper;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import kr.hs.dgsw.community.domain.member.dto.request.MemberCreateRequest;
import kr.hs.dgsw.community.domain.member.dto.request.MemberUpdateRequest;
import kr.hs.dgsw.community.domain.member.dto.response.MemberResponse;
import kr.hs.dgsw.community.domain.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@DisplayName("MemberController MockMvc 테스트")
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    private MemberResponse sampleResponse() {
        return new MemberResponse(1L, "hong@dgsw.hs.kr", "홍길동", "안녕하세요",
                "USER", "ACTIVE", LocalDateTime.of(2025, 11, 2, 10, 10));
    }

    @Test
    @DisplayName("POST /api/members - 회원 가입 성공 시 201 Created 와 Location 헤더를 반환한다")
    void create_success() throws Exception {
        MemberCreateRequest request =
                new MemberCreateRequest("hong@dgsw.hs.kr", "password123", "홍길동", "안녕하세요");
        given(memberService.create(any(MemberCreateRequest.class))).willReturn(sampleResponse());

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/members/1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("hong@dgsw.hs.kr"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/members - 이메일 형식이 잘못되면 400 과 필드 오류 목록을 반환한다")
    void create_validationFail() throws Exception {
        MemberCreateRequest request =
                new MemberCreateRequest("not-an-email", "short", "홍", null);

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.name()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/members - 이메일이 중복되면 409 Conflict 를 반환한다")
    void create_duplicateEmail() throws Exception {
        MemberCreateRequest request =
                new MemberCreateRequest("hong@dgsw.hs.kr", "password123", "홍길동", null);
        willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL))
                .given(memberService).create(any(MemberCreateRequest.class));

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_EMAIL.name()));
    }

    @Test
    @DisplayName("GET /api/members/{id} - 회원 조회 성공 시 200 과 통일된 응답을 반환한다")
    void findById_success() throws Exception {
        given(memberService.findById(1L)).willReturn(sampleResponse());

        mockMvc.perform(get("/api/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.nickname").value("홍길동"));
    }

    @Test
    @DisplayName("GET /api/members/{id} - 존재하지 않는 회원이면 404 Not Found 를 반환한다")
    void findById_notFound() throws Exception {
        willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
                .given(memberService).findById(999L);

        mockMvc.perform(get("/api/members/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.name()));
    }

    @Test
    @DisplayName("PUT /api/members/{id} - 회원 정보 수정 성공 시 200 을 반환한다")
    void update_success() throws Exception {
        MemberUpdateRequest request = new MemberUpdateRequest("홍길동", "소개 변경");
        given(memberService.update(eq(1L), any(MemberUpdateRequest.class))).willReturn(sampleResponse());

        mockMvc.perform(put("/api/members/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 정보가 수정되었습니다."));
    }

    @Test
    @DisplayName("DELETE /api/members/{id} - 회원 탈퇴 성공 시 204 No Content 를 반환한다")
    void delete_success() throws Exception {
        doNothing().when(memberService).delete(1L);

        mockMvc.perform(delete("/api/members/1"))
                .andExpect(status().isNoContent());

        verify(memberService).delete(1L);
    }
}
