package com.zpantry.pantryimport.api;

import static com.zpantry.pantryimport.api.PantryImportDtos.*;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.pantryimport.service.PantryImportService;
import com.zpantry.user.security.AuthenticatedUserResolver;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me/pantry-import")
public class PantryImportController {
    private final PantryImportService service;
    private final AuthenticatedUserResolver users;

    public PantryImportController(PantryImportService s, AuthenticatedUserResolver u) {
        service = s;
        users = u;
    }

    private java.util.UUID id(Authentication a) {
        return users.resolve(a).orElseThrow().userId();
    }

    @PostMapping(value = "/receipt/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PantryImportPreviewResponse receipt(Authentication a, @RequestPart("image") MultipartFile image) {
        id(a);
        validate(image);
        return service.analyze(SourceType.RECEIPT, image);
    }

    @PostMapping(value = "/food-image/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PantryImportPreviewResponse food(Authentication a, @RequestPart("image") MultipartFile image) {
        id(a);
        validate(image);
        return service.analyze(SourceType.FOOD_IMAGE, image);
    }

    @PostMapping("/confirm")
    public ApiResponse<Void> confirm(Authentication a, @Valid @RequestBody ConfirmPantryImportRequest r) {
        service.confirm(id(a), r);
        return new ApiResponse<>(true, "Pantry import confirmed.", null, null, "", Instant.now());
    }

    private void validate(MultipartFile f) {
        if (f == null || f.isEmpty() || f.getSize() > 10 * 1024 * 1024 || f.getContentType() == null || !Set.of("image/jpeg", "image/png", "image/webp").contains(f.getContentType()))
            throw new IllegalArgumentException("A JPEG, PNG, or WEBP image up to 10 MB is required.");
    }
}
