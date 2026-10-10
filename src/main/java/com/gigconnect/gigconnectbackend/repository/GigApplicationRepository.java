package com.gigconnect.gigconnectbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigconnect.gigconnectbackend.entity.GigApplication;

public interface GigApplicationRepository
        extends JpaRepository<GigApplication, Long> {

    // Find all applications for a particular gig
    List<GigApplication> findByGigId(Long gigId);

    // Find all applications submitted by a freelancer
    List<GigApplication> findByFreelancerId(Long freelancerId);

    // Check whether a freelancer has already applied
    boolean existsByGigIdAndFreelancerId(
            Long gigId,
            Long freelancerId
    );

    // Find a particular freelancer's application for a gig
    Optional<GigApplication> findByGigIdAndFreelancerId(
            Long gigId,
            Long freelancerId
    );
}