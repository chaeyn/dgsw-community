package kr.hs.dgsw.community.domain.recruitment.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record RecruitmentPageResponse(
        List<RecruitmentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static RecruitmentPageResponse from(Page<RecruitmentResponse> page) {
        return new RecruitmentPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
