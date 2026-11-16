package kr.hs.dgsw.community.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.hs.dgsw.community.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Column(length = 255)
    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Builder
    private Member(String email, String password, String nickname, String introduction, MemberRole role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.introduction = introduction;
        this.role = role != null ? role : MemberRole.USER;
        this.status = MemberStatus.ACTIVE;
    }

    public void updateProfile(String nickname, String introduction) {
        this.nickname = nickname;
        this.introduction = introduction;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
    }
}
