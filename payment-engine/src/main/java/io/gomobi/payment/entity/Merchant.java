package io.gomobi.payment.entity;

import io.gomobi.payment.core.enums.MerchantCategory;
import io.gomobi.payment.core.enums.MerchantStatus;
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

/** Read-mostly mapping of the MERCHANT master table; this service never creates/updates merchants. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "MERCHANT")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MERCHANT_ID")
    private Long merchantId;

    @Column(name = "MASTER_MID", nullable = false, unique = true)
    private String masterMid;

    @Column(name = "GLOBAL_ACCOUNT_ID", nullable = false)
    private String globalAccountId;

    @Column(name = "MERCHANT_NAME", nullable = false)
    private String merchantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "MERCHANT_CATEGORY", nullable = false)
    private MerchantCategory merchantCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private MerchantStatus status;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;
}
