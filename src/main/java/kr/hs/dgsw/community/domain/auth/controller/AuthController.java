package kr.hs.dgsw.community.domain.auth.controller;

import jakarta.validation.Valid;
import kr.hs.dgsw.community.domain.auth.dto.request.LoginRequest;
import kr.hs.dgsw.community.domain.auth.dto.request.TokenReissueRequest;
import kr.hs.dgsw.community.domain.auth.dto.response.TokenResponse;
import kr.hs.dgsw.community.domain.auth.service.AuthService;
import kr.hs.dgsw.community.domain.member.dto.response.MemberResponse;
import kr.hs.dgsw.community.global.response.ApiResponse;
import kr.hs.dgsw.community.global.security.BearerTokenResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.of("로그인에 성공했습니다.", authService.login(request)));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @Valid @RequestBody TokenReissueRequest request) {
        return ResponseEntity.ok(ApiResponse.of("토큰이 재발급되었습니다.", authService.reissue(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        MemberResponse response = authService.getMe(BearerTokenResolver.resolve(authorization));
        return ResponseEntity.ok(ApiResponse.of("내 정보 조회에 성공했습니다.", response));
    }

    @DeleteMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(BearerTokenResolver.resolve(authorization));
        return ResponseEntity.ok(ApiResponse.of("로그아웃되었습니다."));
    }
}
