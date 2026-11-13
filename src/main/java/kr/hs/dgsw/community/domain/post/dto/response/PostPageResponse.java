package kr.hs.dgsw.community.domain.post.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PostPageResponse(
        List<PostResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static PostPageResponse from(Page<PostResponse> page) {
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
