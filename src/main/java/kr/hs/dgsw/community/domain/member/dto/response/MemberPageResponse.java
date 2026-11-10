package kr.hs.dgsw.community.domain.member.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;


public record MemberPageResponse(
        List<MemberResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static MemberPageResponse from(Page<MemberResponse> page) {
        return new MemberPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
