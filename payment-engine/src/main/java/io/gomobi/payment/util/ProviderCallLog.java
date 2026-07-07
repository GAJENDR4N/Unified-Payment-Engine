package io.gomobi.payment.util;

import org.slf4j.Logger;

/**
 * Consistent, structured (key=value) logging around outbound calls to
 * upstream payment providers, so external log tooling (ELK/Loki/Splunk)
 * can filter/aggregate on a fixed set of field names regardless of which
 * provider adapter emitted the line.
 *
 * Deliberately logs the RAW request/response bodies with no masking, per
 * product decision: this service no longer keeps its own audit trail (that
 * responsibility moved to the gateway layer), so the provider call log is
 * the only record of exactly what was sent/received. Access to these logs
 * should be restricted at the log-aggregation layer since they may contain
 * customer PII and provider payloads.
 */
public final class ProviderCallLog {

    private ProviderCallLog() {
    }

    public static void logRequest(Logger log, String provider, String operation, String transactionId, String rawRequest) {
        log.info("event=CLIENT_REQUEST provider={} operation={} transactionId={} rawRequest={}",
                provider, operation, transactionId, rawRequest);
    }

    public static void logResponse(Logger log, String provider, String operation, String transactionId,
                                    String rawResponse, long durationMs) {
        log.info("event=CLIENT_RESPONSE provider={} operation={} transactionId={} result=SUCCESS durationMs={} rawResponse={}",
                provider, operation, transactionId, durationMs, rawResponse);
    }

    public static void logResponseFailure(Logger log, String provider, String operation, String transactionId,
                                          long durationMs, int httpStatus, String rawResponse) {
        log.error("event=CLIENT_RESPONSE provider={} operation={} transactionId={} result=FAILED durationMs={} httpStatus={} rawResponse={}",
                provider, operation, transactionId, durationMs, httpStatus, rawResponse);
    }

    public static void logTimeout(Logger log, String provider, String operation, String transactionId,
                                  long durationMs, Throwable error) {
        log.error("event=PROVIDER_TIMEOUT provider={} operation={} transactionId={} result=FAILED durationMs={} error={}",
                provider, operation, transactionId, durationMs, error.getMessage(), error);
    }

    public static void logError(Logger log, String provider, String operation, String transactionId,
                                 long durationMs, Throwable error) {
        log.error("event=CLIENT_ERROR provider={} operation={} transactionId={} result=FAILED durationMs={} error={}",
                provider, operation, transactionId, durationMs, error.getMessage(), error);
    }
}
