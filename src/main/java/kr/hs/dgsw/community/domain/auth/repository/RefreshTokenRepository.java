package kr.hs.dgsw.community.domain.auth.repository;

import kr.hs.dgsw.community.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying(clearAutomatically = true)
    @Query("delete from RefreshToken rt where rt.memberId = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("delete from RefreshToken rt where rt.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
