package com.zpantry.pantryimport.api;

import com.zpantry.pantryimport.api.PantryImportDtos.UnifiedImageAnalysisResponse;
import com.zpantry.pantryimport.service.PantryImportService;
import com.zpantry.user.security.AuthenticatedUserResolver;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Additive V2 contract; V1 receipt and food-image paths intentionally remain unchanged. */
@RestController
@RequestMapping("/api/v2/ingredients")
public class IngredientImageV2Controller {
    private final PantryImportService imports;
    private final AuthenticatedUserResolver users;

    public IngredientImageV2Controller(PantryImportService imports, AuthenticatedUserResolver users) {
        this.imports = imports;
        this.users = users;
    }

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UnifiedImageAnalysisResponse analyze(Authentication authentication,
            @RequestPart("image") MultipartFile image) {
        users.resolve(authentication).orElseThrow();
        validate(image);
        return imports.analyzeUnifiedImage(image);
    }

    private static void validate(MultipartFile image) {
        if (image == null || image.isEmpty() || image.getSize() > 10 * 1024 * 1024
                || image.getContentType() == null
                || !Set.of("image/jpeg", "image/png", "image/webp").contains(image.getContentType())) {
            throw new IllegalArgumentException("A JPEG, PNG, or WEBP image up to 10 MB is required.");
        }
    }
}
