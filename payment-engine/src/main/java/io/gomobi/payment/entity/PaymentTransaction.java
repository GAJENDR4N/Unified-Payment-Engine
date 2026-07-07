package io.gomobi.payment.entity;

import io.gomobi.payment.core.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps 1:1 to the PAYMENT_TRANSACTION table of the unified payments schema
 * (v5). A row is inserted with TRANSACTION_STATUS = INITIATED before the
 * provider is ever called, and updated afterwards with the outcome - so a
 * crash or timeout mid-call still leaves a durable, queryable record of
 * what was attempted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PAYMENT_TRANSACTION")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_TRANSACTION_ID")
    private Long paymentTransactionId;

    /** Mobi-generated transaction id; exposed to the caller as `transaction_id`. */
    @Column(name = "TRANSACTION_ID", nullable = false, unique = true)
    private String transactionId;

    /** Caller-supplied order/order reference; API request/response `reference_id`. */
    @Column(name = "MERCHANT_REF_NO", nullable = false)
    private String merchantRefNo;

    /** Caller-supplied idempotency key, scoped unique per merchant. */
    @Column(name = "GATEWAY_IDEMPOTENCY_KEY", nullable = false)
    private String gatewayIdempotencyKey;

    @Column(name = "RRN")
    private String rrn;

    /** PSP-side reference, populated once the provider responds. */
    @Column(name = "PSP_REF_NO")
    private String pspRefNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "TRANSACTION_STATUS", nullable = false)
    private TransactionStatus transactionStatus;

    @Column(name = "PROVIDER_CONFIGURATION_FK", nullable = false)
    private Long providerConfigurationFk;

    @Column(name = "MERCHANT_FK", nullable = false)
    private Long merchantFk;

    @Column(name = "SUB_MERCHANT_FK")
    private Long subMerchantFk;

    @Column(name = "PAYMENT_METHOD_FK", nullable = false)
    private Long paymentMethodFk;

    @Column(name = "TRANSACTION_AMOUNT", nullable = false)
    private BigDecimal transactionAmount;

    @Column(name = "CURRENCY_CODE", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;

    @Column(name = "PAID_TIMESTAMP")
    private LocalDateTime paidTimestamp;
}
