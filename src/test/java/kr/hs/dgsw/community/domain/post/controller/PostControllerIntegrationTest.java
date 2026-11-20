package kr.hs.dgsw.community.domain.post.controller;

import kr.hs.dgsw.community.global.error.ErrorCode;
import kr.hs.dgsw.community.global.security.JwtTokenProvider;
import kr.hs.dgsw.community.global.config.JwtProperties;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import kr.hs.dgsw.community.domain.member.entity.Member;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
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

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:post-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private MemberRepository memberRepository;

    private String authorization(long memberId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(memberId, "USER");
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("게시글을 등록·조회·수정한 뒤 삭제할 수 있다")
    void createReadUpdateAndDeletePost() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"hong@dgsw.hs.kr","password":"password123"}
                                """))
                .andExpect(status().isOk()).andReturn();
        String token = "Bearer " + objectMapper.readTree(login.getResponse().getContentAsByteArray())
                .path("data").path("accessToken").asText();
        MvcResult created = mockMvc.perform(post("/api/posts").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "첫 게시글", "content": "처음 작성한 본문"}
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

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        mockMvc.perform(get("/api/posts").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(id))
                .andExpect(jsonPath("$.data.content[0].content").doesNotExist());
        assertThat(statistics.getEntityLoadCount()).isZero();

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("첫 게시글"));

        MvcResult updated = mockMvc.perform(put(location).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "수정한 제목", "content": "수정한 본문"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정한 제목"))
                .andExpect(jsonPath("$.data.content").value("수정한 본문"))
                .andExpect(jsonPath("$.data.authorId").value(2))
                .andReturn();
        String updatedAt = objectMapper.readTree(updated.getResponse().getContentAsByteArray())
                .path("data").path("updatedAt").asText();
        assertThat(updatedAt).isNotBlank();

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정한 제목"))
                .andExpect(jsonPath("$.data.content").value("수정한 본문"))
                .andExpect(jsonPath("$.data.authorId").value(2))
                .andExpect(jsonPath("$.data.updatedAt").value(updatedAt));

        mockMvc.perform(delete(location).header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(location))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()));
    }

    @Test
    @DisplayName("다른 작성자의 수정·삭제 요청은 403을 반환하고 게시글을 변경하지 않는다")
    void rejectChangesByDifferentAuthor() throws Exception {
        MvcResult before = mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(put("/api/posts/1").header("Authorization", authorization(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorId": 1, "title": "수정 시도", "content": "다른 회원의 수정 요청"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_FORBIDDEN.name()));

        mockMvc.perform(delete("/api/posts/1").header("Authorization", authorization(2)).param("authorId", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_FORBIDDEN.name()));

        MvcResult after = mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(after.getResponse().getContentAsByteArray()).path("data"))
                .isEqualTo(objectMapper.readTree(before.getResponse().getContentAsByteArray()).path("data"));
    }

    @Test
    @DisplayName("제목이 공백인 수정 요청은 400을 반환한다")
    void rejectInvalidUpdate() throws Exception {
        mockMvc.perform(put("/api/posts/1").header("Authorization", authorization(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": " ", "content": "수정한 본문"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.name()))
                .andExpect(jsonPath("$.errors[*].field", hasItems("title")));
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정·삭제 시 404를 반환한다")
    void rejectMissingPostChanges() throws Exception {
        mockMvc.perform(put("/api/posts/{id}", Long.MAX_VALUE).header("Authorization", authorization(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "수정한 제목", "content": "수정한 본문"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()));

        mockMvc.perform(delete("/api/posts/{id}", Long.MAX_VALUE).header("Authorization", authorization(2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.POST_NOT_FOUND.name()));
    }

    @Test
    @DisplayName("인증되지 않은 삭제 요청은 401을 반환한다")
    void rejectDeleteWithoutAuthentication() throws Exception {
        mockMvc.perform(delete("/api/posts/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.name()));
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
        mockMvc.perform(post("/api/posts").header("Authorization", authorization(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorId": 10, "title": "게시글", "content": "게시글 본문"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.name()));
    }

    private void assertWritesRejected(String authorization, ErrorCode code) throws Exception {
        MockHttpServletRequestBuilder[] requests = {
                post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"인증 확인\",\"content\":\"본문\"}"),
                put("/api/posts/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":1,\"title\":\"변조 시도\",\"content\":\"본문\"}"),
                delete("/api/posts/1").param("authorId", "1")
        };
        for (var request : requests) {
            if (authorization != null) request.header("Authorization", authorization);
            mockMvc.perform(request)
                    .andExpect(status().is(code.getHttpStatus().value()))
                    .andExpect(jsonPath("$.code").value(code.name()));
        }
    }

    @Test
    @DisplayName("토큰 누락·잘못된 Bearer 헤더로 생성·수정·삭제할 수 없다")
    void rejectMissingAuthentication() throws Exception {
        assertWritesRejected(null, ErrorCode.UNAUTHORIZED);
        assertWritesRejected("Basic invalid", ErrorCode.UNAUTHORIZED);
        assertWritesRejected("Bearer ", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("위조·만료·리프레시 토큰으로 생성·수정·삭제할 수 없다")
    void rejectInvalidTokens() throws Exception {
        assertWritesRejected("Bearer not.a.jwt", ErrorCode.INVALID_TOKEN);
        var foreignProvider = new JwtTokenProvider(new JwtProperties(
                "different-signing-key-for-post-test-12345678901234567890", 3600, 1209600));
        assertWritesRejected("Bearer " + foreignProvider.createAccessToken(1L, "ADMIN"), ErrorCode.INVALID_TOKEN);
        assertWritesRejected("Bearer " + jwtTokenProvider.createRefreshToken(1L), ErrorCode.INVALID_TOKEN);
        var expiredProvider = new JwtTokenProvider(new JwtProperties(jwtProperties.secret(), -60, 1209600));
        assertWritesRejected("Bearer " + expiredProvider.createAccessToken(1L, "ADMIN"), ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 토큰으로 생성·수정·삭제할 수 없다")
    void rejectMissingMember() throws Exception {
        assertWritesRejected(authorization(Long.MAX_VALUE), ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @Transactional
    @DisplayName("로그인 후 탈퇴한 작성자는 기존 토큰으로도 생성·수정·삭제할 수 없다")
    void rejectWithdrawnMember() throws Exception {
        String token = authorization(1);
        Member member = memberRepository.findById(1L).orElseThrow();
        member.withdraw();
        memberRepository.flush();
        assertWritesRejected(token, ErrorCode.MEMBER_NOT_FOUND);
        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("커뮤니티 이용 안내"));
    }

    @Test
    @Transactional
    @DisplayName("본문의 작성자 ID는 무시하고 인증된 회원을 작성자로 저장한다")
    void ignoreSpoofedCreateAuthor() throws Exception {
        mockMvc.perform(post("/api/posts").header("Authorization", authorization(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":1,\"title\":\"작성자 확인\",\"content\":\"본문\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authorId").value(2));
    }
}
