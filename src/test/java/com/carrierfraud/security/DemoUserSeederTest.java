package com.carrierfraud.security;

import com.carrierfraud.domain.User;
import com.carrierfraud.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoUserSeederTest {

    private static final String STRONG_ADMIN = "AdminPass1234!";
    private static final String STRONG_ANALYST = "AnalystPass1234!";
    private static final String STRONG_COMPLIANCE = "CompliancePass1234!";
    private static final String STRONG_CLIENT = "ClientPass1234!";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void run_seedsAllThreeUsersWhenMissing() {
        DemoUserSeeder seeder = new DemoUserSeeder(
                userRepository, passwordEncoder, STRONG_ADMIN, STRONG_ANALYST, STRONG_COMPLIANCE, STRONG_CLIENT);

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        seeder.run();

        verify(userRepository, times(4)).save(any(User.class));
    }

    @Test
    void run_skipsExistingUsers() {
        DemoUserSeeder seeder = new DemoUserSeeder(
                userRepository, passwordEncoder, STRONG_ADMIN, STRONG_ANALYST, STRONG_COMPLIANCE, STRONG_CLIENT);

        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        seeder.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_throwsOnShortAdminPassword() {
        DemoUserSeeder seeder = new DemoUserSeeder(
                userRepository, passwordEncoder, "short", STRONG_ANALYST, STRONG_COMPLIANCE, STRONG_CLIENT);

        assertThatThrownBy(seeder::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("admin");
    }

    @Test
    void run_throwsOnNullAnalystPassword() {
        DemoUserSeeder seeder = new DemoUserSeeder(
                userRepository, passwordEncoder, STRONG_ADMIN, null, STRONG_COMPLIANCE, STRONG_CLIENT);

        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(seeder::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("analyst");
    }
}