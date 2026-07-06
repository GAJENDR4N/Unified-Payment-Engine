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

/**
 * Read-mostly mapping of the PROVIDER_CONFIGURATION table - the "host"
 * (bank / e-wallet / switch) that a transaction is ultimately routed to,
 * e.g. MAYBANK, TNG, GOPAY, PAYOK. See {@code HostResolutionService} for
 * how a host is resolved for a given payment method, and how a host maps
 * onto a technical gateway adapter ({@code PaymentProvider}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PROVIDER_CONFIGURATION")
public class ProviderConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROVIDER_CONFIGURATION_ID")
    private Long providerConfigurationId;

    @Column(name = "PROVIDER_CODE", nullable = false, unique = true)
    private String providerCode;

    @Column(name = "PROVIDER_NAME", nullable = false)
    private String providerName;

    @Column(name = "REGION_CODE")
    private String regionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;
}
