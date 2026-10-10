package com.gigconnect.gigconnectbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gigconnect.gigconnectbackend.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByGigId(Long gigId);

    boolean existsByGigId(Long gigId);

    List<Payment> findAllByClientId(Long clientId);

    List<Payment> findAllByFreelancerId(Long freelancerId);

    List<Payment> findByClientIdOrderByPaymentDateDesc(Long clientId);

    List<Payment> findByFreelancerIdOrderByPaymentDateDesc(Long freelancerId);
}