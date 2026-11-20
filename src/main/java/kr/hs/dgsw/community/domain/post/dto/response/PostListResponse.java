package kr.hs.dgsw.community.domain.post.dto.response;

import java.time.LocalDateTime;

public record PostListResponse(
        Long id,
        Long authorId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
