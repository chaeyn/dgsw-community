package kr.hs.dgsw.community.domain.member.controller;

import jakarta.validation.Valid;
import kr.hs.dgsw.community.global.response.ApiResponse;
import kr.hs.dgsw.community.domain.member.dto.request.MemberCreateRequest;
import kr.hs.dgsw.community.domain.member.dto.request.MemberUpdateRequest;
import kr.hs.dgsw.community.domain.member.dto.request.PasswordChangeRequest;
import kr.hs.dgsw.community.domain.member.dto.response.MemberPageResponse;
import kr.hs.dgsw.community.domain.member.dto.response.MemberResponse;
import kr.hs.dgsw.community.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> create(
            @Valid @RequestBody MemberCreateRequest request) {
        MemberResponse response = memberService.create(request);
        return ResponseEntity.created(URI.create("/api/members/" + response.id()))
                .body(ApiResponse.of("회원 가입이 완료되었습니다.", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of("회원 조회에 성공했습니다.", memberService.findById(id)));
    }

    @GetMapping("/search/email")
    public ResponseEntity<ApiResponse<MemberResponse>> findByEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.of("회원 조회에 성공했습니다.", memberService.findByEmail(email)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MemberPageResponse>> search(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.of("회원 목록 조회에 성공했습니다.", memberService.search(keyword, pageable)));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countActiveMembers() {
        return ResponseEntity.ok(
                ApiResponse.of("활동 회원 수 조회에 성공했습니다.", memberService.countActiveMembers()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody MemberUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.of("회원 정보가 수정되었습니다.", memberService.update(id, request)));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordChangeRequest request) {
        memberService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.of("비밀번호가 변경되었습니다."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
