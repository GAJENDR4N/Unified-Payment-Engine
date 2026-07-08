package io.gomobi.payment.controller;

import io.gomobi.payment.dto.CreatePaymentRequest;
import io.gomobi.payment.dto.CreatePaymentResponse;
import io.gomobi.payment.service.PaymentCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Payment creation - see {@link PaymentStatusController} for status enquiry. */
@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentCreationService paymentCreationService;

    @PostMapping
    public ResponseEntity<CreatePaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("event=REQUEST_VALIDATED endpoint=/v1/payments result=SUCCESS referenceId={}",
                request.getReferenceId());
        CreatePaymentResponse response = paymentCreationService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
