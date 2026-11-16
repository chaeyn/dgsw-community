package kr.hs.dgsw.community.domain.recruitment.service;

import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberRole;
import kr.hs.dgsw.community.domain.member.entity.MemberStatus;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentUpdateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentPageResponse;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentResponse;
import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentStatus;
import kr.hs.dgsw.community.domain.recruitment.repository.RecruitmentApplicationRepository;
import kr.hs.dgsw.community.domain.recruitment.repository.RecruitmentRepository;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("RecruitmentService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RecruitmentServiceImplTest {

    private static final LocalDate DEADLINE = LocalDate.now().plusDays(7);

    @Mock
    private RecruitmentRepository recruitmentRepository;

    @Mock
    private RecruitmentApplicationRepository applicationRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private RecruitmentServiceImpl recruitmentService;

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

    private Recruitment createRecruitment(Long id, Member author) {
        Recruitment recruitment = Recruitment.builder()
                .author(author)
                .title("백엔드 팀원 구합니다")
                .content("같이 하실 분")
                .position(RecruitmentPosition.BACKEND)
                .capacity(2)
                .deadline(DEADLINE)
                .build();
        ReflectionTestUtils.setField(recruitment, "id", id);
        return recruitment;
    }

    @Nested
    @DisplayName("모집글 등록")
    class Create {

        private final RecruitmentCreateRequest request = new RecruitmentCreateRequest(
                1L, "백엔드 팀원 구합니다", "같이 하실 분", RecruitmentPosition.BACKEND, 2, DEADLINE);

        @Test
        @DisplayName("성공 - OPEN 상태로 저장되고 작성자 정보가 응답에 담긴다")
        void create_success() {
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(createMember(1L, "홍길동")));
            given(recruitmentRepository.countByAuthorIdAndStatus(1L, RecruitmentStatus.OPEN)).willReturn(0L);
            given(recruitmentRepository.save(any(Recruitment.class)))
                    .willAnswer(invocation -> {
                        Recruitment recruitment = invocation.getArgument(0);
                        ReflectionTestUtils.setField(recruitment, "id", 10L);
                        return recruitment;
                    });

            RecruitmentResponse response = recruitmentService.create(request);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.authorNickname()).isEqualTo("홍길동");
            assertThat(response.status()).isEqualTo(RecruitmentStatus.OPEN.name());
        }

        @Test
        @DisplayName("실패 - 탈퇴했거나 없는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void create_memberNotFound() {
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recruitmentService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

            verify(recruitmentRepository, never()).save(any(Recruitment.class));
        }

        @Test
        @DisplayName("실패 - 진행 중인 모집글이 이미 3개면 RECRUITMENT_LIMIT_EXCEEDED 예외가 발생한다")
        void create_limitExceeded() {
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(createMember(1L, "홍길동")));
            given(recruitmentRepository.countByAuthorIdAndStatus(1L, RecruitmentStatus.OPEN)).willReturn(3L);

            assertThatThrownBy(() -> recruitmentService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_LIMIT_EXCEEDED);

            verify(recruitmentRepository, never()).save(any(Recruitment.class));
        }
    }

    @Nested
    @DisplayName("모집글 조회")
    class Find {

        @Test
        @DisplayName("실패 - 없는 모집글이면 RECRUITMENT_NOT_FOUND 예외가 발생한다")
        void findById_notFound() {
            given(recruitmentRepository.findWithAuthorById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recruitmentService.findById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("모집글 검색")
    class Search {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("검색 결과가 페이지 응답 DTO 로 변환된다")
        void search_success() {
            given(recruitmentRepository.search("백엔드", RecruitmentPosition.BACKEND, RecruitmentStatus.OPEN, pageable))
                    .willReturn(new PageImpl<>(List.of(createRecruitment(10L, createMember(1L, "홍길동")))));

            RecruitmentPageResponse response = recruitmentService.search(
                    "백엔드", RecruitmentPosition.BACKEND, RecruitmentStatus.OPEN, pageable);

            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.content().get(0).authorNickname()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("결과가 없으면 빈 목록을 반환한다")
        void search_empty() {
            given(recruitmentRepository.search(null, null, null, pageable)).willReturn(Page.empty());

            RecruitmentPageResponse response = recruitmentService.search(null, null, null, pageable);

            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("모집글 수정")
    class Update {

        private final RecruitmentUpdateRequest request = new RecruitmentUpdateRequest(
                1L, "제목 수정", "내용 수정", RecruitmentPosition.FRONTEND, 4, DEADLINE);

        @Test
        @DisplayName("성공 - 작성자가 수정하면 변경 내용이 반영된다")
        void update_success() {
            Recruitment recruitment = createRecruitment(10L, createMember(1L, "홍길동"));
            given(recruitmentRepository.findWithAuthorById(10L)).willReturn(Optional.of(recruitment));

            RecruitmentResponse response = recruitmentService.update(10L, request);

            assertThat(response.title()).isEqualTo("제목 수정");
            assertThat(response.position()).isEqualTo("FRONTEND");
            assertThat(recruitment.getCapacity()).isEqualTo(4);
        }

        @Test
        @DisplayName("실패 - 작성자가 아니면 RECRUITMENT_FORBIDDEN 예외가 발생하고 내용은 그대로다")
        void update_notAuthor() {
            Recruitment recruitment = createRecruitment(10L, createMember(2L, "김코딩"));
            given(recruitmentRepository.findWithAuthorById(10L)).willReturn(Optional.of(recruitment));

            assertThatThrownBy(() -> recruitmentService.update(10L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_FORBIDDEN);

            assertThat(recruitment.getTitle()).isEqualTo("백엔드 팀원 구합니다");
        }

        @Test
        @DisplayName("실패 - 마감된 모집글은 수정할 수 없다")
        void update_closed() {
            Recruitment recruitment = createRecruitment(10L, createMember(1L, "홍길동"));
            recruitment.close();
            given(recruitmentRepository.findWithAuthorById(10L)).willReturn(Optional.of(recruitment));

            assertThatThrownBy(() -> recruitmentService.update(10L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_ALREADY_CLOSED);
        }
    }

    @Nested
    @DisplayName("모집 마감")
    class Close {

        @Test
        @DisplayName("성공 - 상태가 CLOSED 로 바뀐다")
        void close_success() {
            Recruitment recruitment = createRecruitment(10L, createMember(1L, "홍길동"));
            given(recruitmentRepository.findWithAuthorById(10L)).willReturn(Optional.of(recruitment));

            RecruitmentResponse response = recruitmentService.close(10L, 1L);

            assertThat(response.status()).isEqualTo(RecruitmentStatus.CLOSED.name());
        }

        @Test
        @DisplayName("실패 - 이미 마감된 글을 다시 마감하면 예외가 발생한다")
        void close_alreadyClosed() {
            Recruitment recruitment = createRecruitment(10L, createMember(1L, "홍길동"));
            recruitment.close();
            given(recruitmentRepository.findWithAuthorById(10L)).willReturn(Optional.of(recruitment));

            assertThatThrownBy(() -> recruitmentService.close(10L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_ALREADY_CLOSED);
        }
    }

    @Nested
    @DisplayName("모집글 삭제")
    class Delete {

        @Test
        @DisplayName("성공 - 작성자가 요청하면 삭제된다")
        void delete_success() {
            Recruitment recruitment = createRecruitment(10L, createMember(1L, "홍길동"));
            given(recruitmentRepository.findWithAuthorById(10L)).willReturn(Optional.of(recruitment));

            recruitmentService.delete(10L, 1L);

            verify(applicationRepository).deleteByRecruitmentId(10L);
            verify(recruitmentRepository).delete(recruitment);
        }

        @Test
        @DisplayName("실패 - 작성자가 아니면 삭제되지 않는다")
        void delete_notAuthor() {
            Recruitment recruitment = createRecruitment(10L, createMember(2L, "김코딩"));
            given(recruitmentRepository.findWithAuthorById(10L)).willReturn(Optional.of(recruitment));

            assertThatThrownBy(() -> recruitmentService.delete(10L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECRUITMENT_FORBIDDEN);

            verify(recruitmentRepository, never()).delete(any(Recruitment.class));
        }
    }
}
