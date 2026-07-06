package io.gomobi.payment.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Generates the internal TRANSACTION_ID stamped onto PAYMENT_TRANSACTION.
 * Format: TXN-{epochMillis}-{8 random hex chars} - sortable by creation
 * time at a glance while remaining collision-safe without a DB round trip.
 */
@Component
public class TransactionIdGenerator {

    public String generate() {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "TXN-" + Instant.now().toEpochMilli() + "-" + random;
    }
}
