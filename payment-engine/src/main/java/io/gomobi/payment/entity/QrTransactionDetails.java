package io.gomobi.payment.entity;

import io.gomobi.payment.core.enums.QrMode;
import io.gomobi.payment.core.enums.QrType;
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

import java.time.LocalDateTime;

/**
 * Maps 1:1 to QR_TRANSACTION_DETAILS - QR-specific + customer data for a
 * PAYMENT_TRANSACTION. Persisted alongside the parent transaction, before
 * the provider is called, using the same pre-call insert strategy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "QR_TRANSACTION_DETAILS")
public class QrTransactionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "QR_TRANSACTION_DETAIL_ID")
    private Long qrTransactionDetailId;

    @Column(name = "PAYMENT_TRANSACTION_FK", nullable = false, unique = true)
    private Long paymentTransactionFk;

    @Enumerated(EnumType.STRING)
    @Column(name = "QR_TYPE", nullable = false)
    private QrType qrType;

    @Enumerated(EnumType.STRING)
    @Column(name = "QR_MODE")
    private QrMode qrMode;

    @Column(name = "QR_IMAGE_URL")
    private String qrImageUrl;

    @Column(name = "MERCHANT_CATEGORY_CODE")
    private String merchantCategoryCode;

    @Column(name = "MERCHANT_CITY")
    private String merchantCity;

    @Column(name = "COUNTRY_CODE", columnDefinition = "char(3)", length = 3)
    private String countryCode;

    @Column(name = "QR_EXPIRY_TIMESTAMP")
    private LocalDateTime qrExpiryTimestamp;

    @Column(name = "CUSTOMER_ID")
    private String customerId;

    @Column(name = "CUSTOMER_NAME")
    private String customerName;

    @Column(name = "CUSTOMER_EMAIL")
    private String customerEmail;

    @Column(name = "CUSTOMER_PHONE")
    private String customerPhone;

    @Column(name = "CUSTOMER_ACCOUNT_MASKED")
    private String customerAccountMasked;

    /** Merchant-defined, opaque, echoed back as-is via API `metadata`. Stored as a JSON string. */
    @Column(name = "METADATA", columnDefinition = "json")
    private String metadata;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;
}
