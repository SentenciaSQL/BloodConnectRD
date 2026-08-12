package com.bloodconnect.auth.repository;

import com.bloodconnect.auth.entity.RefreshToken;
import com.bloodconnect.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken token set token.revoked = true "
            + "where token.token = :token and token.revoked = false")
    int revokeByToken(@Param("token") String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken token set token.revoked = true "
            + "where token.user = :user and token.revoked = false")
    int revokeAllByUser(@Param("user") User user);
}
