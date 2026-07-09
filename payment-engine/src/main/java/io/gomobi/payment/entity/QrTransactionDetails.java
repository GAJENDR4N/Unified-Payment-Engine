package io.gomobi.payment.entity;

import io.gomobi.payment.core.enums.QrMode;
import io.gomobi.payment.core.enums.QrType;
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
import jakarta.persistence.OneToOne;
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
 * Maps 1:1 to QR_TRANSACTION_DETAILS - QR-specific + customer data for a
 * PAYMENT_TRANSACTION. Persisted alongside the parent transaction, before
 * the provider is called, using the same pre-call insert strategy.
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
        name = "QR_TRANSACTION_DETAILS",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_QR_DETAILS_TXN", columnNames = "PAYMENT_TRANSACTION_FK")
        },
        indexes = {
                @Index(name = "IDX_QR_CUSTOMER_ID", columnList = "CUSTOMER_ID")
        }
)
public class QrTransactionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", columnDefinition = "BIGINT UNSIGNED")
    @EqualsAndHashCode.Include
    private BigInteger id;

    /** 1:1 owning side - the join column carries the UK_QR_DETAILS_TXN unique constraint. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "PAYMENT_TRANSACTION_FK",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "FK_QRD_PT")
    )
    @ToString.Exclude
    private PaymentTransaction paymentTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "QR_TYPE", nullable = false, length = 20)
    private QrType qrType;

    @Enumerated(EnumType.STRING)
    @Column(name = "QR_MODE", length = 20)
    private QrMode qrMode;

    @Column(name = "QR_IMAGE_URL", length = 1000)
    private String qrImageUrl;

    @Column(name = "MERCHANT_CATEGORY_CODE", length = 4)
    private String merchantCategoryCode;

    @Column(name = "MERCHANT_CITY", length = 100)
    private String merchantCity;

    @Column(name = "COUNTRY_CODE", columnDefinition = "CHAR(2)", length = 2)
    private String countryCode;

    @Column(name = "QR_EXPIRY_TIMESTAMP")
    private LocalDateTime qrExpiryTimestamp;

    /** Merchant-supplied customer id; API `customer.id`. */
    @Column(name = "CUSTOMER_ID", length = 100)
    private String customerId;

    @Column(name = "CUSTOMER_NAME", length = 150)
    private String customerName;

    @Column(name = "CUSTOMER_EMAIL", length = 150)
    private String customerEmail;

    @Column(name = "CUSTOMER_PHONE", length = 50)
    private String customerPhone;

    @Column(name = "CUSTOMER_ACCOUNT_MASKED", length = 50)
    private String customerAccountMasked;

    /** Merchant-defined, opaque, echoed back as-is via API `metadata`. Stored as a JSON string. */
    @Column(name = "METADATA", columnDefinition = "json")
    private String metadata;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;
}