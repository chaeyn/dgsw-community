package kr.hs.dgsw.community.domain.recruitment.dto.request;

import jakarta.validation.constraints.Size;

public record RecruitmentApplicationCreateRequest(

        @Size(max = 500, message = "지원 메시지는 500자 이하여야 합니다.")
        String message
) {
}
