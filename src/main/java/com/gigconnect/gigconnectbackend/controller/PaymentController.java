package com.gigconnect.gigconnectbackend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigconnect.gigconnectbackend.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175"
})
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // View payment history for a client
    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getClientPayments(
            @PathVariable Long clientId) {
        try {
            return ResponseEntity.ok(
                    paymentService.getClientPayments(clientId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage(e)));
        }
    }

    // View payment history for a freelancer
    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<?> getFreelancerPayments(
            @PathVariable Long freelancerId) {
        try {
            return ResponseEntity.ok(
                    paymentService.getFreelancerPayments(freelancerId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage(e)));
        }
    }

    private String errorMessage(RuntimeException e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return "Unable to retrieve payment history.";
        }

        return e.getMessage();
    }
}