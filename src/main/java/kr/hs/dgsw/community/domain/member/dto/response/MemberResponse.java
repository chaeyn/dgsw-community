package kr.hs.dgsw.community.domain.member.dto.response;

import kr.hs.dgsw.community.domain.member.entity.Member;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String email,
        String nickname,
        String introduction,
        String role,
        String status,
        LocalDateTime createdAt
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getIntroduction(),
                member.getRole().name(),
                member.getStatus().name(),
                member.getCreatedAt()
        );
    }
}
