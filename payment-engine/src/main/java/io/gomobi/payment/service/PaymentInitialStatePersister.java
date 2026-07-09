package io.gomobi.payment.service;

import io.gomobi.payment.core.enums.QrMode;
import io.gomobi.payment.core.enums.QrType;
import io.gomobi.payment.core.enums.TransactionStatus;
import io.gomobi.payment.dto.CreatePaymentRequest;
import io.gomobi.payment.dto.CustomerInfo;
import io.gomobi.payment.entity.Merchant;
import io.gomobi.payment.entity.PaymentTransaction;
import io.gomobi.payment.entity.QrTransactionDetails;
import io.gomobi.payment.entity.SubMerchant;
import io.gomobi.payment.exception.ErrorCode;
import io.gomobi.payment.exception.PaymentEngineException;
import io.gomobi.payment.repository.PaymentTransactionRepository;
import io.gomobi.payment.repository.QrTransactionDetailsRepository;
import io.gomobi.payment.util.DbPersistLog;
import io.gomobi.payment.util.JsonUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

/**
 * Persists the pre-provider-call state (PAYMENT_TRANSACTION +
 * QR_TRANSACTION_DETAILS) as a single atomic unit.
 * <p>
 * Both rows are written inside one DB transaction: either both commit, or
 * neither does. Without this, a failure on the QR_TRANSACTION_DETAILS
 * insert (after PAYMENT_TRANSACTION already committed) would leave an
 * orphaned PAYMENT_TRANSACTION row stuck in INITIATED with no way to
 * recover it.
 * <p>
 * This is deliberately its own Spring bean rather than a private method on
 * {@link PaymentCreationService}: {@code @Transactional} is proxy-based, so
 * calling it via {@code this.method()} from within the same class would
 * silently not participate in a transaction at all. Keeping it in a
 * separate bean makes the atomic boundary explicit and unambiguous, and
 * keeps the outbound provider HTTP call in {@link PaymentCreationService}
 * outside of any DB transaction, where it belongs - a network call to an
 * external provider should never hold a DB connection open.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentInitialStatePersister {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final QrTransactionDetailsRepository qrTransactionDetailsRepository;
    private final EntityManager entityManager;

    @Transactional
    public PaymentTransaction persist(CreatePaymentRequest request, Merchant merchant, SubMerchant subMerchant,
                                      ResolvedHost resolvedHost, String transactionId) {
        PaymentTransaction transaction = persistTransaction(request, merchant, subMerchant, resolvedHost, transactionId);
        persistQrDetails(request, transaction);
        return transaction;
    }

    private PaymentTransaction persistTransaction(CreatePaymentRequest request, Merchant merchant,
                                                  SubMerchant subMerchant, ResolvedHost resolvedHost,
                                                  String transactionId) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionId(transactionId)
                .merchantRefNo(request.getReferenceId())
                .transactionStatus(TransactionStatus.INITIATED)
                .providerConfiguration(resolvedHost.providerConfiguration())
                .merchant(merchant)
                .subMerchant(subMerchant)
                .paymentMethod(resolvedHost.paymentMethod())
                .transactionAmount(BigInteger.valueOf(request.getAmount()))
                .currencyCode(request.getCurrency())
                .build();

        long start = System.currentTimeMillis();
        try {
            transaction = paymentTransactionRepository.save(transaction);
            // Force INSERT and refresh from DB so default/generated columns are visible immediately.
            entityManager.flush();
            entityManager.refresh(transaction);
            DbPersistLog.log(log, "PAYMENT_TRANSACTION", transactionId, System.currentTimeMillis() - start);
            return transaction;
        } catch (Exception e) {
            DbPersistLog.logError(log, "PAYMENT_TRANSACTION", transactionId, System.currentTimeMillis() - start, e);
            throw new PaymentEngineException(ErrorCode.DB_PERSIST_ERROR, "Failed to persist payment transaction", e);
        }
    }

    private void persistQrDetails(CreatePaymentRequest request, PaymentTransaction transaction) {
        CustomerInfo customer = request.getCustomer();

        QrTransactionDetails details = QrTransactionDetails.builder()
                .paymentTransaction(transaction)
                .qrType(QrType.DYNAMIC)
                .qrMode(QrMode.MERCHANT_PRESENTED)
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null ? customer.getName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .metadata(JsonUtils.toJson(request.getMetadata()))
                .build();

        long start = System.currentTimeMillis();
        try {
            qrTransactionDetailsRepository.save(details);
            DbPersistLog.log(log, "QR_TRANSACTION_DETAILS", transaction.getTransactionId(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            DbPersistLog.logError(log, "QR_TRANSACTION_DETAILS", transaction.getTransactionId(),
                    System.currentTimeMillis() - start, e);
            // RuntimeException -> triggers rollback of the whole @Transactional method,
            // so the PAYMENT_TRANSACTION insert above is rolled back too.
            throw new PaymentEngineException(ErrorCode.DB_PERSIST_ERROR, "Failed to persist QR transaction details", e);
        }
    }
}