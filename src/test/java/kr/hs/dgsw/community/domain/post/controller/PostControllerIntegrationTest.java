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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("활동 회원이 게시글을 등록하면 생성된 ID와 Location을 반환한다")
    void createPost() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorId": 2, "title": "첫 게시글", "content": "처음 작성한 본문"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.authorId").value(2))
                .andExpect(jsonPath("$.data.title").value("첫 게시글"))
                .andExpect(jsonPath("$.data.content").value("처음 작성한 본문"))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsByteArray())
                .path("data").path("id").asLong();
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/posts/" + id);
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
