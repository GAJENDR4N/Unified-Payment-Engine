package io.gomobi.payment.adapter;

import io.gomobi.payment.core.enums.PaymentBrand;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.core.model.PaymentRequest;
import io.gomobi.payment.core.model.PaymentResponse;
import io.gomobi.payment.core.model.StatusQueryRequest;
import io.gomobi.payment.core.model.StatusQueryResponse;

import java.security.NoSuchAlgorithmException;
import java.util.Set;

/**
 * Implemented once per upstream technical gateway (FIUU, PAYOK, CURLEC...).
 * A single gateway can serve multiple brands (e.g. FIUU handles DuitNow,
 * QRIS and TNG) - the adapter internally knows how to build the
 * gateway-specific request for whichever brand it's asked to process.
 *
 * Scope is limited to payment creation and status enquiry - inbound
 * provider callbacks/IPNs are handled by the gateway layer upstream of
 * this service, not here.
 */
public interface PaymentProviderAdapter {

    PaymentProvider getProviderCode();

    /** Brands this gateway is configured/certified to process. Used for fail-fast validation. */
    Set<PaymentBrand> getSupportedBrands();

    default boolean supports(PaymentBrand brand) {
        return getSupportedBrands().contains(brand);
    }

    PaymentResponse initiatePayment(PaymentRequest request);

    StatusQueryResponse queryStatus(StatusQueryRequest request) throws NoSuchAlgorithmException;
}
