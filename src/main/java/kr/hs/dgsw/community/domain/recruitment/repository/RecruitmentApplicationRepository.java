package kr.hs.dgsw.community.domain.recruitment.repository;

import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentApplication;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecruitmentApplicationRepository extends JpaRepository<RecruitmentApplication, Long> {

    boolean existsByRecruitmentIdAndApplicantId(Long recruitmentId, Long applicantId);

    @Query("""
            select a from RecruitmentApplication a
            join fetch a.applicant
            where a.id = :id and a.recruitment.id = :recruitmentId
            """)
    Optional<RecruitmentApplication> findByIdAndRecruitmentId(@Param("id") Long id,
                                                              @Param("recruitmentId") Long recruitmentId);

    @Query("""
            select a from RecruitmentApplication a
            join fetch a.applicant
            where a.recruitment.id = :recruitmentId
            order by a.createdAt asc
            """)
    List<RecruitmentApplication> findByRecruitmentIdOrderByCreatedAtAsc(@Param("recruitmentId") Long recruitmentId);

    @Query("""
            select count(a) from RecruitmentApplication a
            where a.recruitment.id = :recruitmentId and a.status = :status
            """)
    long countByRecruitmentIdAndStatus(@Param("recruitmentId") Long recruitmentId,
                                       @Param("status") RecruitmentApplicationStatus status);

    @Modifying
    @Query("delete from RecruitmentApplication a where a.recruitment.id = :recruitmentId")
    void deleteByRecruitmentId(@Param("recruitmentId") Long recruitmentId);
}
