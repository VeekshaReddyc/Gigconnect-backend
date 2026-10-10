
package com.gigconnect.gigconnectbackend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigconnect.gigconnectbackend.entity.Gig;
import com.gigconnect.gigconnectbackend.entity.Payment;
import com.gigconnect.gigconnectbackend.repository.GigRepository;
import com.gigconnect.gigconnectbackend.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final GigRepository gigRepository;
    private final NotificationService notificationService;

    public PaymentService(
            PaymentRepository paymentRepository,
            GigRepository gigRepository,
            NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.gigRepository = gigRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Payment recordPayment(Long gigId, Long clientId, Double amount) {

        Gig gig = gigRepository.findById(gigId)
                .orElseThrow(() -> new RuntimeException("Gig not found"));

        if (gig.getClient() == null
                || !gig.getClient().getId().equals(clientId)) {
            throw new RuntimeException(
                    "You can only pay for your own gigs");
        }

        if (!"COMPLETED".equalsIgnoreCase(gig.getStatus())) {
            throw new RuntimeException(
                    "Work must be approved before recording payment");
        }

        if (gig.getFreelancer() == null) {
            throw new RuntimeException(
                    "No freelancer is assigned to this gig");
        }

        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw new RuntimeException(
                    "Payment amount must be greater than zero");
        }

        if (gig.getPaymentAmount() == null
                || Double.compare(amount, gig.getPaymentAmount()) != 0) {
            throw new RuntimeException(
                    "Payment must match the agreed amount");
        }

        if ("PAID".equalsIgnoreCase(gig.getPaymentStatus())
                || paymentRepository.existsByGigId(gigId)) {
            throw new RuntimeException(
                    "Payment has already been recorded");
        }

        Long freelancerId = gig.getFreelancer().getId();

        Payment payment = new Payment();
        payment.setGigId(gigId);
        payment.setClientId(clientId);
        payment.setFreelancerId(freelancerId);
        payment.setAmount(amount);
        payment.setStatus("RECORDED");

        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        gig.setPaymentStatus("PAID");
        gig.setPaidAt(LocalDateTime.now());
        gigRepository.save(gig);

        notificationService.notifyFreelancer(
                freelancerId,
                gigId,
                "Payment Recorded",
                "A payment of ₹"
                        + String.format("%.2f", amount)
                        + " has been recorded for gig: "
                        + gig.getTitle()
                        + ". Payment ID: "
                        + savedPayment.getId()
        );

        return savedPayment;
    }

    public List<Payment> getClientPayments(Long clientId) {
        return paymentRepository
                .findByClientIdOrderByPaymentDateDesc(clientId);
    }

    public List<Payment> getFreelancerPayments(Long freelancerId) {
        return paymentRepository
                .findByFreelancerIdOrderByPaymentDateDesc(freelancerId);
    }
}

