package com.zpantry.user.service;

import com.zpantry.user.domain.ApplicationRole;
import com.zpantry.user.domain.UserEntity;
import com.zpantry.user.persistence.UserRepository;
import java.util.regex.Pattern;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates exactly one deployment-configured top-level account on an empty installation. */
@Component
@ConditionalOnProperty(name = "zpantry.admin.bootstrap.enabled", havingValue = "true")
public class SuperAdminBootstrap implements ApplicationRunner {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final UserRepository users;
    private final LegacyPasswordHasher passwords;
    private final String email;
    private final String password;

    public SuperAdminBootstrap(UserRepository users, LegacyPasswordHasher passwords,
            @Value("${ADMIN_EMAIL:}") String email, @Value("${ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.passwords = passwords;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRoleIgnoreCase(ApplicationRole.SUPER_ADMIN.name())) return;
        if (!EMAIL.matcher(email).matches() || password == null || password.isBlank()) {
            throw new IllegalStateException("Initial SUPER_ADMIN bootstrap requires valid ADMIN_EMAIL and ADMIN_PASSWORD");
        }
        users.save(UserEntity.bootstrapSuperAdmin(email, passwords.hash(password)));
    }
}
