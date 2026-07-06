package io.gomobi.payment.registry;

import io.gomobi.payment.adapter.PaymentProviderAdapter;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the correct {@link PaymentProviderAdapter} for a given
 * {@link PaymentProvider} gateway. Spring auto-wires every adapter bean;
 * each adapter self-registers via {@link PaymentProviderAdapter#getProviderCode()}.
 *
 * The gateway to use per-transaction is resolved upstream by
 * {@code HostResolutionService} - this registry does pure dispatch, no
 * routing decisions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAdapterRegistry {

    private final List<PaymentProviderAdapter> adapters;

    private Map<PaymentProvider, PaymentProviderAdapter> adapterMap;

    @PostConstruct
    void init() {
        adapterMap = adapters.stream()
                .collect(Collectors.toMap(PaymentProviderAdapter::getProviderCode, Function.identity()));
        log.info("Registered payment adapters: {}", adapterMap.keySet());
    }

    public PaymentProviderAdapter getAdapter(PaymentProvider provider) {
        PaymentProviderAdapter adapter = adapterMap.get(provider);
        if (adapter == null) {
            throw new PaymentEngineException(ErrorCode.PROVIDER_NOT_FOUND, "No adapter registered for provider: " + provider);
        }
        return adapter;
    }
}
