package com.hospital.auth.config;

import com.hospital.auth.entity.UserAccount;
import com.hospital.auth.enums.Role;
import com.hospital.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userAccountRepository.existsByEmail("admin@hospital.ma")) {
            UserAccount admin = UserAccount.builder()
                    .email("admin@hospital.ma")
                    .password(passwordEncoder.encode("Admin@2026"))
                    .role(Role.ADMIN)
                    .nom("Admin")
                    .prenom("Système")
                    .enabled(true)
                    .emailVerified(true)
                    .build();
            userAccountRepository.save(admin);
            log.info("=== Compte admin créé: admin@hospital.ma / Admin@2026 ===");
        }
    }
}
