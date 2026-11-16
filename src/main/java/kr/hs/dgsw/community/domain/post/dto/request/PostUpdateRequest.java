package kr.hs.dgsw.community.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest(

        @NotNull(message = "작성자 ID는 필수입니다.")
        @Positive(message = "작성자 ID는 양수여야 합니다.")
        Long authorId,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 120, message = "제목은 120자 이하여야 합니다.")
        String title,

        @NotBlank(message = "본문은 필수입니다.")
        @Size(max = 10000, message = "본문은 10000자 이하여야 합니다.")
        String content
) {
}
