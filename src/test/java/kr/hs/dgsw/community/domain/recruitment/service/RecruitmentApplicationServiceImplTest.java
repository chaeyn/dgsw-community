package kr.hs.dgsw.community.domain.recruitment.service;

import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberRole;
import kr.hs.dgsw.community.domain.member.entity.MemberStatus;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationDecisionRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentApplicationResponse;
import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentApplication;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentApplicationStatus;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import kr.hs.dgsw.community.domain.recruitment.repository.RecruitmentApplicationRepository;
import kr.hs.dgsw.community.domain.recruitment.repository.RecruitmentRepository;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("RecruitmentApplicationService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RecruitmentApplicationServiceImplTest {

    private static final Long RECRUITMENT_ID = 10L;
    private static final Long APPLICATION_ID = 100L;

    @Mock
    private RecruitmentApplicationRepository applicationRepository;

    @Mock
    private RecruitmentRepository recruitmentRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private RecruitmentApplicationServiceImpl applicationService;

    private Member author;
    private Member applicant;

    @BeforeEach
    void setUp() {
        author = createMember(1L, "홍길동");
        applicant = createMember(2L, "김코딩");
    }

    private Member createMember(Long id, String nickname) {
        Member member = Member.builder()
                .email(nickname + "@dgsw.hs.kr")
                .password("password123")
                .nickname(nickname)
                .role(MemberRole.USER)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Recruitment createRecruitment(int capacity, LocalDate deadline) {
        Recruitment recruitment = Recruitment.builder()
                .author(author)
                .title("백엔드 팀원 구합니다")
                .content("같이 하실 분")
                .position(RecruitmentPosition.BACKEND)
                .capacity(capacity)
                .deadline(deadline)
                .build();
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);
        return recruitment;
    }

    private Recruitment createRecruitment(int capacity) {
        return createRecruitment(capacity, LocalDate.now().plusDays(7));
    }

    private RecruitmentApplication createApplication(Recruitment recruitment) {
        RecruitmentApplication application = RecruitmentApplication.builder()
                .recruitment(recruitment)
                .applicant(applicant)
                .message("지원합니다")
                .build();
        ReflectionTestUtils.setField(application, "id", APPLICATION_ID);
        return application;
    }

    @Nested
    @DisplayName("지원")
    class Apply {

        private final RecruitmentApplicationCreateRequest request =
                new RecruitmentApplicationCreateRequest(2L, "지원합니다");

        @Test
        @DisplayName("성공 - PENDING 상태로 저장된다")
        void apply_success() {
            given(recruitmentRepository.findWithAuthorById(RECRUITMENT_ID)).willReturn(Optional.of(createRecruitment(2)));
            given(memberRepository.findByIdAndStatus(2L, MemberStatus.ACTIVE)).willReturn(Optional.of(applicant));
            given(applicationRepository.existsByRecruitmentIdAndApplicantId(RECRUITMENT_ID, 2L)).willReturn(false);
            given(applicationRepository.save(any(RecruitmentApplication.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            RecruitmentApplicationResponse response = applicationService.apply(RECRUITMENT_ID, request);

            assertThat(response.applicantNickname()).isEqualTo("김코딩");
            assertThat(response.status()).isEqualTo(RecruitmentApplicationStatus.PENDING.name());
        }

        @Test
        @DisplayName("실패 - 마감된 모집글에는 지원할 수 없다")
        void apply_closed() {
            Recruitment recruitment = createRecruitment(2);
            recruitment.close();
            given(recruitmentRepository.findWithAuthorById(RECRUITMENT_ID)).willReturn(Optional.of(recruitment));

            assertThatThrownBy(() -> applicationService.apply(RECRUITMENT_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_ALREADY_CLOSED);
        }

        @Test
        @DisplayName("실패 - 상태가 OPEN 이어도 마감일이 지났으면 지원할 수 없다")
        void apply_expired() {
            given(recruitmentRepository.findWithAuthorById(RECRUITMENT_ID))
                    .willReturn(Optional.of(createRecruitment(2, LocalDate.now().minusDays(1))));

            assertThatThrownBy(() -> applicationService.apply(RECRUITMENT_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_ALREADY_CLOSED);
        }

        @Test
        @DisplayName("실패 - 본인이 쓴 모집글에는 지원할 수 없다")
        void apply_self() {
            given(recruitmentRepository.findWithAuthorById(RECRUITMENT_ID)).willReturn(Optional.of(createRecruitment(2)));
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).willReturn(Optional.of(author));

            assertThatThrownBy(() -> applicationService.apply(
                    RECRUITMENT_ID, new RecruitmentApplicationCreateRequest(1L, "제가 쓴 글")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_SELF_NOT_ALLOWED);
        }

        @Test
        @DisplayName("실패 - 이미 지원한 모집글이면 DUPLICATE_APPLICATION 예외가 발생한다")
        void apply_duplicate() {
            given(recruitmentRepository.findWithAuthorById(RECRUITMENT_ID)).willReturn(Optional.of(createRecruitment(2)));
            given(memberRepository.findByIdAndStatus(2L, MemberStatus.ACTIVE)).willReturn(Optional.of(applicant));
            given(applicationRepository.existsByRecruitmentIdAndApplicantId(RECRUITMENT_ID, 2L)).willReturn(true);

            assertThatThrownBy(() -> applicationService.apply(RECRUITMENT_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_APPLICATION);

            verify(applicationRepository, never()).save(any(RecruitmentApplication.class));
        }
    }

    @Nested
    @DisplayName("수락/거절")
    class Decide {

        @Test
        @DisplayName("성공 - 수락해도 정원이 남으면 모집글은 OPEN 으로 유지된다")
        void accept_notFull() {
            Recruitment recruitment = createRecruitment(2);
            RecruitmentApplication application = createApplication(recruitment);
            given(recruitmentRepository.findWithAuthorById(RECRUITMENT_ID)).willReturn(Optional.of(recruitment));
            given(applicationRepository.findByIdAndRecruitmentId(APPLICATION_ID, RECRUITMENT_ID))
                    .willReturn(Optional.of(application));
            given(applicationRepository.countByRecruitmentIdAndStatus(
                    RECRUITMENT_ID, RecruitmentApplicationStatus.ACCEPTED)).willReturn(0L);

            RecruitmentApplicationResponse response = applicationService.decide(
                    RECRUITMENT_ID, APPLICATION_ID, new RecruitmentApplicationDecisionRequest(1L, true));

            assertThat(response.status()).isEqualTo(RecruitmentApplicationStatus.ACCEPTED.name());
            assertThat(recruitment.isClosed()).isFalse();
        }

        @Test
        @DisplayName("성공 - 수락으로 정원이 차면 모집글이 자동 마감된다")
        void accept_full() {
            Recruitment recruitment = createRecruitment(2);
            RecruitmentApplication application = createApplication(recruitment);
            given(recruitmentRepository.findWithAuthorById(RECRUITMENT_ID)).willReturn(Optional.of(recruitment));
            given(applicationRepository.findByIdAndRecruitmentId(APPLICATION_ID, RECRUITMENT_ID))
                    .willReturn(Optional.of(application));
            given(applicationRepository.countByRecruitmentIdAndStatus(
                    RECRUITMENT_ID, RecruitmentApplicationStatus.ACCEPTED)).willReturn(1L);

            applicationService.decide(
                    RECRUITMENT_ID, APPLICATION_ID, new RecruitmentApplicationDecisionRequest(1L, true));

            assertThat(recruitment.isClosed()).isTrue();
        }

        @Test
        @DisplayName("성공 - 거절하면 REJECTED 가 되고 모집글 상태는 그대로다")
        void reject_success() {
            Recruitment recruitment = createRecruitment(1);
            RecruitmentApplication application = createApplication(recruitment);
            given(recruitmentRepository.findWithAuthorById(RECRUITMENT_ID)).willReturn(Optional.of(recruitment));
            given(applicationRepository.findByIdAndRecruitmentId(APPLICATION_ID, RECRUITMENT_ID))
                    .willReturn(Optional.of(application));

            RecruitmentApplicationResponse response = applicationService.decide(
                    RECRUITMENT_ID, APPLICATION_ID, new RecruitmentApplicationDecisionRequest(1L, false));

            assertThat(response.status()).isEqualTo(RecruitmentApplicationStatus.REJECTED.name());
            assertThat(recruitment.isClosed()).isFalse();
        }

        @Test
        @DisplayName("실패 - 모집글 작성자가 아니면 RECRUITMENT_FORBIDDEN 예외가 발생한다")
        void decide_notAuthor() {
            given(recruitmentRepository.findWithAuthorById(RECRUITMENT_ID)).willReturn(Optional.of(createRecruitment(2)));

            assertThatThrownBy(() -> applicationService.decide(
                    RECRUITMENT_ID, APPLICATION_ID, new RecruitmentApplicationDecisionRequest(2L, true)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_FORBIDDEN);
        }

        @Test
        @DisplayName("실패 - 이미 처리된 지원은 다시 수락/거절할 수 없다")
        void decide_alreadyProcessed() {
            Recruitment recruitment = createRecruitment(2);
            RecruitmentApplication application = createApplication(recruitment);
            application.reject();
            given(recruitmentRepository.findWithAuthorById(RECRUITMENT_ID)).willReturn(Optional.of(recruitment));
            given(applicationRepository.findByIdAndRecruitmentId(APPLICATION_ID, RECRUITMENT_ID))
                    .willReturn(Optional.of(application));

            assertThatThrownBy(() -> applicationService.decide(
                    RECRUITMENT_ID, APPLICATION_ID, new RecruitmentApplicationDecisionRequest(1L, true)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_ALREADY_PROCESSED);

            assertThat(application.getStatus()).isEqualTo(RecruitmentApplicationStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("지원 취소")
    class Cancel {

        @Test
        @DisplayName("성공 - 대기 중인 본인 지원은 삭제된다")
        void cancel_success() {
            RecruitmentApplication application = createApplication(createRecruitment(2));
            given(applicationRepository.findByIdAndRecruitmentId(APPLICATION_ID, RECRUITMENT_ID))
                    .willReturn(Optional.of(application));

            applicationService.cancel(RECRUITMENT_ID, APPLICATION_ID, 2L);

            verify(applicationRepository).delete(application);
        }

        @Test
        @DisplayName("실패 - 다른 회원의 지원은 취소할 수 없다")
        void cancel_notApplicant() {
            RecruitmentApplication application = createApplication(createRecruitment(2));
            given(applicationRepository.findByIdAndRecruitmentId(APPLICATION_ID, RECRUITMENT_ID))
                    .willReturn(Optional.of(application));

            assertThatThrownBy(() -> applicationService.cancel(RECRUITMENT_ID, APPLICATION_ID, 3L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_FORBIDDEN);

            verify(applicationRepository, never()).delete(any(RecruitmentApplication.class));
        }

        @Test
        @DisplayName("실패 - 없는 지원 내역이면 APPLICATION_NOT_FOUND 예외가 발생한다")
        void cancel_notFound() {
            given(applicationRepository.findByIdAndRecruitmentId(APPLICATION_ID, RECRUITMENT_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.cancel(RECRUITMENT_ID, APPLICATION_ID, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
        }
    }
}
