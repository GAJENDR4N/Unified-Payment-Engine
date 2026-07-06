package io.gomobi.payment.adapter.fiuu;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.gomobi.payment.adapter.fiuu.dto.FiuuPaymentRequestDto;
import io.gomobi.payment.adapter.fiuu.dto.FiuuPaymentResponseDto;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.util.JsonUtils;
import io.gomobi.payment.util.ProviderCallLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Thin HTTP wrapper around FIUU's REST endpoints. Uses a single shared
 * java.net.HttpClient instance (no double-brace init / per-call client
 * construction).
 *
 * Resilience: transient network failures / timeouts are retried a few
 * times with backoff; if failures keep piling up, the circuit breaker
 * trips and short-circuits further calls to FIUU until it recovers,
 * falling back to a clear PaymentEngineException instead of letting
 * requests queue up against a downed provider.
 *
 * Every outbound call and its response are logged RAW (no masking) via
 * {@link ProviderCallLog} along with call duration - this is a deliberate
 * product decision now that payment audit trails live in the gateway
 * layer rather than in this service; see that class's javadoc for the
 * access-control implication.
 *
 * IMPORTANT: retry/circuit-breaker apply only to transport-level failures
 * (IOException/timeout). Signature/validation failures are never retried.
 */
@Slf4j
@Component
public class FiuuClient {

    private static final String CIRCUIT_BREAKER_NAME = "fiuu";
    private static final String PROVIDER_NAME = "FIUU";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${payment.provider.fiuu.base-url}")
    private String baseUrl;

    @Value("${payment.provider.fiuu.merchant-id}")
    private String merchantId;

    @Value("${payment.provider.fiuu.verify-key}")
    private String verifyKey;

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "submitPaymentFallback")
    @Retryable(
            retryFor = {IOException.class, HttpTimeoutException.class, HttpConnectTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public FiuuPaymentResponseDto submitPayment(FiuuPaymentRequestDto request) {
        long start = System.currentTimeMillis();
        try {
            String body = toFormBody(Map.of(
                    "MerchantID", merchantId,
                    "ReferenceNo", UUID.randomUUID().toString(),
                    "TxnType", "SALS",
                    "TxnChannel", "RPP_DUITNOWQR",
                    "TxnCurrency", request.getCurrency(),
                    "TxnAmount", request.getAmount(),
                    "Signature", request.getVsign()
            ));

            ProviderCallLog.logRequest(log, PROVIDER_NAME, "SUBMIT_PAYMENT", request.getOrderId(), JsonUtils.toJson(request));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/RMS/API/Direct/1.4.0/index.php"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            long durationMs = System.currentTimeMillis() - start;
            ProviderCallLog.logResponse(log, PROVIDER_NAME, "SUBMIT_PAYMENT", request.getOrderId(),
                    "httpStatus=" + response.statusCode() + " body=" + response.body(), durationMs);

            return FiuuPaymentResponseDto.builder()
                    .orderId(request.getOrderId())
                    .rawBody(response.body())
                    .status(response.statusCode() == 200 ? "SUBMITTED" : "ERROR")
                    .build();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            long durationMs = System.currentTimeMillis() - start;
            ProviderCallLog.logError(log, PROVIDER_NAME, "SUBMIT_PAYMENT", request.getOrderId(), durationMs, e);
            throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_ERROR, "FIUU payment submission failed", e);
        }
    }

    /** Invoked by the circuit breaker once it's open, or once retries are exhausted. */
    private FiuuPaymentResponseDto submitPaymentFallback(FiuuPaymentRequestDto request, Throwable t) {
        log.error("event=CLIENT_CIRCUIT_OPEN provider={} operation=SUBMIT_PAYMENT transactionId={} error={}",
                PROVIDER_NAME, request.getOrderId(), t.getMessage());
        throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_UNAVAILABLE,
                "FIUU is currently unavailable, please retry shortly", t);
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "queryStatusFallback")
    @Retryable(
            retryFor = {IOException.class, HttpTimeoutException.class, HttpConnectTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public String queryStatus(String orderId) {
        long start = System.currentTimeMillis();
        try {
            String skey = computeSignature(merchantId, orderId, verifyKey);
            String url = baseUrl + "/RMS/API/query_by_tid.php"
                    + "?merchantid=" + urlEncode(merchantId)
                    + "&orderid=" + urlEncode(orderId)
                    + "&skey=" + urlEncode(skey);

            ProviderCallLog.logRequest(log, PROVIDER_NAME, "QUERY_STATUS", orderId, url);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            long durationMs = System.currentTimeMillis() - start;
            ProviderCallLog.logResponse(log, PROVIDER_NAME, "QUERY_STATUS", orderId, response.body(), durationMs);

            return response.body();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            long durationMs = System.currentTimeMillis() - start;
            ProviderCallLog.logError(log, PROVIDER_NAME, "QUERY_STATUS", orderId, durationMs, e);
            throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_ERROR, "FIUU status query failed", e);
        }
    }

    private String queryStatusFallback(String orderId, Throwable t) {
        log.error("event=CLIENT_CIRCUIT_OPEN provider={} operation=QUERY_STATUS transactionId={} error={}",
                PROVIDER_NAME, orderId, t.getMessage());
        throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_UNAVAILABLE,
                "FIUU is currently unavailable, please retry shortly", t);
    }

    /** MD5 signature over an arbitrary ordered set of components, per FIUU's skey formula. */
    public static String computeSignature(String... components) {
        try {
            String joined = String.join("", components);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new PaymentEngineException(ErrorCode.EXTERNAL_PROVIDER_ERROR, "Failed to compute FIUU signature", e);
        }
    }

    private String toFormBody(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(nullToEmpty(e.getValue())))
                .collect(Collectors.joining("&"));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
