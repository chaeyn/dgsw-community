package kr.hs.dgsw.community.domain.member.service;

import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import kr.hs.dgsw.community.domain.member.dto.request.MemberCreateRequest;
import kr.hs.dgsw.community.domain.member.dto.request.MemberUpdateRequest;
import kr.hs.dgsw.community.domain.member.dto.request.PasswordChangeRequest;
import kr.hs.dgsw.community.domain.member.dto.response.MemberPageResponse;
import kr.hs.dgsw.community.domain.member.dto.response.MemberResponse;
import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberRole;
import kr.hs.dgsw.community.domain.member.entity.MemberStatus;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public MemberResponse create(MemberCreateRequest request) {
        validateEmailNotDuplicated(request.email());
        validateNicknameNotDuplicated(request.nickname());

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .introduction(request.introduction())
                .role(MemberRole.USER)
                .build();

        Member saved = memberRepository.save(member);
        log.info("회원 가입 완료: id={}, email={}", saved.getId(), saved.getEmail());
        return MemberResponse.from(saved);
    }

    @Override
    public MemberResponse findById(Long id) {
        return MemberResponse.from(getActiveMember(id));
    }

    @Override
    public MemberResponse findByEmail(String email) {
        Member member = memberRepository.findActiveByEmail(email, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }

    @Override
    public MemberPageResponse search(String keyword, Pageable pageable) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return MemberPageResponse.from(
                memberRepository.searchByNickname(normalized, MemberStatus.ACTIVE, pageable)
                        .map(MemberResponse::from)
        );
    }

    @Override
    public long countActiveMembers() {
        return memberRepository.countActiveMembers(MemberStatus.ACTIVE);
    }

    @Override
    @Transactional
    public MemberResponse update(Long id, MemberUpdateRequest request) {
        Member member = getActiveMember(id);

        if (!member.getNickname().equals(request.nickname())) {
            validateNicknameNotDuplicated(request.nickname());
        }
        member.updateProfile(request.nickname(), request.introduction());
        return MemberResponse.from(member);
    }

    @Override
    @Transactional
    public void changePassword(Long id, PasswordChangeRequest request) {
        Member member = getActiveMember(id);

        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_CHANGED);
        }

        member.changePassword(passwordEncoder.encode(request.newPassword()));
        log.info("비밀번호 변경 완료: id={}", id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Member member = getActiveMember(id);
        member.withdraw();
        log.info("회원 탈퇴 처리: id={}", id);
    }

    private Member getActiveMember(Long id) {
        return memberRepository.findByIdAndStatus(id, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateEmailNotDuplicated(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateNicknameNotDuplicated(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
