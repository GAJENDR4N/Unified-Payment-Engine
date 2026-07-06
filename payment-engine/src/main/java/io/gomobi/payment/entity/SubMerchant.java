package io.gomobi.payment.entity;

import io.gomobi.payment.core.enums.Status;
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

/** Read-mostly mapping of the SUB_MERCHANT master table. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SUB_MERCHANT")
public class SubMerchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SUB_MERCHANT_ID")
    private Long subMerchantId;

    @Column(name = "MERCHANT_FK", nullable = false)
    private Long merchantFk;

    @Column(name = "SUB_MERCHANT_MID", nullable = false, unique = true)
    private String subMerchantMid;

    @Column(name = "SUB_MERCHANT_NAME", nullable = false)
    private String subMerchantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;
}
