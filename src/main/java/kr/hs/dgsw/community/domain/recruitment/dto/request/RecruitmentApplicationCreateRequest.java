package kr.hs.dgsw.community.domain.recruitment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecruitmentApplicationCreateRequest(

        @NotNull(message = "지원자 id는 필수입니다.")
        Long memberId,

        @Size(max = 500, message = "지원 메시지는 500자 이하여야 합니다.")
        String message
) {
}
