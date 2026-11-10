package kr.hs.dgsw.community.domain.member.service;

import kr.hs.dgsw.community.domain.member.dto.request.MemberCreateRequest;
import kr.hs.dgsw.community.domain.member.dto.request.MemberUpdateRequest;
import kr.hs.dgsw.community.domain.member.dto.request.PasswordChangeRequest;
import kr.hs.dgsw.community.domain.member.dto.response.MemberPageResponse;
import kr.hs.dgsw.community.domain.member.dto.response.MemberResponse;
import org.springframework.data.domain.Pageable;

public interface MemberService {

    MemberResponse create(MemberCreateRequest request);

    MemberResponse findById(Long id);

    MemberResponse findByEmail(String email);

    MemberPageResponse search(String keyword, Pageable pageable);

    long countActiveMembers();

    MemberResponse update(Long id, MemberUpdateRequest request);

    void changePassword(Long id, PasswordChangeRequest request);

    void delete(Long id);
}
