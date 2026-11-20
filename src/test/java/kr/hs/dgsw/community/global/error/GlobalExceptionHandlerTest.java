package kr.hs.dgsw.community.global.error;

import kr.hs.dgsw.community.domain.post.controller.PostController;
import kr.hs.dgsw.community.domain.post.service.PostService;
import kr.hs.dgsw.community.domain.recruitment.controller.RecruitmentController;
import kr.hs.dgsw.community.domain.recruitment.service.RecruitmentService;
import kr.hs.dgsw.community.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RecruitmentController.class, PostController.class})
@DisplayName("GlobalExceptionHandler 상태 코드 테스트")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecruitmentService recruitmentService;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("JSON 문법이 틀리면 400 INVALID_JSON 을 반환한다")
    void invalidJson() throws Exception {
        mockMvc.perform(post("/api/recruitments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": 1, \"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_JSON.name()));
    }

    @Test
    @DisplayName("없는 enum 값을 본문에 넣으면 400 INVALID_JSON 을 반환한다")
    void invalidEnumInBody() throws Exception {
        String body = """
                {"memberId": 1, "title": "제목", "content": "내용",
                 "position": "DEVOPS", "capacity": 2, "deadline": "2099-12-31"}
                """;

        mockMvc.perform(post("/api/recruitments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_JSON.name()));
    }

    @Test
    @DisplayName("경로 변수에 숫자가 아닌 값이 오면 400 INVALID_TYPE_VALUE 를 반환한다")
    void pathVariableTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/recruitments/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TYPE_VALUE.name()))
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    @DisplayName("쿼리 파라미터에 없는 enum 값이 오면 400 INVALID_TYPE_VALUE 를 반환한다")
    void queryParameterTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/recruitments").param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    @DisplayName("필수 쿼리 파라미터가 없으면 400 MISSING_PARAMETER 를 반환한다")
    void missingParameter() throws Exception {
        mockMvc.perform(delete("/api/posts/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MISSING_PARAMETER.name()))
                .andExpect(jsonPath("$.errors[0].field").value("authorId"));
    }

    @Test
    @DisplayName("없는 경로를 요청하면 404 RESOURCE_NOT_FOUND 를 반환한다")
    void noResource() throws Exception {
        mockMvc.perform(get("/api/nothing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.name()));
    }

    @Test
    @DisplayName("지원하지 않는 메서드로 요청하면 405 를 반환한다")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(patch("/api/recruitments/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.name()));
    }

    @Test
    @DisplayName("Content-Type 이 JSON 이 아니면 415 를 반환한다")
    void unsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/recruitments")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500 으로 처리된다")
    void unexpectedException() throws Exception {
        given(recruitmentService.findById(anyLong())).willThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/recruitments/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.name()));
    }
}
