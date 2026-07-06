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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Read-mostly mapping of the PAYMENT_METHOD lookup table. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PAYMENT_METHOD")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_METHOD_ID")
    private Long paymentMethodId;

    @Column(name = "PAYMENT_METHOD_CODE", nullable = false, unique = true)
    private String paymentMethodCode;

    @Column(name = "PAYMENT_METHOD_NAME", nullable = false)
    private String paymentMethodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_METHOD_TYPE", nullable = false)
    private PaymentMethodType paymentMethodType;

    @Column(name = "CHANNEL_CODE", nullable = false, unique = true)
    private String channelCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime updatedTimestamp;
}
