package kr.hs.dgsw.community.domain.recruitment.service;

import kr.hs.dgsw.community.domain.member.entity.Member;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentServiceImpl implements RecruitmentService {

    // 한 회원이 동시에 열어둘 수 있는 모집글 수
    private static final int MAX_OPEN_RECRUITMENTS = 3;

    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentApplicationRepository applicationRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public RecruitmentResponse create(RecruitmentCreateRequest request) {
        Member author = memberRepository.findByIdAndStatus(request.memberId(), MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        long openCount = recruitmentRepository.countByAuthorIdAndStatus(author.getId(), RecruitmentStatus.OPEN);
        if (openCount >= MAX_OPEN_RECRUITMENTS) {
            throw new BusinessException(ErrorCode.RECRUITMENT_LIMIT_EXCEEDED);
        }

        Recruitment recruitment = Recruitment.builder()
                .author(author)
                .title(request.title())
                .content(request.content())
                .position(request.position())
                .capacity(request.capacity())
                .deadline(request.deadline())
                .build();

        Recruitment saved = recruitmentRepository.save(recruitment);
        log.info("모집글 등록: id={}, authorId={}", saved.getId(), author.getId());
        return RecruitmentResponse.from(saved);
    }

    @Override
    public RecruitmentResponse findById(Long id) {
        return RecruitmentResponse.from(getRecruitment(id));
    }

    @Override
    public RecruitmentPageResponse search(String keyword, RecruitmentPosition position,
                                          RecruitmentStatus status, Pageable pageable) {
        return RecruitmentPageResponse.from(
                recruitmentRepository.search(keyword, position, status, pageable)
                        .map(RecruitmentResponse::from)
        );
    }

    @Override
    @Transactional
    public RecruitmentResponse update(Long id, RecruitmentUpdateRequest request) {
        Recruitment recruitment = getRecruitment(id);
        validateAuthor(recruitment, request.memberId());
        validateNotClosed(recruitment);

        recruitment.update(request.title(), request.content(), request.position(),
                request.capacity(), request.deadline());
        return RecruitmentResponse.from(recruitment);
    }

    @Override
    @Transactional
    public RecruitmentResponse close(Long id, Long memberId) {
        Recruitment recruitment = getRecruitment(id);
        validateAuthor(recruitment, memberId);
        validateNotClosed(recruitment);

        recruitment.close();
        return RecruitmentResponse.from(recruitment);
    }

    @Override
    @Transactional
    public void delete(Long id, Long memberId) {
        Recruitment recruitment = getRecruitment(id);
        validateAuthor(recruitment, memberId);

        applicationRepository.deleteByRecruitmentId(id);
        recruitmentRepository.delete(recruitment);
        log.info("모집글 삭제: id={}", id);
    }

    private Recruitment getRecruitment(Long id) {
        return recruitmentRepository.findWithAuthorById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_NOT_FOUND));
    }

    private void validateAuthor(Recruitment recruitment, Long memberId) {
        if (!recruitment.isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.RECRUITMENT_FORBIDDEN);
        }
    }

    private void validateNotClosed(Recruitment recruitment) {
        if (recruitment.isClosed()) {
            throw new BusinessException(ErrorCode.RECRUITMENT_ALREADY_CLOSED);
        }
    }
}
