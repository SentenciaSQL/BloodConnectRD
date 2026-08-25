package com.bloodconnect.auth.repository;

import com.bloodconnect.auth.entity.PasswordResetToken;
import com.bloodconnect.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);
}
