package com.medsys.auth.repository;

import com.medsys.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByRefreshToken(String refreshToken);
    Optional<UserAccount> findByResetToken(String resetToken);
    Optional<UserAccount> findByEmailVerificationToken(String token);
    boolean existsByEmail(String email);
    boolean existsByCin(String cin);
}
