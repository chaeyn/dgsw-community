package kr.hs.dgsw.community.domain.member.repository;

import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberRole;
import kr.hs.dgsw.community.domain.member.entity.MemberStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("MemberRepository JPQL / Query Method 테스트")
@TestPropertySource(properties = "spring.sql.init.mode=never")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.saveAll(java.util.List.of(
                member("hong@dgsw.hs.kr", "홍길동"),
                member("kim@dgsw.hs.kr", "김코딩"),
                member("choi@dgsw.hs.kr", "최코딩")
        ));

        Member withdrawn = member("ghost@dgsw.hs.kr", "탈퇴한회원");
        withdrawn.withdraw();
        memberRepository.save(withdrawn);
    }

    private Member member(String email, String nickname) {
        return Member.builder()
                .email(email)
                .password("hashed")
                .nickname(nickname)
                .introduction("소개")
                .role(MemberRole.USER)
                .build();
    }

    @Test
    @DisplayName("JPQL - 닉네임 키워드로 활동 중인 회원만 검색된다")
    void searchByNickname_withKeyword() {
        Page<Member> result = memberRepository.searchByNickname(
                "코딩", MemberStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Member::getNickname)
                .containsExactlyInAnyOrder("김코딩", "최코딩");
    }

    @Test
    @DisplayName("JPQL - 키워드가 null 이면 활동 중인 전체 회원이 조회되고 탈퇴 회원은 제외된다")
    void searchByNickname_withoutKeyword() {
        Page<Member> result = memberRepository.searchByNickname(
                null, MemberStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(Member::getNickname)
                .doesNotContain("탈퇴한회원");
    }

    @Test
    @DisplayName("JPQL - 탈퇴한 회원은 이메일로 조회되지 않는다 (실패 케이스)")
    void findActiveByEmail_withdrawn() {
        Optional<Member> result =
                memberRepository.findActiveByEmail("ghost@dgsw.hs.kr", MemberStatus.ACTIVE);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("JPQL - 활동 중인 회원 수를 집계한다")
    void countActiveMembers() {
        assertThat(memberRepository.countActiveMembers(MemberStatus.ACTIVE)).isEqualTo(3);
    }

    @Test
    @DisplayName("Query Method - 이메일/닉네임 중복 여부를 확인한다")
    void existsBy() {
        assertThat(memberRepository.existsByEmail("hong@dgsw.hs.kr")).isTrue();
        assertThat(memberRepository.existsByEmail("none@dgsw.hs.kr")).isFalse();
        assertThat(memberRepository.existsByNickname("김코딩")).isTrue();
    }

    @Test
    @DisplayName("Auditing - 저장 시 생성/수정 시각이 자동으로 채워진다")
    void auditing() {
        Member saved = memberRepository.save(member("audit@dgsw.hs.kr", "감사테스트"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
