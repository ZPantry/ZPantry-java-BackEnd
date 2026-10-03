package com.zpantry.user.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.user.security.AuthenticatedUserResolver;
import com.zpantry.user.service.OwnerAuthorizationException;
import com.zpantry.user.service.ProfileV2Service;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/profile/v2")
public class ProfileV2Controller {
    private final ProfileV2Service service;
    private final AuthenticatedUserResolver users;

    public ProfileV2Controller(ProfileV2Service service, AuthenticatedUserResolver users) {
        this.service = service;
        this.users = users;
    }

    @GetMapping
    public ApiResponse<ProfileV2Dtos.Response> get(Authentication authentication) {
        return service.get(users.resolve(authentication).orElseThrow(OwnerAuthorizationException::new).userId());
    }

    @PutMapping
    public ApiResponse<ProfileV2Dtos.Response> put(Authentication authentication,
                                                     @Valid @RequestBody ProfileV2Dtos.UpdateRequest request) {
        return service.update(users.resolve(authentication).orElseThrow(OwnerAuthorizationException::new).userId(), request);
    }
}
