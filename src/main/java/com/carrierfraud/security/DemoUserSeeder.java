package com.carrierfraud.security;

import com.carrierfraud.domain.Role;
import com.carrierfraud.domain.User;
import com.carrierfraud.infrastructure.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DemoUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String analystPassword;
    private final String compliancePassword;
    private final String clientPassword;
    private static final int MIN_PASSWORD_LENGTH = 12;

    public DemoUserSeeder(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.seed.admin-password}") String adminPassword,
                          @Value("${app.seed.analyst-password}") String analystPassword,
                          @Value("${app.seed.compliance-password}") String compliancePassword,
                          @Value("${app.seed.client-password}") String clientPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.analystPassword = analystPassword;
        this.compliancePassword = compliancePassword;
        this.clientPassword = clientPassword;
    }

    @Override
    public void run(String... args) {
        seedUser("admin", adminPassword, Role.ADMIN);
        seedUser("analyst", analystPassword, Role.ANALYST);
        seedUser("compliance", compliancePassword, Role.COMPLIANCE);
        seedUser("client1", clientPassword, Role.CLIENT);
    }

    private void seedUser(String username, String rawPassword, Role role) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "Seed password for " + username + " is missing or shorten than " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = new User(username, passwordEncoder.encode(rawPassword), role);
        userRepository.save(user);
        log.info("Seeded demo user: username={} role={}", username, role);
    }
}