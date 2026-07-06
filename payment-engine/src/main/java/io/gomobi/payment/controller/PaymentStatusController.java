package io.gomobi.payment.controller;

import io.gomobi.payment.dto.StatusEnquiryResponse;
import io.gomobi.payment.service.PaymentStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;

/** Status enquiry - see {@link PaymentController} for payment creation. */
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentStatusController {

    private final PaymentStatusService paymentStatusService;

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<StatusEnquiryResponse> getStatus(@PathVariable String transactionId) throws NoSuchAlgorithmException {
        return ResponseEntity.ok(paymentStatusService.getStatus(transactionId));
    }
}
