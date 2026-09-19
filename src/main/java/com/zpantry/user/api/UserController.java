package com.zpantry.user.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.common.api.PagedResponse;
import com.zpantry.user.security.AuthenticatedUserResolver;
import com.zpantry.user.service.OwnerAuthorizationException;
import com.zpantry.user.service.UserService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    private final AuthenticatedUserResolver identities;

    public UserController(UserService service, AuthenticatedUserResolver identities) {
        this.service = service;
        this.identities = identities;
    }

    @GetMapping
    public PagedResponse<UserResponse> list(
            @RequestParam(defaultValue = "1") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        return service.list(pageIndex, pageSize);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable UUID id,
            @RequestBody UserUpdateRequest request, Authentication authentication) {
        var identity = identities.resolve(authentication).orElseThrow(OwnerAuthorizationException::new);
        var response = service.update(id, request, identity);
        return ResponseEntity.status(response.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable UUID id) { return service.delete(id); }
}
