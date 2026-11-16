package kr.hs.dgsw.community.domain.recruitment.dto.response;

import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentApplication;

import java.time.LocalDateTime;

public record RecruitmentApplicationResponse(
        Long id,
        Long recruitmentId,
        Long applicantId,
        String applicantNickname,
        String message,
        String status,
        LocalDateTime createdAt
) {
    public static RecruitmentApplicationResponse from(RecruitmentApplication application) {
        return new RecruitmentApplicationResponse(
                application.getId(),
                application.getRecruitment().getId(),
                application.getApplicant().getId(),
                application.getApplicant().getNickname(),
                application.getMessage(),
                application.getStatus().name(),
                application.getCreatedAt()
        );
    }
}
