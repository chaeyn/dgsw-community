package kr.hs.dgsw.community.domain.auth.service;

import kr.hs.dgsw.community.domain.auth.dto.request.LoginRequest;
import kr.hs.dgsw.community.domain.auth.dto.request.TokenReissueRequest;
import kr.hs.dgsw.community.domain.auth.dto.response.TokenResponse;
import kr.hs.dgsw.community.domain.member.dto.response.MemberResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);

    TokenResponse reissue(TokenReissueRequest request);

    void logout(String accessToken);

    MemberResponse getMe(String accessToken);
}
