package kr.hs.dgsw.community.domain.recruitment.service;

import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberRole;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationDecisionRequest;
import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentStatus;
import kr.hs.dgsw.community.domain.recruitment.repository.RecruitmentRepository;
import kr.hs.dgsw.community.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 DB 와 JPA flush 동작까지 포함해 정원 도달 시 자동 마감을 검증한다.
 * (Mockito 단위 테스트는 count 결과를 고정값으로 주기 때문에 flush 로 인한 중복 집계를 잡지 못한다.)
 */
@DataJpaTest
@Import({RecruitmentApplicationServiceImpl.class, JpaAuditingConfig.class})
@DisplayName("지원 수락 시 자동 마감 통합 테스트")
@TestPropertySource(properties = "spring.sql.init.mode=never")
class RecruitmentApplicationAutoCloseTest {

    @Autowired
    private RecruitmentApplicationService applicationService;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member author;
    private Member kim;
    private Member lee;
    private Recruitment recruitment;

    @BeforeEach
    void setUp() {
        author = memberRepository.save(member("hong@dgsw.hs.kr", "홍길동"));
        kim = memberRepository.save(member("kim@dgsw.hs.kr", "김코딩"));
        lee = memberRepository.save(member("lee@dgsw.hs.kr", "이자바"));
        recruitment = recruitmentRepository.save(Recruitment.builder()
                .author(author)
                .title("백엔드 팀원 구합니다")
                .content("같이 하실 분")
                .position(RecruitmentPosition.BACKEND)
                .capacity(2)
                .deadline(LocalDate.now().plusDays(7))
                .build());
    }

    private Member member(String email, String nickname) {
        return Member.builder()
                .email(email)
                .password("password123")
                .nickname(nickname)
                .role(MemberRole.USER)
                .build();
    }

    private Long apply(Member applicant) {
        return applicationService.apply(recruitment.getId(), applicant.getId(),
                new RecruitmentApplicationCreateRequest("지원합니다")).id();
    }

    private void accept(Long applicationId) {
        applicationService.decide(recruitment.getId(), applicationId, author.getId(),
                new RecruitmentApplicationDecisionRequest(true));
    }

    @Test
    @DisplayName("정원 2명 중 1명만 수락하면 모집글은 OPEN 으로 유지된다")
    void acceptOne_staysOpen() {
        accept(apply(kim));

        assertThat(recruitmentRepository.findById(recruitment.getId()).orElseThrow().getStatus())
                .isEqualTo(RecruitmentStatus.OPEN);
    }

    @Test
    @DisplayName("정원 2명을 모두 수락하면 모집글이 CLOSED 로 바뀐다")
    void acceptTwo_closes() {
        Long first = apply(kim);
        Long second = apply(lee);

        accept(first);
        accept(second);

        assertThat(recruitmentRepository.findById(recruitment.getId()).orElseThrow().getStatus())
                .isEqualTo(RecruitmentStatus.CLOSED);
    }
}
