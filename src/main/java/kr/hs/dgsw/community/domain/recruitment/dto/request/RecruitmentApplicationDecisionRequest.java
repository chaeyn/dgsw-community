package kr.hs.dgsw.community.domain.recruitment.dto.request;

import jakarta.validation.constraints.NotNull;

public record RecruitmentApplicationDecisionRequest(

        @NotNull(message = "수락 여부는 필수입니다.")
        Boolean accepted
) {
}
