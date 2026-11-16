package kr.hs.dgsw.community.domain.recruitment.repository;

import jakarta.persistence.EntityManager;
import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberRole;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentStatus;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("RecruitmentRepository 테스트")
@TestPropertySource(properties = "spring.sql.init.mode=never")
class RecruitmentRepositoryTest {

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    private Member hong;
    private Member kim;

    @BeforeEach
    void setUp() {
        hong = memberRepository.save(member("hong@dgsw.hs.kr", "홍길동"));
        kim = memberRepository.save(member("kim@dgsw.hs.kr", "김코딩"));

        recruitmentRepository.save(recruitment(hong, "스프링 백엔드 팀원 구합니다", RecruitmentPosition.BACKEND));
        recruitmentRepository.save(recruitment(hong, "리액트 프론트 구해요", RecruitmentPosition.FRONTEND));

        Recruitment closed = recruitment(kim, "Spring 스터디 겸 프로젝트", RecruitmentPosition.BACKEND);
        closed.close();
        recruitmentRepository.save(closed);

        em.flush();
        em.clear();
    }

    private Member member(String email, String nickname) {
        return Member.builder()
                .email(email)
                .password("password123")
                .nickname(nickname)
                .role(MemberRole.USER)
                .build();
    }

    private Recruitment recruitment(Member author, String title, RecruitmentPosition position) {
        return Recruitment.builder()
                .author(author)
                .title(title)
                .content("같이 프로젝트 하실 분")
                .position(position)
                .capacity(3)
                .deadline(LocalDate.now().plusDays(7))
                .build();
    }

    @Test
    @DisplayName("조건이 모두 없으면 전체 모집글이 조회된다")
    void search_noCondition() {
        Page<Recruitment> result = recruitmentRepository.search(null, null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("제목 키워드는 대소문자를 구분하지 않고 검색된다")
    void search_keyword() {
        Page<Recruitment> result = recruitmentRepository.search("spring", null, null, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Recruitment::getTitle)
                .containsExactly("Spring 스터디 겸 프로젝트");
    }

    @Test
    @DisplayName("모집 분야와 상태를 같이 걸면 둘 다 만족하는 글만 나온다")
    void search_positionAndStatus() {
        Page<Recruitment> result = recruitmentRepository.search(
                null, RecruitmentPosition.BACKEND, RecruitmentStatus.OPEN, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAuthor().getNickname()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("조건에 맞는 글이 없으면 빈 페이지를 반환한다")
    void search_noResult() {
        Page<Recruitment> result = recruitmentRepository.search(
                "없는제목", RecruitmentPosition.DESIGN, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("페이지 크기보다 글이 많으면 나눠서 조회되고 전체 개수는 유지된다")
    void search_paging() {
        Page<Recruitment> result = recruitmentRepository.search(null, null, null, PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("작성자별 진행 중인 모집글 수를 센다")
    void countByAuthorIdAndStatus() {
        assertThat(recruitmentRepository.countByAuthorIdAndStatus(hong.getId(), RecruitmentStatus.OPEN))
                .isEqualTo(2);
        assertThat(recruitmentRepository.countByAuthorIdAndStatus(kim.getId(), RecruitmentStatus.OPEN))
                .isZero();
    }

    @Test
    @DisplayName("단건 조회 시 작성자가 함께 로딩되고, 없는 id 면 비어 있다")
    void findWithAuthorById() {
        Long id = recruitmentRepository.search("리액트", null, null, PageRequest.of(0, 1))
                .getContent().get(0).getId();
        em.clear();

        Recruitment found = recruitmentRepository.findWithAuthorById(id).orElseThrow();

        assertThat(Hibernate.isInitialized(found.getAuthor())).isTrue();
        assertThat(found.getAuthor().getNickname()).isEqualTo("홍길동");
        assertThat(recruitmentRepository.findWithAuthorById(999L)).isEmpty();
    }
}
