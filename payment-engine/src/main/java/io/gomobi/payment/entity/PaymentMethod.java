package io.gomobi.payment.entity;

import io.gomobi.payment.core.enums.PaymentMethodType;
import io.gomobi.payment.core.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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

/** Read-mostly mapping of the PAYMENT_METHOD lookup table. */
@Getter
@Setter
@ToString
//@EqualsAndHashCode(onlyExplicitlyInclude = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "PAYMENT_METHOD",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_PAYMENT_METHOD_CODE", columnNames = "PAYMENT_METHOD_CODE"),
                @UniqueConstraint(name = "UK_CHANNEL_CODE", columnNames = "CHANNEL_CODE")
        },
        indexes = {
                @Index(name = "IDX_PAYMENT_METHOD_TYPE", columnList = "PAYMENT_METHOD_TYPE")
        }
)
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", columnDefinition = "BIGINT UNSIGNED")
    @EqualsAndHashCode.Include
    private BigInteger id;

    @Column(name = "PAYMENT_METHOD_CODE", nullable = false, length = 50)
    private String paymentMethodCode;

    @Column(name = "PAYMENT_METHOD_NAME", nullable = false, length = 100)
    private String paymentMethodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_METHOD_TYPE", nullable = false, length = 20)
    private PaymentMethodType paymentMethodType;

    @Column(name = "CHANNEL_CODE", nullable = false, length = 50)
    private String channelCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20)
    private Status status;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;
}