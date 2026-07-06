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

/** Read-mostly mapping of the many-to-many PROVIDER_SUPPORTED_METHOD table. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PROVIDER_SUPPORTED_METHOD")
public class ProviderSupportedMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROVIDER_SUPPORTED_METHOD_ID")
    private Long providerSupportedMethodId;

    @Column(name = "PROVIDER_CONFIGURATION_FK", nullable = false)
    private Long providerConfigurationFk;

    @Column(name = "PAYMENT_METHOD_FK", nullable = false)
    private Long paymentMethodFk;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;
}
