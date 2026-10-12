package com.zpantry.pantryimport.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zpantry.pantryimport.service.PantryImportService;
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
    void unifiedImageAnalysisConsumesTheSubscriptionOcrQuota() {
        PantryImportService imports = Mockito.mock(PantryImportService.class);
        SubscriptionService subscriptions = Mockito.mock(SubscriptionService.class);
        AuthenticatedUserResolver users = Mockito.mock(AuthenticatedUserResolver.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        UUID userId = UUID.randomUUID();
        when(users.resolve(authentication)).thenReturn(Optional.of(new AuthenticatedUser(userId, "user@example.test", java.util.Set.of("USER"), "test-jti")));
        IngredientImageV2Controller controller = new IngredientImageV2Controller(imports, subscriptions, users);

        controller.analyze(authentication, new MockMultipartFile("image", "food.png", "image/png", new byte[] {1}));

        verify(subscriptions).consume(userId, SubscriptionService.OCR);
    }
}
