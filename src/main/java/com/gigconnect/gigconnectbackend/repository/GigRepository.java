package com.gigconnect.gigconnectbackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigconnect.gigconnectbackend.entity.Gig;

public interface GigRepository extends JpaRepository<Gig, Long> {

    // Find gigs by a single status
    List<Gig> findByStatus(String status);

    // Find gigs by multiple statuses
    List<Gig> findByStatusIn(List<String> statuses);

    // Find gigs created by a client
    List<Gig> findByClientId(Long clientId);

    // Find gigs assigned to a freelancer
    List<Gig> findByFreelancerId(Long freelancerId);
}