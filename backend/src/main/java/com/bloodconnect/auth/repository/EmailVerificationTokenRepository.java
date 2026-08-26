package com.bloodconnect.auth.repository;

import com.bloodconnect.auth.entity.EmailVerificationToken;
import com.bloodconnect.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(
            String tokenHash
    );

    void deleteByUser(User user);
}
