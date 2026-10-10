package com.gigconnect.gigconnectbackend.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "gig_applications",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_gig_freelancer",
            columnNames = {"gig_id", "freelancer_id"}
        )
    }
)
public class GigApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gig_id", nullable = false)
    @JsonIgnore
    private Gig gig;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    @Column(length = 2000)
    private String proposal;

    private Double proposedBudget;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();

    public GigApplication() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Gig getGig() {
        return gig;
    }

    public void setGig(Gig gig) {
        this.gig = gig;
    }

    public User getFreelancer() {
        return freelancer;
    }

    public void setFreelancer(User freelancer) {
        this.freelancer = freelancer;
    }

    public String getProposal() {
        return proposal;
    }

    public void setProposal(String proposal) {
        this.proposal = proposal;
    }

    public Double getProposedBudget() {
        return proposedBudget;
    }

    public void setProposedBudget(Double proposedBudget) {
        this.proposedBudget = proposedBudget;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}