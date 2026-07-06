package io.gomobi.payment.util;

import org.slf4j.Logger;

/** Consistent, structured logging around DB persist operations and their duration. */
public final class DbPersistLog {

    private DbPersistLog() {
    }

    public static void log(Logger log, String entity, String key, long durationMs) {
        log.info("event=DB_PERSIST entity={} key={} durationMs={}", entity, key, durationMs);
    }

    public static void logError(Logger log, String entity, String key, long durationMs, Throwable error) {
        log.error("event=DB_PERSIST_ERROR entity={} key={} durationMs={} error={}",
                entity, key, durationMs, error.getMessage(), error);
    }
}
