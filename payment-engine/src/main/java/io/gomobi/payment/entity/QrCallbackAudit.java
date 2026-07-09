package io.gomobi.payment.entity;

import io.gomobi.payment.core.enums.CallbackType;
import io.gomobi.payment.core.enums.ProcessResult;
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
 * Raw inbound provider callback audit record. Also reused by Payment Engine
 * for Hello Clever webhook audit persistence.
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
        name = "QR_CALLBACK_AUDIT",
        indexes = {
                @Index(name = "IDX_QCA_PAYMENT_TRANSACTION", columnList = "PAYMENT_TRANSACTION_FK"),
                @Index(name = "IDX_QCA_RECEIVED_TIMESTAMP", columnList = "RECEIVED_TIMESTAMP"),
                @Index(name = "IDX_QCA_PROCESSED_FLAG", columnList = "PROCESSED_FLAG"),
                @Index(name = "IDX_QCA_CALLBACK_TYPE", columnList = "CALLBACK_TYPE")
        }
)
public class QrCallbackAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", columnDefinition = "BIGINT UNSIGNED")
    @EqualsAndHashCode.Include
    private BigInteger id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "PAYMENT_TRANSACTION_FK",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_QCA_PT")
    )
    @ToString.Exclude
    private PaymentTransaction paymentTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "CALLBACK_TYPE", nullable = false, length = 30)
    private CallbackType callbackType;

    @Column(name = "REQUEST_HEADERS", columnDefinition = "json")
    private String requestHeaders;

    @Column(name = "REQUEST_BODY", columnDefinition = "json")
    private String requestBody;

    @Column(name = "RECEIVED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime receivedTimestamp;

    @Column(name = "PROCESSED_FLAG")
    private Boolean processedFlag;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROCESS_RESULT", length = 20)
    private ProcessResult processResult;

    @Column(name = "HTTP_STATUS")
    private Integer httpStatus;

    @Column(name = "RETRY_COUNT")
    private Integer retryCount;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;
}