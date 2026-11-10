package kr.hs.dgsw.community.domain.recruitment.repository;

import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentPosition;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    Page<Recruitment> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Recruitment> findByTitleContainingIgnoreCaseAndPosition(
            String title, RecruitmentPosition position, Pageable pageable);

    Page<Recruitment> findByTitleContainingIgnoreCaseAndStatus(
            String title, RecruitmentStatus status, Pageable pageable);

    Page<Recruitment> findByTitleContainingIgnoreCaseAndPositionAndStatus(
            String title, RecruitmentPosition position, RecruitmentStatus status, Pageable pageable);

    long countByAuthorIdAndStatus(Long authorId, RecruitmentStatus status);
}
