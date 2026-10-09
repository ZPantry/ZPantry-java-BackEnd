package com.zpantry.subscription.api;

import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public browser landing pages used only to hand a completed or cancelled checkout back to the
 * installed mobile application. Payment status is deliberately not read from the redirect query:
 * only a verified provider webhook may activate a subscription.
 */
@RestController
public class PaymentReturnController {
    private static final MediaType HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

    @GetMapping(value = "/payment/success", produces = "text/html")
    ResponseEntity<String> success() {
        return redirectToApp("zpantry://payment/success", "Thanh toán đang được xử lý", "Bạn có thể quay lại Z-Pantry để kiểm tra trạng thái gói.");
    }

    @GetMapping(value = "/payment/cancel", produces = "text/html")
    ResponseEntity<String> cancel() {
        return redirectToApp("zpantry://payment/cancel", "Bạn đã hủy thanh toán", "Bạn có thể quay lại Z-Pantry bất cứ lúc nào để thử lại.");
    }

    private ResponseEntity<String> redirectToApp(String deepLink, String title, String detail) {
        String page = """
                <!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Z-Pantry</title><style>body{margin:0;font-family:Arial,sans-serif;background:#fff8f2;color:#2d2119;display:grid;min-height:100vh;place-items:center}.card{max-width:360px;margin:24px;padding:28px;text-align:center;background:#fff;border-radius:20px;box-shadow:0 8px 28px #0001}a{display:inline-block;margin-top:18px;padding:13px 20px;border-radius:12px;background:#ef7b25;color:#fff;text-decoration:none;font-weight:700}</style>
                </head><body><main class="card"><h1>%s</h1><p>%s</p><a href="%s">Mở Z-Pantry</a></main><script>location.replace('%s');</script></body></html>
                """.formatted(title, detail, deepLink, deepLink);
        return ResponseEntity.ok()
                .contentType(HTML_UTF8)
                .cacheControl(CacheControl.noStore())
                .body(page);
    }
}
