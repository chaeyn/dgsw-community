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
import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "recruitment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruitment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member author;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruit_position", nullable = false, length = 20)
    private RecruitmentPosition position;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecruitmentStatus status;

    @Builder
    private Recruitment(Member author, String title, String content,
                        RecruitmentPosition position, int capacity, LocalDate deadline) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.position = position;
        this.capacity = capacity;
        this.deadline = deadline;
        this.status = RecruitmentStatus.OPEN;
    }

    public void update(String title, String content, RecruitmentPosition position,
                       int capacity, LocalDate deadline) {
        this.title = title;
        this.content = content;
        this.position = position;
        this.capacity = capacity;
        this.deadline = deadline;
    }

    public void close() {
        this.status = RecruitmentStatus.CLOSED;
    }

    public boolean isClosed() {
        return this.status == RecruitmentStatus.CLOSED;
    }

    public boolean isWrittenBy(Long memberId) {
        return this.author.getId().equals(memberId);
    }
}
