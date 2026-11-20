package kr.hs.dgsw.community.domain.post.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PostPageResponse(
        List<PostListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static PostPageResponse from(Page<PostListResponse> page) {
        return new PostPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
