package io.gomobi.payment.service;

import io.gomobi.payment.core.enums.PaymentMethodType;
import io.gomobi.payment.core.enums.PaymentProvider;
import io.gomobi.payment.dto.CreatePaymentRequest;
import io.gomobi.payment.dto.CustomerInfo;
import io.gomobi.payment.dto.PaymentMethodInfo;
import io.gomobi.payment.entity.PaymentMethod;
import io.gomobi.payment.entity.ProviderConfiguration;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePaymentRequestValidationServiceTest {

    private final CreatePaymentRequestValidationService validationService = new CreatePaymentRequestValidationService();

    @Test
    void normalizesVietnamMobileNumber() {
        CreatePaymentRequest request = createRequest("VN_VIETQR", "0912345678", BigInteger.valueOf(10000));

        validationService.validateAndNormalize(request, vietnamResolvedHost());

        assertThat(request.getCustomer().getPhone()).isEqualTo("+84912345678");
    }

    @Test
    void leavesNonVietnamPaymentUnchanged() {
        CreatePaymentRequest request = createRequest("MY_DUITNOW_QR", "0912345678", BigInteger.valueOf(10000));

        validationService.validateAndNormalize(request, nonVietnamResolvedHost());

        assertThat(request.getCustomer().getPhone()).isEqualTo("0912345678");
    }

    @Test
    void rejectsInvalidVietnamMobileNumber() {
        CreatePaymentRequest request = createRequest("VN_VIETQR", "12345", BigInteger.valueOf(10000));

        PaymentEngineException ex = null;
        try {
            validationService.validateAndNormalize(request, vietnamResolvedHost());
        } catch (PaymentEngineException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void rejectsNonPositiveAmount() {
        CreatePaymentRequest request = createRequest("VN_VIETQR", "0912345678", BigInteger.ZERO);

        PaymentEngineException ex = null;
        try {
            validationService.validateAndNormalize(request, vietnamResolvedHost());
        } catch (PaymentEngineException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private CreatePaymentRequest createRequest(String channelCode, String phone, BigInteger amount) {
        return CreatePaymentRequest.builder()
                .referenceId("REF-001")
                .globalAccountId("GA000001")
                .masterMid("MID000001")
                .amount(amount)
                .currency("VND")
                .description("Test payment")
                .paymentMethod(PaymentMethodInfo.builder()
                        .type(PaymentMethodType.QR)
                        .channelCode(channelCode)
                        .build())
                .customer(CustomerInfo.builder()
                        .id("CUST-001")
                        .name("Test Customer")
                        .email("customer@example.com")
                        .phone(phone)
                        .build())
                .build();
    }

    private ResolvedHost vietnamResolvedHost() {
        return new ResolvedHost(
                PaymentMethod.builder()
                        .channelCode("VN_VIETQR")
                        .build(),
                ProviderConfiguration.builder()
                        .regionCode("VN")
                        .build(),
                PaymentProvider.PAYOK
        );
    }

    private ResolvedHost nonVietnamResolvedHost() {
        return new ResolvedHost(
                PaymentMethod.builder()
                        .channelCode("MY_DUITNOW_QR")
                        .build(),
                ProviderConfiguration.builder()
                        .regionCode("MY")
                        .build(),
                PaymentProvider.PAYOK
        );
    }
}

