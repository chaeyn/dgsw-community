package kr.hs.dgsw.community.domain.post.dto.response;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        Long authorId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
