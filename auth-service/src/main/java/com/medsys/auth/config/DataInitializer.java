package com.medsys.auth.config;

import com.medsys.auth.entity.UserAccount;
import com.medsys.auth.enums.Role;
import com.medsys.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepo.existsByEmail("admin@medsys.ma")) {
            UserAccount admin = UserAccount.builder()
                    .email("admin@medsys.ma")
                    .password(passwordEncoder.encode("Admin@2026"))
                    .role(Role.ADMIN)
                    .nom("Admin")
                    .prenom("Système")
                    .cin("ADMIN001")
                    .enabled(true)
                    .emailVerified(true)
                    .build();
            userRepo.save(admin);
            log.info("[INIT] Admin account created: admin@medsys.ma / Admin@2026");
        }

        if (!userRepo.existsByEmail("directeur@medsys.ma")) {
            UserAccount directeur = UserAccount.builder()
                    .email("directeur@medsys.ma")
                    .password(passwordEncoder.encode("Directeur@2026"))
                    .role(Role.DIRECTEUR)
                    .nom("Directeur")
                    .prenom("Hôpital")
                    .cin("DIR001")
                    .enabled(true)
                    .emailVerified(true)
                    .build();
            userRepo.save(directeur);
            log.info("[INIT] Directeur account created: directeur@medsys.ma / Directeur@2026");
        }
    }
}
