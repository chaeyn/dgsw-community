package kr.hs.dgsw.community.domain.recruitment.repository;

import jakarta.persistence.EntityManager;
import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberRole;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentApplication;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentApplicationStatus;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("RecruitmentApplicationRepository 테스트")
@TestPropertySource(properties = "spring.sql.init.mode=never")
class RecruitmentApplicationRepositoryTest {

    @Autowired
    private RecruitmentApplicationRepository applicationRepository;

    @Autowired
    private RecruitmentRepository recruitmentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    private Recruitment recruitment;
    private Member kim;
    private Member lee;

    @BeforeEach
    void setUp() {
        Member hong = memberRepository.save(member("hong@dgsw.hs.kr", "홍길동"));
        kim = memberRepository.save(member("kim@dgsw.hs.kr", "김코딩"));
        lee = memberRepository.save(member("lee@dgsw.hs.kr", "이자바"));

        recruitment = recruitmentRepository.save(Recruitment.builder()
                .author(hong)
                .title("백엔드 팀원 구합니다")
                .content("같이 하실 분")
                .position(RecruitmentPosition.BACKEND)
                .capacity(2)
                .deadline(LocalDate.now().plusDays(7))
                .build());

        RecruitmentApplication accepted = application(kim);
        accepted.accept();
        applicationRepository.save(accepted);
    }

    private Member member(String email, String nickname) {
        return Member.builder()
                .email(email)
                .password("password123")
                .nickname(nickname)
                .role(MemberRole.USER)
                .build();
    }

    private RecruitmentApplication application(Member applicant) {
        return RecruitmentApplication.builder()
                .recruitment(recruitment)
                .applicant(applicant)
                .message("지원합니다")
                .build();
    }

    @Test
    @DisplayName("이미 지원한 회원인지 확인한다")
    void existsByRecruitmentIdAndApplicantId() {
        assertThat(applicationRepository.existsByRecruitmentIdAndApplicantId(recruitment.getId(), kim.getId()))
                .isTrue();
        assertThat(applicationRepository.existsByRecruitmentIdAndApplicantId(recruitment.getId(), lee.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("상태별 지원 수를 센다")
    void countByRecruitmentIdAndStatus() {
        applicationRepository.save(application(lee));

        assertThat(applicationRepository.countByRecruitmentIdAndStatus(
                recruitment.getId(), RecruitmentApplicationStatus.ACCEPTED)).isEqualTo(1);
        assertThat(applicationRepository.countByRecruitmentIdAndStatus(
                recruitment.getId(), RecruitmentApplicationStatus.PENDING)).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 모집글의 id 로는 지원 내역이 조회되지 않는다")
    void findByIdAndRecruitmentId_otherRecruitment() {
        RecruitmentApplication saved = applicationRepository.save(application(lee));

        assertThat(applicationRepository.findByIdAndRecruitmentId(saved.getId(), recruitment.getId()))
                .isPresent();
        assertThat(applicationRepository.findByIdAndRecruitmentId(saved.getId(), 999L))
                .isEmpty();
    }

    @Test
    @DisplayName("같은 모집글에 같은 회원이 두 번 저장되면 유니크 제약으로 실패한다")
    void save_duplicate() {
        assertThatThrownBy(() -> applicationRepository.saveAndFlush(application(kim)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("지원자 목록은 지원한 순서대로 조회되고 지원자 정보가 함께 로딩된다")
    void findByRecruitmentIdOrderByCreatedAtAsc() {
        applicationRepository.saveAndFlush(application(lee));
        em.clear();

        List<RecruitmentApplication> result =
                applicationRepository.findByRecruitmentIdOrderByCreatedAtAsc(recruitment.getId());

        assertThat(result)
                .extracting(a -> a.getApplicant().getNickname())
                .containsExactly("김코딩", "이자바");
        assertThat(Hibernate.isInitialized(result.get(0).getApplicant())).isTrue();
    }

    @Test
    @DisplayName("모집글의 지원 내역을 한 번에 삭제하고 다른 모집글의 지원은 남는다")
    void deleteByRecruitmentId() {
        applicationRepository.saveAndFlush(application(lee));

        applicationRepository.deleteByRecruitmentId(999L);
        assertThat(applicationRepository.count()).isEqualTo(2);

        applicationRepository.deleteByRecruitmentId(recruitment.getId());
        em.clear();
        assertThat(applicationRepository.count()).isZero();
    }
}
