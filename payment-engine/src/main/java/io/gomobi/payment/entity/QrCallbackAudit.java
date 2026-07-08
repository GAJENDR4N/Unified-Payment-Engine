package io.gomobi.payment.entity;

import io.gomobi.payment.core.enums.CallbackType;
import io.gomobi.payment.core.enums.ProcessResult;
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
 * Raw inbound provider callback audit record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "QR_CALLBACK_AUDIT")
public class QrCallbackAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "QR_CALLBACK_AUDIT_ID")
    private Long qrCallbackAuditId;

    @Column(name = "PAYMENT_TRANSACTION_FK", nullable = false)
    private Long paymentTransactionFk;

    @Enumerated(EnumType.STRING)
    @Column(name = "CALLBACK_TYPE", nullable = false)
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
    @Column(name = "PROCESS_RESULT")
    private ProcessResult processResult;

    @Column(name = "HTTP_STATUS")
    private Integer httpStatus;

    @Column(name = "RETRY_COUNT")
    private Integer retryCount;

    @Column(name = "CREATED_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime createdTimestamp;
}

