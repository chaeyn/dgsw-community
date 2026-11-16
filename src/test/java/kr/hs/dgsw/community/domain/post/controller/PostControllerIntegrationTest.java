package kr.hs.dgsw.community.domain.post.controller;

import kr.hs.dgsw.community.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:post-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("게시글을 등록·조회·수정할 수 있고 삭제 요청은 거부한다")
    void createReadAndUpdatePost() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorId": 2, "title": "첫 게시글", "content": "처음 작성한 본문"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.authorId").value(2))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsByteArray())
                .path("data").path("id").asLong();
        String location = created.getResponse().getHeader("Location");
        assertThat(location).isEqualTo("/api/posts/" + id);

        mockMvc.perform(get("/api/posts").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(id));

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("첫 게시글"));

        MvcResult updated = mockMvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorId": 2, "title": "수정한 제목", "content": "수정한 본문"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정한 제목"))
                .andExpect(jsonPath("$.data.content").value("수정한 본문"))
                .andExpect(jsonPath("$.data.authorId").value(2))
                .andReturn();
        String updatedAt = objectMapper.readTree(updated.getResponse().getContentAsByteArray())
                .path("data").path("updatedAt").asText();
        assertThat(updatedAt).isNotBlank();

        mockMvc.perform(delete(location))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.name()));

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정한 제목"))
                .andExpect(jsonPath("$.data.content").value("수정한 본문"))
                .andExpect(jsonPath("$.data.authorId").value(2))
                .andExpect(jsonPath("$.data.updatedAt").value(updatedAt));
    }

    @Test
    @DisplayName("다른 작성자의 수정 요청은 403을 반환하고 게시글을 변경하지 않는다")
    void rejectUpdateByDifferentAuthor() throws Exception {
        MvcResult before = mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(put("/api/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorId": 2, "title": "수정 시도", "content": "다른 회원의 수정 요청"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_FORBIDDEN.name()));

        MvcResult after = mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(after.getResponse().getContentAsByteArray()).path("data"))
                .isEqualTo(objectMapper.readTree(before.getResponse().getContentAsByteArray()).path("data"));
    }

    @Test
    @DisplayName("작성자 ID가 없거나 제목이 공백인 수정 요청은 400을 반환한다")
    void rejectInvalidUpdate() throws Exception {
        mockMvc.perform(put("/api/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": " ", "content": "수정한 본문"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.name()))
                .andExpect(jsonPath("$.errors[*].field", hasItems("authorId", "title")));
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정 시 404를 반환한다")
    void rejectMissingPostUpdate() throws Exception {
        mockMvc.perform(put("/api/posts/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorId": 2, "title": "수정한 제목", "content": "수정한 본문"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()));
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 404를 반환한다")
    void rejectMissingPost() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()));
    }

    @Test
    @DisplayName("탈퇴한 회원으로 게시글을 등록하면 404를 반환한다")
    void rejectWithdrawnAuthor() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorId": 10, "title": "게시글", "content": "게시글 본문"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.name()));
    }
}
