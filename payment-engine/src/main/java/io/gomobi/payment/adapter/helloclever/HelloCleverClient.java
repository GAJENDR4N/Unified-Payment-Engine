package io.gomobi.payment.adapter.helloclever;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.gomobi.payment.adapter.helloclever.dto.HelloCleverPayinRequestDto;
import io.gomobi.payment.adapter.helloclever.dto.HelloCleverPayinResponseDto;
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

/**
 * HTTP client for Hello Clever pay-in creation.
 * <p>
 * Expected API contract for now:
 * - POST JSON to configured `payment.provider.hello-clever.api-url`
 * - headers: `app-id`, `secret-key`, `Accept`, `Content-Type`
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HelloCleverClient {

    private static final String CIRCUIT_BREAKER_NAME = "helloClever";
    private static final String PROVIDER_NAME = "HELLO_CLEVER";

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${payment.provider.hello-clever.api-url}")
    private String apiUrl;

    @Value("${payment.provider.hello-clever.app-id}")
    private String appId;

    @Value("${payment.provider.hello-clever.secret-key}")
    private String secretKey;

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "submitPaymentFallback")
    @Retryable(
            retryFor = {IOException.class, HttpTimeoutException.class, HttpConnectTimeoutException.class},
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public HelloCleverPayinResponseDto submitPayment(HelloCleverPayinRequestDto request) {
        long start = System.currentTimeMillis();
        String transactionRef = request.getExternalId();
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            ProviderCallLog.logRequest(log, PROVIDER_NAME, "SUBMIT_PAYMENT", transactionRef, JsonUtils.toJson(request));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("app-id", appId)
                    .header("secret-key", secretKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            long durationMs = System.currentTimeMillis() - start;

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                ProviderCallLog.logResponseFailure(log, PROVIDER_NAME, "SUBMIT_PAYMENT", transactionRef,
                        durationMs, response.statusCode(), response.body());
                throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_ERROR,
                        "Hello Clever returned non-success status: " + response.statusCode());
            }

            ProviderCallLog.logResponse(log, PROVIDER_NAME, "SUBMIT_PAYMENT", transactionRef,
                    "httpStatus=" + response.statusCode() + " body=" + response.body(), durationMs);

            HelloCleverPayinResponseDto mapped = objectMapper.readValue(response.body(), HelloCleverPayinResponseDto.class);
            mapped.setRawBody(response.body());
            return mapped;
        } catch (HttpTimeoutException e) {
            long durationMs = System.currentTimeMillis() - start;
            ProviderCallLog.logTimeout(log, PROVIDER_NAME, "SUBMIT_PAYMENT", transactionRef, durationMs, e);
            throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_TIMEOUT,
                    "Hello Clever payment submission timed out", e);
        } catch (IOException e) {
            long durationMs = System.currentTimeMillis() - start;
            ProviderCallLog.logError(log, PROVIDER_NAME, "SUBMIT_PAYMENT", transactionRef, durationMs, e);
            throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_ERROR, "Hello Clever payment submission failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long durationMs = System.currentTimeMillis() - start;
            ProviderCallLog.logTimeout(log, PROVIDER_NAME, "SUBMIT_PAYMENT", transactionRef, durationMs, e);
            throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_TIMEOUT,
                    "Hello Clever payment submission was interrupted", e);
        }
    }

    private HelloCleverPayinResponseDto submitPaymentFallback(HelloCleverPayinRequestDto request, Throwable t) {
        log.error("event=CLIENT_CIRCUIT_OPEN provider={} operation=SUBMIT_PAYMENT transactionId={} error={}",
                PROVIDER_NAME, request.getExternalId(), t.getMessage());
        throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_UNAVAILABLE,
                "Hello Clever is currently unavailable, please retry shortly", t);
    }
}

