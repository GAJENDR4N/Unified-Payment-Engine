package io.gomobi.payment.adapter.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.gomobi.payment.adapter.pricing.dto.PricingCalculationRequestDto;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.util.JsonUtils;
import io.gomobi.payment.util.ProviderCallLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PricingEngineClient {

    private static final String CIRCUIT_BREAKER_NAME = "pricingEngine";
    private static final String PROVIDER_NAME = "PRICING_ENGINE";

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${payment.pricing-engine.url}")
    private String pricingEngineUrl;

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "submitPricingCalculationFallback")
    @Retryable(
            retryFor = {IOException.class, HttpTimeoutException.class, HttpConnectTimeoutException.class},
            maxAttemptsExpression = "${payment.pricing-engine.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${payment.pricing-engine.retry.delay-ms:500}",
                    multiplierExpression = "${payment.pricing-engine.retry.multiplier:2.0}"
            )
    )
    public void submitPricingCalculation(PricingCalculationRequestDto request) {
        long start = System.currentTimeMillis();
        String transactionId = request.getTransactionId();
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            ProviderCallLog.logRequest(log, PROVIDER_NAME, "CALCULATE_PRICING", transactionId, JsonUtils.toJson(request));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(pricingEngineUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            long durationMs = System.currentTimeMillis() - start;

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                ProviderCallLog.logResponseFailure(log, PROVIDER_NAME, "CALCULATE_PRICING", transactionId,
                        durationMs, response.statusCode(), response.body());
                throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_ERROR,
                        "Pricing engine returned non-success status: " + response.statusCode());
            }

            ProviderCallLog.logResponse(log, PROVIDER_NAME, "CALCULATE_PRICING", transactionId,
                    "httpStatus=" + response.statusCode() + " body=" + response.body(), durationMs);
        } catch (HttpTimeoutException e) {
            long durationMs = System.currentTimeMillis() - start;
            ProviderCallLog.logTimeout(log, PROVIDER_NAME, "CALCULATE_PRICING", transactionId, durationMs, e);
            throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_TIMEOUT,
                    "Pricing engine call timed out", e);
        } catch (IOException e) {
            long durationMs = System.currentTimeMillis() - start;
            ProviderCallLog.logError(log, PROVIDER_NAME, "CALCULATE_PRICING", transactionId, durationMs, e);
            throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_ERROR,
                    "Pricing engine call failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long durationMs = System.currentTimeMillis() - start;
            ProviderCallLog.logTimeout(log, PROVIDER_NAME, "CALCULATE_PRICING", transactionId, durationMs, e);
            throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_TIMEOUT,
                    "Pricing engine call was interrupted", e);
        }
    }

    private String normalizeUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    private void submitPricingCalculationFallback(PricingCalculationRequestDto request, Throwable throwable) {
        String transactionId = request.getTransactionId();
        log.error("event=PRICING_ENGINE_FALLBACK transactionId={} reason={}", transactionId, throwable.getMessage(), throwable);
        throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_ERROR,
                "Pricing engine call failed after retries and circuit breaker fallback", throwable);
    }
}

