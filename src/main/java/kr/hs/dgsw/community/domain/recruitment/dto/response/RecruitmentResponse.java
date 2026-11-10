package kr.hs.dgsw.community.domain.recruitment.dto.response;

import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RecruitmentResponse(
        Long id,
        Long authorId,
        String authorNickname,
        String title,
        String content,
        String position,
        int capacity,
        LocalDate deadline,
        String status,
        LocalDateTime createdAt
) {
    public static RecruitmentResponse from(Recruitment recruitment) {
        return new RecruitmentResponse(
                recruitment.getId(),
                recruitment.getAuthor().getId(),
                recruitment.getAuthor().getNickname(),
                recruitment.getTitle(),
                recruitment.getContent(),
                recruitment.getPosition().name(),
                recruitment.getCapacity(),
                recruitment.getDeadline(),
                recruitment.getStatus().name(),
                recruitment.getCreatedAt()
        );
    }
}
