package kr.hs.dgsw.community.domain.recruitment.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;

import java.time.LocalDate;

public record RecruitmentCreateRequest(

        @NotNull(message = "작성자 id는 필수입니다.")
        Long memberId,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
        String content,

        @NotNull(message = "모집 분야는 필수입니다.")
        RecruitmentPosition position,

        @NotNull(message = "모집 인원은 필수입니다.")
        @Min(value = 1, message = "모집 인원은 1명 이상이어야 합니다.")
        @Max(value = 10, message = "모집 인원은 10명 이하여야 합니다.")
        Integer capacity,

        @NotNull(message = "마감일은 필수입니다.")
        @FutureOrPresent(message = "마감일은 오늘 이후여야 합니다.")
        LocalDate deadline
) {
}
