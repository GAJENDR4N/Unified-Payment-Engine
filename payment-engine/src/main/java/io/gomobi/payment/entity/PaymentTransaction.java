package io.gomobi.payment.entity;

import io.gomobi.payment.core.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * Maps 1:1 to the PAYMENT_TRANSACTION table of the unified payments schema
 * (v5). A row is inserted with TRANSACTION_STATUS = INITIATED before the
 * provider is ever called, and updated afterwards with the outcome - so a
 * crash or timeout mid-call still leaves a durable, queryable record of
 * what was attempted.
 */
@Getter
@Setter
@ToString
//@EqualsAndHashCode(onlyExplicitlyInclude = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "PAYMENT_TRANSACTION",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_TRANSACTION_ID", columnNames = "TRANSACTION_ID"),
                @UniqueConstraint(name = "UK_MERCHANT_REF", columnNames = {"MERCHANT_FK", "MERCHANT_REF_NO"})
        },
        indexes = {
                @Index(name = "IDX_PAYMENT_PROVIDER_CONFIG", columnList = "PROVIDER_CONFIGURATION_FK"),
                @Index(name = "IDX_PAYMENT_MERCHANT", columnList = "MERCHANT_FK"),
                @Index(name = "IDX_PAYMENT_SUBMERCHANT", columnList = "SUB_MERCHANT_FK"),
                @Index(name = "IDX_PAYMENT_METHOD", columnList = "PAYMENT_METHOD_FK"),
                @Index(name = "IDX_STATUS_CREATED", columnList = "TRANSACTION_STATUS, CREATED_TIMESTAMP"),
                @Index(name = "IDX_METHOD_CREATED", columnList = "PAYMENT_METHOD_FK, CREATED_TIMESTAMP"),
                @Index(name = "IDX_PSP_REF", columnList = "PSP_REF_NO"),
                @Index(name = "IDX_TRANSACTION_ID_STATUS", columnList = "TRANSACTION_ID, TRANSACTION_STATUS"),
                @Index(name = "IDX_PROVIDER_STATUS", columnList = "PROVIDER_CONFIGURATION_FK, TRANSACTION_STATUS"),
                @Index(name = "IDX_CREATED_PAID", columnList = "CREATED_TIMESTAMP, PAID_TIMESTAMP")
        }
)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", columnDefinition = "BIGINT UNSIGNED")
    @EqualsAndHashCode.Include
    private BigInteger id;

    /** Mobi-generated transaction id; exposed to the caller as `transaction_id`. */
    @Column(name = "TRANSACTION_ID", nullable = false, length = 150)
    private String transactionId;

    /** Caller-supplied order/order reference; API request/response `reference_id`. */
    @Column(name = "MERCHANT_REF_NO", nullable = false, length = 150)
    private String merchantRefNo;

    /** PSP-side reference; used for lookup/reconciliation/support. */
    @Column(name = "PSP_REF_NO", length = 150)
    private String pspRefNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "TRANSACTION_STATUS", nullable = false, length = 20)
    private TransactionStatus transactionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "PROVIDER_CONFIGURATION_FK",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_PT_PROVIDER_CONFIG")
    )
    @ToString.Exclude
    private ProviderConfiguration providerConfiguration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "MERCHANT_FK",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_PT_MERCHANT")
    )
    @ToString.Exclude
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "SUB_MERCHANT_FK",
            foreignKey = @ForeignKey(name = "FK_PT_SUB")
    )
    @ToString.Exclude
    private SubMerchant subMerchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "PAYMENT_METHOD_FK",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_PT_PM")
    )
    @ToString.Exclude
    private PaymentMethod paymentMethod;

    /** Minor units (e.g. 10000 = 100.00). */
    @Column(name = "TRANSACTION_AMOUNT", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private BigInteger transactionAmount;

    @Column(name = "CURRENCY_CODE", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;

    @Column(name = "PAID_TIMESTAMP")
    private LocalDateTime paidTimestamp;
}