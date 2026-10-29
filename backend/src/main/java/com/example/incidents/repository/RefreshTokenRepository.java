package com.example.incidents.repository;

import com.example.incidents.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :hash")
    Optional<RefreshToken> findByTokenHashWithUser(@Param("hash") String hash);

    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken t set t.revoked = true where t.user.id = :userId and t.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);

    /** Les tokens révoqués sont conservés jusqu'à leur expiration pour détecter la réutilisation d'un ancien token. */
    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken t where t.user.id = :userId and t.expiresAt < :now")
    int deleteExpiredByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
