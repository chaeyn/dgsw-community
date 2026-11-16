package kr.hs.dgsw.community.domain.recruitment.service;

import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberStatus;
import kr.hs.dgsw.community.domain.member.repository.MemberRepository;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationDecisionRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentApplicationResponse;
import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentApplication;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentApplicationStatus;
import kr.hs.dgsw.community.domain.recruitment.repository.RecruitmentApplicationRepository;
import kr.hs.dgsw.community.domain.recruitment.repository.RecruitmentRepository;
import kr.hs.dgsw.community.global.error.BusinessException;
import kr.hs.dgsw.community.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentApplicationServiceImpl implements RecruitmentApplicationService {

    private final RecruitmentApplicationRepository applicationRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public RecruitmentApplicationResponse apply(Long recruitmentId, RecruitmentApplicationCreateRequest request) {
        Recruitment recruitment = getRecruitment(recruitmentId);
        validateOpen(recruitment);

        Member applicant = memberRepository.findByIdAndStatus(request.memberId(), MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (recruitment.isWrittenBy(applicant.getId())) {
            throw new BusinessException(ErrorCode.APPLICATION_SELF_NOT_ALLOWED);
        }
        if (applicationRepository.existsByRecruitmentIdAndApplicantId(recruitmentId, applicant.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_APPLICATION);
        }

        RecruitmentApplication application = RecruitmentApplication.builder()
                .recruitment(recruitment)
                .applicant(applicant)
                .message(request.message())
                .build();

        RecruitmentApplication saved = applicationRepository.save(application);
        log.info("모집글 지원: recruitmentId={}, applicantId={}", recruitmentId, applicant.getId());
        return RecruitmentApplicationResponse.from(saved);
    }

    @Override
    public List<RecruitmentApplicationResponse> findAll(Long recruitmentId, Long memberId) {
        Recruitment recruitment = getRecruitment(recruitmentId);
        validateAuthor(recruitment, memberId);

        return applicationRepository.findByRecruitmentIdOrderByCreatedAtAsc(recruitmentId).stream()
                .map(RecruitmentApplicationResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public RecruitmentApplicationResponse decide(Long recruitmentId, Long applicationId,
                                                 RecruitmentApplicationDecisionRequest request) {
        Recruitment recruitment = getRecruitment(recruitmentId);
        validateAuthor(recruitment, request.memberId());

        RecruitmentApplication application = getApplication(recruitmentId, applicationId);
        if (!application.isPending()) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        if (request.accepted()) {
            accept(recruitment, application);
        } else {
            application.reject();
        }
        return RecruitmentApplicationResponse.from(application);
    }

    @Override
    @Transactional
    public void cancel(Long recruitmentId, Long applicationId, Long memberId) {
        RecruitmentApplication application = getApplication(recruitmentId, applicationId);

        if (!application.isAppliedBy(memberId)) {
            throw new BusinessException(ErrorCode.APPLICATION_FORBIDDEN);
        }
        if (!application.isPending()) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
        applicationRepository.delete(application);
    }

    private void accept(Recruitment recruitment, RecruitmentApplication application) {
        if (recruitment.isClosed()) {
            throw new BusinessException(ErrorCode.RECRUITMENT_ALREADY_CLOSED);
        }
        application.accept();

        long acceptedCount = applicationRepository.countByRecruitmentIdAndStatus(
                recruitment.getId(), RecruitmentApplicationStatus.ACCEPTED) + 1;
        if (acceptedCount >= recruitment.getCapacity()) {
            recruitment.close();
            log.info("정원이 차서 모집글 자동 마감: recruitmentId={}", recruitment.getId());
        }
    }

    private Recruitment getRecruitment(Long recruitmentId) {
        return recruitmentRepository.findWithAuthorById(recruitmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_NOT_FOUND));
    }

    private RecruitmentApplication getApplication(Long recruitmentId, Long applicationId) {
        return applicationRepository.findByIdAndRecruitmentId(applicationId, recruitmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    private void validateAuthor(Recruitment recruitment, Long memberId) {
        if (!recruitment.isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.RECRUITMENT_FORBIDDEN);
        }
    }

    private void validateOpen(Recruitment recruitment) {
        if (recruitment.isClosed() || recruitment.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.RECRUITMENT_ALREADY_CLOSED);
        }
    }
}
