package com.carrierfraud.security;

import com.carrierfraud.domain.Role;
import com.carrierfraud.domain.User;
import com.carrierfraud.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    private static final String USERNAME = "alice";
    private static final String PASSWORD_HASH = "$2a$12$hashedPassword";

    @Mock private UserRepository userRepository;

    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    void loadUserByUsername_returnsUserDetailsWhenFound() {
        User user = new User(USERNAME, PASSWORD_HASH, Role.ANALYST);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(USERNAME);

        assertThat(details.getUsername()).isEqualTo(USERNAME);
        assertThat(details.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_ANALYST");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void loadUserByUsername_throwsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}