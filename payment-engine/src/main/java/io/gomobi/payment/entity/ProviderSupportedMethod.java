package io.gomobi.payment.entity;

import io.gomobi.payment.core.enums.Status;
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

/** Read-mostly mapping of the many-to-many PROVIDER_SUPPORTED_METHOD join table. */
@Getter
@Setter
@ToString
//@EqualsAndHashCode(onlyExplicitlyInclude = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "PROVIDER_SUPPORTED_METHOD",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_PROVIDER_SUPPORTED_METHOD",
                        columnNames = {"PROVIDER_CONFIGURATION_FK", "PAYMENT_METHOD_FK"}
                )
        },
        indexes = {
                @Index(name = "IDX_PSM_PAYMENT_METHOD", columnList = "PAYMENT_METHOD_FK")
        }
)
public class ProviderSupportedMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", columnDefinition = "BIGINT UNSIGNED")
    @EqualsAndHashCode.Include
    private BigInteger id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "PROVIDER_CONFIGURATION_FK",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_PSM_PROVIDER")
    )
    @ToString.Exclude
    private ProviderConfiguration providerConfiguration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "PAYMENT_METHOD_FK",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_PSM_METHOD")
    )
    @ToString.Exclude
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20)
    private Status status;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;
}