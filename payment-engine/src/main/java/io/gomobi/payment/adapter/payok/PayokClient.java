package io.gomobi.payment.adapter.payok;

import io.gomobi.payment.adapter.payok.dto.PayokPaymentRequestDto;
import io.gomobi.payment.adapter.payok.dto.PayokPaymentResponseDto;
import io.gomobi.payment.util.JsonUtils;
import io.gomobi.payment.util.ProviderCallLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Thin client for the PAYOK VietQR gateway.
 *
 * STATUS: typed skeleton, not yet wired to a live endpoint - PAYOK's exact
 * request/response contract (auth scheme, field names, status codes) has
 * not been confirmed yet (see GoMobi's QR scheme comparison notes: VietQR
 * uses a direct bank-transfer model keyed by the receiving bank's code,
 * unlike QRIS/DuitNow's merchant-token model). Wire {@code submitPayment}/
 * {@code queryStatus} up to the real HTTP calls (mirroring
 * {@code FiuuClient}'s retry/circuit-breaker/raw-logging pattern) once the
 * contract is confirmed; until then this returns a deterministic PENDING
 * response so the create-payment flow can be exercised end-to-end.
 */
@Slf4j
@Component
public class PayokClient {

    private static final String PROVIDER_NAME = "PAYOK";

    @Value("${payment.provider.payok.base-url}")
    private String baseUrl;

    public PayokPaymentResponseDto submitPayment(PayokPaymentRequestDto request) {
        long start = System.currentTimeMillis();
        ProviderCallLog.logRequest(log, PROVIDER_NAME, "SUBMIT_PAYMENT", request.getOrderId(), JsonUtils.toJson(request));

        // TODO: replace with a real HTTP call to `baseUrl` once PAYOK's contract is confirmed.
        PayokPaymentResponseDto response = PayokPaymentResponseDto.builder()
                .transactionId(request.getOrderId())
                .orderId(request.getOrderId())
                .status("PENDING")
                .qrString("00020101021138570010A000000727" + request.getOrderId()) // placeholder EMV-QR payload
                .rawBody("SKELETON_RESPONSE - PAYOK integration pending contract confirmation")
                .build();

        long durationMs = System.currentTimeMillis() - start;
        ProviderCallLog.logResponse(log, PROVIDER_NAME, "SUBMIT_PAYMENT", request.getOrderId(),
                JsonUtils.toJson(response), durationMs);

        return response;
    }

    public String queryStatus(String orderId) {
        long start = System.currentTimeMillis();
        ProviderCallLog.logRequest(log, PROVIDER_NAME, "QUERY_STATUS", orderId, "orderId=" + orderId);

        // TODO: replace with a real HTTP call once PAYOK's contract is confirmed.
        String rawResponse = "SKELETON_RESPONSE status=PENDING orderId=" + orderId;

        long durationMs = System.currentTimeMillis() - start;
        ProviderCallLog.logResponse(log, PROVIDER_NAME, "QUERY_STATUS", orderId, rawResponse, durationMs);

        return rawResponse;
    }

    /** Placeholder MD5 signature - replace once PAYOK's documented signature formula is confirmed. */
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
            throw new RuntimeException("Failed to compute PAYOK signature", e);
        }
    }
}
