package com.zpantry.user.service;

import com.zpantry.user.domain.ApplicationRole;
import com.zpantry.user.domain.UserEntity;
import com.zpantry.user.persistence.UserRepository;
import com.zpantry.user.security.AuthenticatedUser;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAdministrationService {
    private final UserRepository users;

    public AccountAdministrationService(UserRepository users) { this.users = users; }

    @Transactional
    public void changeRole(AuthenticatedUser actor, UUID targetId, String requestedRole) {
        var target = users.findByIdAndDeletedFalse(targetId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        var actorRole = highestRole(actor);
        var targetRole = target.applicationRole();
        var next = ApplicationRole.fromPersisted(requestedRole);
        if (!mayManage(actorRole, targetRole) || !mayAssign(actorRole, next)) {
            throw new SecurityException("Role change is not permitted.");
        }
        target.changeRole(next, Instant.now());
    }

    private static ApplicationRole highestRole(AuthenticatedUser actor) {
        return actor.roles().stream().map(ApplicationRole::fromPersisted)
                .min(java.util.Comparator.comparingInt(AccountAdministrationService::rank))
                .orElse(ApplicationRole.USER);
    }

    private static boolean mayManage(ApplicationRole actor, ApplicationRole target) {
        return switch (actor) {
            case SUPER_ADMIN -> true;
            case ADMIN -> target == ApplicationRole.MANAGER || target == ApplicationRole.USER;
            default -> false;
        };
    }

    private static boolean mayAssign(ApplicationRole actor, ApplicationRole requested) {
        return switch (actor) {
            case SUPER_ADMIN -> true;
            case ADMIN -> requested == ApplicationRole.MANAGER || requested == ApplicationRole.USER;
            default -> false;
        };
    }

    private static int rank(ApplicationRole role) {
        return switch (role) { case SUPER_ADMIN -> 0; case ADMIN -> 1; case MANAGER -> 2; case USER -> 3; };
    }
}
