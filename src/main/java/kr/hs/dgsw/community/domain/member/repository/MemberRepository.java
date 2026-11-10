package kr.hs.dgsw.community.domain.member.repository;

import kr.hs.dgsw.community.domain.member.entity.Member;
import kr.hs.dgsw.community.domain.member.entity.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {


    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<Member> findByIdAndStatus(Long id, MemberStatus status);

    @Query("""
            select m from Member m
            where m.status = :status
              and (:keyword is null or lower(m.nickname) like lower(concat('%', :keyword, '%')))
            """)
    Page<Member> searchByNickname(@Param("keyword") String keyword,
                                  @Param("status") MemberStatus status,
                                  Pageable pageable);

    @Query("select m from Member m where m.email = :email and m.status = :status")
    Optional<Member> findActiveByEmail(@Param("email") String email,
                                       @Param("status") MemberStatus status);

    @Query("select count(m) from Member m where m.status = :status")
    long countActiveMembers(@Param("status") MemberStatus status);
}
