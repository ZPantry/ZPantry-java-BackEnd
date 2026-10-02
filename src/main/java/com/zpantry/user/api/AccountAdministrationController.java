package com.zpantry.user.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.user.security.AuthenticatedUserResolver;
import com.zpantry.user.service.AccountAdministrationService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AccountAdministrationController {
    private final AccountAdministrationService service;
    private final AuthenticatedUserResolver identities;
    public AccountAdministrationController(AccountAdministrationService service, AuthenticatedUserResolver identities) {
        this.service = service; this.identities = identities;
    }
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<Object>> changeRole(@PathVariable UUID id, @RequestBody RoleChangeRequest request, Authentication authentication) {
        try {
            service.changeRole(identities.resolve(authentication).orElseThrow(SecurityException::new), id, request.role());
            return ResponseEntity.ok(new ApiResponse<>(true, "Role updated.", null, null, "", Instant.now()));
        } catch (SecurityException exception) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden.", null, null, "", Instant.now()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, exception.getMessage(), null, null, "", Instant.now()));
        }
    }
    public record RoleChangeRequest(String role) { }
}
