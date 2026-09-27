package com.zpantry.user.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.user.security.AuthenticatedUserResolver;
import com.zpantry.user.service.OwnerAuthorizationException;
import com.zpantry.user.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/profile")
public class UserProfileController {
    
    private final UserProfileService service;
    private final AuthenticatedUserResolver identities;

    public UserProfileController(UserProfileService service, AuthenticatedUserResolver identities) {
        this.service = service;
        this.identities = identities;
    }

    @GetMapping
    public ApiResponse<UserProfileResponse> get(@PathVariable UUID userId, Authentication authentication) {
        var identity = identities.resolve(authentication).orElseThrow(OwnerAuthorizationException::new);
        return service.get(userId, identity);
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> update(
            @PathVariable UUID userId,
            @RequestBody UserProfileUpdateRequest request, 
            Authentication authentication) {
        var identity = identities.resolve(authentication).orElseThrow(OwnerAuthorizationException::new);
        var response = service.update(userId, request, identity);
        return ResponseEntity.status(response.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(response);
    }
}
