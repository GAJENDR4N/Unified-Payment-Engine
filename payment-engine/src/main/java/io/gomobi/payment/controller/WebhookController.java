package io.gomobi.payment.controller;

import io.gomobi.payment.service.HelloCleverWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Receives Hello Clever webhook events. */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final HelloCleverWebhookService helloCleverWebhookService;

    @PostMapping(value = "/hello-clever", consumes = "application/json")
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody String requestBody,
                                                             @RequestHeader Map<String, String> headers) {
        helloCleverWebhookService.processWebhook(requestBody, headers);
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }
}

