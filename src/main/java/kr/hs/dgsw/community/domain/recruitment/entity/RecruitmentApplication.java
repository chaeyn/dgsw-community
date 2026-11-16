package kr.hs.dgsw.community.domain.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "recruitment_application",
        uniqueConstraints = @UniqueConstraint(columnNames = {"recruitment_id", "member_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member applicant;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecruitmentApplicationStatus status;

    @Builder
    private RecruitmentApplication(Recruitment recruitment, Member applicant, String message) {
        this.recruitment = recruitment;
        this.applicant = applicant;
        this.message = message;
        this.status = RecruitmentApplicationStatus.PENDING;
    }

    public void accept() {
        this.status = RecruitmentApplicationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = RecruitmentApplicationStatus.REJECTED;
    }

    public boolean isPending() {
        return this.status == RecruitmentApplicationStatus.PENDING;
    }

    public boolean isAppliedBy(Long memberId) {
        return this.applicant.getId().equals(memberId);
    }
}
