package kr.hs.dgsw.community.domain.recruitment.controller;

import jakarta.validation.Valid;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentCreateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.request.RecruitmentUpdateRequest;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentPageResponse;
import kr.hs.dgsw.community.domain.recruitment.dto.response.RecruitmentResponse;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentStatus;
import kr.hs.dgsw.community.domain.recruitment.service.RecruitmentService;
import kr.hs.dgsw.community.global.response.ApiResponse;
import kr.hs.dgsw.community.global.security.BearerTokenResolver;
import kr.hs.dgsw.community.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitments")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    public ResponseEntity<ApiResponse<RecruitmentResponse>> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody RecruitmentCreateRequest request) {
        RecruitmentResponse response = recruitmentService.create(loginMemberId(authorization), request);
        return ResponseEntity.created(URI.create("/api/recruitments/" + response.id()))
                .body(ApiResponse.of("모집글이 등록되었습니다.", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecruitmentResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.of("모집글 조회에 성공했습니다.", recruitmentService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RecruitmentPageResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RecruitmentPosition position,
            @RequestParam(required = false) RecruitmentStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of("모집글 목록 조회에 성공했습니다.",
                recruitmentService.search(keyword, position, status, pageable)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecruitmentResponse>> update(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody RecruitmentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of("모집글이 수정되었습니다.",
                recruitmentService.update(id, loginMemberId(authorization), request)));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<RecruitmentResponse>> close(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return ResponseEntity.ok(ApiResponse.of("모집이 마감되었습니다.",
                recruitmentService.close(id, loginMemberId(authorization))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        recruitmentService.delete(id, loginMemberId(authorization));
        return ResponseEntity.noContent().build();
    }

    private Long loginMemberId(String authorization) {
        return jwtTokenProvider.parseMemberId(BearerTokenResolver.resolve(authorization));
    }
}
