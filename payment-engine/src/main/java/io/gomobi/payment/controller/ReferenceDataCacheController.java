package io.gomobi.payment.controller;

import io.gomobi.payment.service.ReferenceDataCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/reference-data-cache")
@RequiredArgsConstructor
public class ReferenceDataCacheController {

    private final ReferenceDataCacheService referenceDataCacheService;

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh() {
        ReferenceDataCacheService.CacheMetrics metrics = referenceDataCacheService.refreshCache();
        return ResponseEntity.ok(Map.of(
                "status", "refreshed",
                "paymentMethodCount", metrics.paymentMethodCount(),
                "providerConfigurationCount", metrics.providerConfigurationCount(),
                "activeChannelCount", metrics.activeChannelCount()
        ));
    }
}

