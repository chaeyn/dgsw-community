package kr.hs.dgsw.community.domain.recruitment.repository;

import kr.hs.dgsw.community.domain.recruitment.entity.Recruitment;
import kr.hs.dgsw.community.domain.recruitment.entity.RecruitmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long>, RecruitmentRepositoryCustom {

    @Query("select r from Recruitment r join fetch r.author where r.id = :id")
    Optional<Recruitment> findWithAuthorById(@Param("id") Long id);

    @Query("select count(r) from Recruitment r where r.author.id = :authorId and r.status = :status")
    long countByAuthorIdAndStatus(@Param("authorId") Long authorId,
                                  @Param("status") RecruitmentStatus status);
}
