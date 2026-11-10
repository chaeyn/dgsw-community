package kr.hs.dgsw.community.domain.member.service;

import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import kr.hs.dgsw.community.domain.member.dto.request.MemberCreateRequest;
import kr.hs.dgsw.community.domain.member.dto.request.MemberUpdateRequest;
import kr.hs.dgsw.community.domain.member.dto.request.PasswordChangeRequest;
import kr.hs.dgsw.community.domain.member.dto.response.MemberResponse;
import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberRole;
import kr.hs.dgsw.community.domain.member.entity.MemberStatus;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("MemberService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member createMember(Long id, String email, String nickname, String password) {
        Member member = Member.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .introduction("소개")
                .role(MemberRole.USER)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Nested
    @DisplayName("회원 가입")
    class Create {

        @Test
        @DisplayName("성공 - 저장된 회원 정보가 반환되고 응답에 비밀번호가 포함되지 않는다")
        void create_success() {
            MemberCreateRequest request =
                    new MemberCreateRequest("new@dgsw.hs.kr", "password123", "신규회원", "안녕하세요");
            given(memberRepository.existsByEmail(request.email())).willReturn(false);
            given(memberRepository.existsByNickname(request.nickname())).willReturn(false);
            given(memberRepository.save(any(Member.class)))
                    .willAnswer(invocation -> {
                        Member member = invocation.getArgument(0);
                        ReflectionTestUtils.setField(member, "id", 1L);
                        return member;
                    });

            MemberResponse response = memberService.create(request);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("new@dgsw.hs.kr");
            assertThat(response.nickname()).isEqualTo("신규회원");
            assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE.name());
        }

        @Test
        @DisplayName("실패 - 이메일이 중복되면 DUPLICATE_EMAIL 예외가 발생한다")
        void create_duplicateEmail() {
            MemberCreateRequest request =
                    new MemberCreateRequest("hong@dgsw.hs.kr", "password123", "중복테스트", null);
            given(memberRepository.existsByEmail(request.email())).willReturn(true);

            assertThatThrownBy(() -> memberService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("실패 - 닉네임이 중복되면 DUPLICATE_NICKNAME 예외가 발생한다")
        void create_duplicateNickname() {
            MemberCreateRequest request =
                    new MemberCreateRequest("new@dgsw.hs.kr", "password123", "홍길동", null);
            given(memberRepository.existsByEmail(request.email())).willReturn(false);
            given(memberRepository.existsByNickname(request.nickname())).willReturn(true);

            assertThatThrownBy(() -> memberService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    @Nested
    @DisplayName("회원 조회")
    class Find {

        @Test
        @DisplayName("성공 - 활동 중인 회원을 조회한다")
        void findById_success() {
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(createMember(1L, "hong@dgsw.hs.kr", "홍길동", "password123")));

            MemberResponse response = memberService.findById(1L);

            assertThat(response.nickname()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원을 조회하면 MEMBER_NOT_FOUND 예외가 발생한다")
        void findById_notFound() {
            given(memberRepository.findByIdAndStatus(999L, MemberStatus.ACTIVE))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.findById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("회원 정보 수정")
    class Update {

        @Test
        @DisplayName("성공 - 닉네임이 그대로면 중복 검사를 수행하지 않는다")
        void update_sameNickname() {
            Member member = createMember(1L, "hong@dgsw.hs.kr", "홍길동", "password123");
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(member));

            MemberResponse response =
                    memberService.update(1L, new MemberUpdateRequest("홍길동", "소개 변경"));

            assertThat(response.introduction()).isEqualTo("소개 변경");
            verify(memberRepository, never()).existsByNickname(anyString());
        }

        @Test
        @DisplayName("실패 - 다른 회원이 사용 중인 닉네임으로 변경하면 예외가 발생한다")
        void update_duplicateNickname() {
            Member member = createMember(1L, "hong@dgsw.hs.kr", "홍길동", "password123");
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(member));
            given(memberRepository.existsByNickname("김코딩")).willReturn(true);

            assertThatThrownBy(() -> memberService.update(1L, new MemberUpdateRequest("김코딩", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("실패 - 현재 비밀번호가 일치하지 않으면 PASSWORD_MISMATCH 예외가 발생한다")
        void changePassword_mismatch() {
            Member member = createMember(1L, "hong@dgsw.hs.kr", "홍길동", "password123");
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(member));

            assertThatThrownBy(() -> memberService.changePassword(
                    1L, new PasswordChangeRequest("wrongPassword1", "newPassword1")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_MISMATCH);

            assertThat(member.getPassword()).isEqualTo("password123");
        }

        @Test
        @DisplayName("성공 - 새 비밀번호가 반영된다")
        void changePassword_success() {
            Member member = createMember(1L, "hong@dgsw.hs.kr", "홍길동", "password123");
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(member));

            memberService.changePassword(1L, new PasswordChangeRequest("password123", "newPassword1"));

            assertThat(member.getPassword()).isEqualTo("newPassword1");
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class Delete {

        @Test
        @DisplayName("성공 - 물리 삭제가 아닌 상태 변경(소프트 삭제)으로 처리된다")
        void delete_success() {
            Member member = createMember(1L, "hong@dgsw.hs.kr", "홍길동", "password123");
            given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE))
                    .willReturn(Optional.of(member));

            memberService.delete(1L);

            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
            verify(memberRepository, never()).delete(any(Member.class));
        }
    }
}
