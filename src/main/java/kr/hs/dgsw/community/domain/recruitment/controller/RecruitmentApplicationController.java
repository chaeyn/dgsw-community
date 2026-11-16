package kr.hs.dgsw.community.domain.recruitment.controller;

import jakarta.validation.Valid;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentApplicationDecisionRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentApplicationResponse;
import kr.hs.dgsw.community.domain.recruitment.service.RecruitmentApplicationService;
import kr.hs.dgsw.community.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitments/{recruitmentId}/applications")
public class RecruitmentApplicationController {

    private final RecruitmentApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecruitmentApplicationResponse>> apply(
            @PathVariable Long recruitmentId,
            @Valid @RequestBody RecruitmentApplicationCreateRequest request) {
        RecruitmentApplicationResponse response = applicationService.apply(recruitmentId, request);
        URI location = URI.create("/api/recruitments/" + recruitmentId + "/applications/" + response.id());
        return ResponseEntity.created(location)
                .body(ApiResponse.of("지원이 완료되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecruitmentApplicationResponse>>> findAll(
            @PathVariable Long recruitmentId,
            @RequestParam Long memberId) {
        return ResponseEntity.ok(ApiResponse.of("지원자 목록 조회에 성공했습니다.",
                applicationService.findAll(recruitmentId, memberId)));
    }

    @PutMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<RecruitmentApplicationResponse>> decide(
            @PathVariable Long recruitmentId,
            @PathVariable Long applicationId,
            @Valid @RequestBody RecruitmentApplicationDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.of("지원 결과가 반영되었습니다.",
                applicationService.decide(recruitmentId, applicationId, request)));
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long recruitmentId,
            @PathVariable Long applicationId,
            @RequestParam Long memberId) {
        applicationService.cancel(recruitmentId, applicationId, memberId);
        return ResponseEntity.noContent().build();
    }
}
