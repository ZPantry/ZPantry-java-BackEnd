package com.zpantry.subscription.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.subscription.service.AdminMonitoringService;
import com.zpantry.subscription.service.AdminMonitoringService.RevenueReport;
import com.zpantry.subscription.service.AdminMonitoringService.ServiceStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/monitoring")
public class AdminMonitoringController {
    private final AdminMonitoringService monitoring;

    public AdminMonitoringController(AdminMonitoringService monitoring) {
        this.monitoring = monitoring;
    }

    @GetMapping("/revenue")
    public ApiResponse<RevenueReport> revenue() {
        return new ApiResponse<>(true, "", monitoring.revenue(), null, "", Instant.now());
    }

    @GetMapping("/services")
    public ApiResponse<List<ServiceStatus>> services() {
        return new ApiResponse<>(true, "", monitoring.services(), null, "", Instant.now());
    }
}
