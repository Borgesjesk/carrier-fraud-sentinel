package com.carrierfraud.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String passwordHash;

    private Role role;

    private boolean enabled;

    private boolean accountLocked;

    private Instant createdAt;

    public User(String username, String passwordHash, Role role) {
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
        this.enabled = true;
        this.accountLocked = false;
        this.createdAt = Instant.now();
    }
}