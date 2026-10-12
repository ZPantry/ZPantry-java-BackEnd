package com.zpantry.pantryimport.api;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zpantry.pantryimport.service.PantryImportService;
import com.zpantry.pantryimport.api.PantryImportDtos.ImageType;
import com.zpantry.pantryimport.api.PantryImportDtos.UnifiedImageAnalysisResponse;
import java.math.BigDecimal;
import java.util.List;
import com.zpantry.subscription.service.SubscriptionService;
import com.zpantry.user.security.AuthenticatedUser;
import com.zpantry.user.security.AuthenticatedUserResolver;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

class IngredientImageV2ControllerTest {
    @Test
    void unifiedImageAnalysisChargesOnlyWhenItHasUsefulResults() {
        PantryImportService imports = Mockito.mock(PantryImportService.class);
        SubscriptionService subscriptions = Mockito.mock(SubscriptionService.class);
        AuthenticatedUserResolver users = Mockito.mock(AuthenticatedUserResolver.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        UUID userId = UUID.randomUUID();
        when(users.resolve(authentication)).thenReturn(Optional.of(new AuthenticatedUser(userId, "user@example.test", java.util.Set.of("USER"), "test-jti")));
        IngredientImageV2Controller controller = new IngredientImageV2Controller(imports, subscriptions, users);
        when(imports.analyzeUnifiedImage(Mockito.any())).thenReturn(new UnifiedImageAnalysisResponse(ImageType.FOOD_IMAGE,
                BigDecimal.ONE, List.of(Mockito.mock(com.zpantry.pantryimport.api.PantryImportDtos.PantryImportPreviewItem.class)), List.of()));

        controller.analyze(authentication, new MockMultipartFile("image", "food.png", "image/png", new byte[] {1}));

        verify(subscriptions).reserve(userId, SubscriptionService.OCR);
        verify(subscriptions, never()).refund(userId, SubscriptionService.OCR);
    }

    @Test
    void unifiedImageAnalysisRefundsUnknownImages() {
        PantryImportService imports = Mockito.mock(PantryImportService.class);
        SubscriptionService subscriptions = Mockito.mock(SubscriptionService.class);
        AuthenticatedUserResolver users = Mockito.mock(AuthenticatedUserResolver.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        UUID userId = UUID.randomUUID();
        when(users.resolve(authentication)).thenReturn(Optional.of(new AuthenticatedUser(userId, "user@example.test", java.util.Set.of("USER"), "test-jti")));
        when(imports.analyzeUnifiedImage(Mockito.any())).thenReturn(new UnifiedImageAnalysisResponse(ImageType.UNKNOWN,
                BigDecimal.ZERO, List.of(), List.of("BLURRY")));

        new IngredientImageV2Controller(imports, subscriptions, users)
                .analyze(authentication, new MockMultipartFile("image", "food.png", "image/png", new byte[] {1}));

        verify(subscriptions).refund(userId, SubscriptionService.OCR);
    }
}
