package com.gigconnect.gigconnectbackend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigconnect.gigconnectbackend.entity.Gig;
import com.gigconnect.gigconnectbackend.entity.GigApplication;
import com.gigconnect.gigconnectbackend.entity.User;
import com.gigconnect.gigconnectbackend.repository.GigApplicationRepository;
import com.gigconnect.gigconnectbackend.repository.GigRepository;
import com.gigconnect.gigconnectbackend.repository.UserRepository;

@Service
public class GigService {

    private final GigRepository gigRepository;
    private final UserRepository userRepository;
    private final GigApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    // Fixed constructor: initialize every final field.
    public GigService(
            GigRepository gigRepository,
            UserRepository userRepository,
            GigApplicationRepository applicationRepository,
            NotificationService notificationService) {

        this.gigRepository = gigRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    // Create a new draft gig
    @Transactional
    public Gig createGig(Gig gig, Long clientId) {

        User client = userRepository.findById(clientId)
                .orElseThrow(() ->
                        new RuntimeException("Client not found"));

        if (!"CLIENT".equalsIgnoreCase(client.getRole())) {
            throw new RuntimeException("Only clients can create gigs");
        }

        gig.setId(null);
        gig.setClient(client);
        gig.setFreelancer(null);
        gig.setStatus("DRAFT");
        gig.setSubmissionDetails(null);
        gig.setSubmittedAt(null);
        gig.setReviewComments(null);
        gig.setReviewedAt(null);
        gig.setPaymentStatus("NOT_PAID");
        gig.setPaymentAmount(null);
        gig.setPaidAt(null);

        return gigRepository.save(gig);
    }

    // Publish a draft gig
    @Transactional
    public Gig publishGig(Long gigId, Long clientId) {

        Gig gig = getGigOrThrow(gigId);
        verifyClientOwnership(gig, clientId);

        if (!"DRAFT".equalsIgnoreCase(gig.getStatus())) {
            throw new RuntimeException("Only draft gigs can be published");
        }

        gig.setStatus("PUBLISHED");

        return gigRepository.save(gig);
    }

    // Browse published, assigned, submitted, and completed gigs
    public List<Gig> getPublishedGigs() {

        return gigRepository.findByStatusIn(
                List.of("PUBLISHED", "ACCEPTED", "SUBMITTED", "COMPLETED"));
    }

    // View all gigs belonging to a client
    public List<Gig> getClientGigs(Long clientId) {
        return gigRepository.findByClientId(clientId);
    }

    // Freelancer applies for a gig
    @Transactional
    public GigApplication applyForGig(
            Long gigId,
            Long freelancerId,
            String proposal,
            Double proposedBudget) {

        Gig gig = getGigOrThrow(gigId);

        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() ->
                        new RuntimeException("Freelancer not found"));

        if (!"FREELANCER".equalsIgnoreCase(freelancer.getRole())) {
            throw new RuntimeException("Only freelancers can apply for gigs");
        }

        if (!"PUBLISHED".equalsIgnoreCase(gig.getStatus())
                || gig.getFreelancer() != null) {
            throw new RuntimeException(
                    "This gig is no longer accepting applications");
        }

        if (gig.getClient() != null
                && gig.getClient().getId().equals(freelancerId)) {
            throw new RuntimeException("You cannot apply for your own gig");
        }

        if (applicationRepository.existsByGigIdAndFreelancerId(
                gigId, freelancerId)) {
            throw new RuntimeException("You have already applied for this gig");
        }

        if (proposal == null || proposal.isBlank()) {
            throw new RuntimeException("Please provide a proposal");
        }

        if (proposal.length() > 2000) {
            throw new RuntimeException("Proposal cannot exceed 2000 characters");
        }

        if (proposedBudget != null
                && (!Double.isFinite(proposedBudget) || proposedBudget <= 0)) {
            throw new RuntimeException("Proposed budget must be greater than zero");
        }

        GigApplication application = new GigApplication();
        application.setGig(gig);
        application.setFreelancer(freelancer);
        application.setProposal(proposal.trim());
        application.setProposedBudget(proposedBudget);
        application.setStatus("PENDING");
        application.setAppliedAt(LocalDateTime.now());

        return applicationRepository.save(application);
    }

    // Client views applications for their own gig
    public List<GigApplication> getGigApplications(
            Long gigId,
            Long clientId) {

        Gig gig = getGigOrThrow(gigId);
        verifyClientOwnership(gig, clientId);

        return applicationRepository.findByGigId(gigId);
    }

    // Client selects an applicant and rejects the others
    @Transactional
    public Gig selectApplicant(
            Long gigId,
            Long applicationId,
            Long clientId) {

        Gig gig = getGigOrThrow(gigId);
        verifyClientOwnership(gig, clientId);

        if (!"PUBLISHED".equalsIgnoreCase(gig.getStatus())
                || gig.getFreelancer() != null) {
            throw new RuntimeException(
                    "This gig has already been assigned or is not available");
        }

        GigApplication selected = applicationRepository.findById(applicationId)
                .orElseThrow(() ->
                        new RuntimeException("Application not found"));

        if (selected.getGig() == null
                || !selected.getGig().getId().equals(gigId)) {
            throw new RuntimeException(
                    "This application does not belong to this gig");
        }

        if (!"PENDING".equalsIgnoreCase(selected.getStatus())) {
            throw new RuntimeException(
                    "This application has already been processed");
        }

        List<GigApplication> applications =
                applicationRepository.findByGigId(gigId);

        for (GigApplication application : applications) {
            if (application.getId().equals(applicationId)) {
                application.setStatus("ACCEPTED");
            } else if ("PENDING".equalsIgnoreCase(application.getStatus())) {
                application.setStatus("REJECTED");
            }
        }

        applicationRepository.saveAll(applications);

        gig.setFreelancer(selected.getFreelancer());
        gig.setStatus("ACCEPTED");
        gig.setReviewComments(null);
        gig.setSubmissionDetails(null);
        gig.setSubmittedAt(null);
        gig.setReviewedAt(null);
        gig.setPaymentStatus("NOT_PAID");
        gig.setPaidAt(null);

        gig.setPaymentAmount(
                selected.getProposedBudget() != null
                        ? selected.getProposedBudget()
                        : gig.getMaxBudget());

        Gig savedGig = gigRepository.save(gig);

        // Notify the selected freelancer.
        notificationService.notifyFreelancer(
                selected.getFreelancer().getId(),
                savedGig.getId(),
                "Application Accepted",
                "The client selected your application for \""
                        + savedGig.getTitle()
                        + "\". You can now start working on the gig.");

        return savedGig;
    }

    // Disable direct acceptance without client selection
    public Gig acceptGig(Long gigId, Long freelancerId) {
        throw new RuntimeException(
                "Apply for the gig first. The client must select your application.");
    }

    // Assigned freelancer submits work or revised work
    @Transactional
    public Gig submitWork(
            Long gigId,
            Long freelancerId,
            String submissionDetails) {

        Gig gig = getGigOrThrow(gigId);

        if (gig.getFreelancer() == null
                || !gig.getFreelancer().getId().equals(freelancerId)) {
            throw new RuntimeException(
                    "Only the assigned freelancer can submit work");
        }

        if (!"ACCEPTED".equalsIgnoreCase(gig.getStatus())) {
            throw new RuntimeException(
                    "The gig must be assigned and awaiting submission");
        }

        if (submissionDetails == null || submissionDetails.isBlank()) {
            throw new RuntimeException(
                    "Please provide your work details or delivery link");
        }

        if (submissionDetails.length() > 2000) {
            throw new RuntimeException(
                    "Submission details cannot exceed 2000 characters");
        }

        gig.setSubmissionDetails(submissionDetails.trim());
        gig.setSubmittedAt(LocalDateTime.now());
        gig.setStatus("SUBMITTED");

        return gigRepository.save(gig);
    }

    // Client approves submitted work
    @Transactional
    public Gig approveWork(Long gigId, Long clientId) {

        Gig gig = getGigOrThrow(gigId);
        verifyClientOwnership(gig, clientId);

        if (!"SUBMITTED".equalsIgnoreCase(gig.getStatus())) {
            throw new RuntimeException("Only submitted work can be approved");
        }

        gig.setReviewedAt(LocalDateTime.now());
        gig.setReviewComments("Work approved");
        gig.setStatus("COMPLETED");

        if (!"PAID".equalsIgnoreCase(gig.getPaymentStatus())) {
            gig.setPaymentStatus("PAYMENT_PENDING");
        }

        Gig savedGig = gigRepository.save(gig);

        // Notify the freelancer that the work was approved.
        if (savedGig.getFreelancer() != null) {
            notificationService.notifyFreelancer(
                    savedGig.getFreelancer().getId(),
                    savedGig.getId(),
                    "Work Approved",
                    "The client approved your submitted work for \""
                            + savedGig.getTitle()
                            + "\". Payment can now be recorded.");
        }

        return savedGig;
    }

    // Client requests revisions
    @Transactional
    public Gig requestRevision(
            Long gigId,
            Long clientId,
            String comments) {

        Gig gig = getGigOrThrow(gigId);
        verifyClientOwnership(gig, clientId);

        if (!"SUBMITTED".equalsIgnoreCase(gig.getStatus())) {
            throw new RuntimeException(
                    "Revisions can only be requested for submitted work");
        }

        if (comments == null || comments.isBlank()) {
            throw new RuntimeException("Please provide revision instructions");
        }

        comments = comments.trim();

        if (comments.length() > 2000) {
            throw new RuntimeException(
                    "Revision comments cannot exceed 2000 characters");
        }

        gig.setReviewComments(comments);
        gig.setReviewedAt(LocalDateTime.now());
        gig.setStatus("ACCEPTED");

        Gig savedGig = gigRepository.save(gig);

        // Notify the freelancer and include the revision instructions.
        if (savedGig.getFreelancer() != null) {
            notificationService.notifyFreelancer(
                    savedGig.getFreelancer().getId(),
                    savedGig.getId(),
                    "Revision Requested",
                    "The client requested revisions for \""
                            + savedGig.getTitle()
                            + "\": " + comments);
        }

        return savedGig;
    }

    // Record payment after the client independently confirms it
    @Transactional
    public Gig confirmPayment(
            Long gigId,
            Long clientId,
            Double amount) {

        Gig gig = getGigOrThrow(gigId);
        verifyClientOwnership(gig, clientId);

        if (!"COMPLETED".equalsIgnoreCase(gig.getStatus())) {
            throw new RuntimeException(
                    "Approve the submitted work before recording payment");
        }

        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw new RuntimeException(
                    "Payment amount must be greater than zero");
        }

        if (gig.getPaymentAmount() == null) {
            throw new RuntimeException(
                    "No agreed payment amount has been set for this gig");
        }

        if (Double.compare(amount, gig.getPaymentAmount()) != 0) {
            throw new RuntimeException(
                    "Payment amount does not match the agreed amount");
        }

        if ("PAID".equalsIgnoreCase(gig.getPaymentStatus())) {
            throw new RuntimeException("Payment has already been recorded");
        }

        // This records the payment status; it does not transfer money.
        gig.setPaymentStatus("PAID");
        gig.setPaidAt(LocalDateTime.now());

        Gig savedGig = gigRepository.save(gig);

        // Notify the freelancer that payment has been recorded.
        if (savedGig.getFreelancer() != null) {
            notificationService.notifyFreelancer(
                    savedGig.getFreelancer().getId(),
                    savedGig.getId(),
                    "Payment Recorded",
                    "Payment of ₹" + savedGig.getPaymentAmount()
                            + " has been recorded for \""
                            + savedGig.getTitle() + "\".");
        }

        return savedGig;
    }

    // View gigs assigned to a freelancer
    public List<Gig> getFreelancerGigs(Long freelancerId) {

        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() ->
                        new RuntimeException("Freelancer not found"));

        if (!"FREELANCER".equalsIgnoreCase(freelancer.getRole())) {
            throw new RuntimeException(
                    "Only freelancers can view their assigned gigs");
        }

        return gigRepository.findByFreelancerId(freelancerId);
    }

    // View applications submitted by a freelancer
    public List<GigApplication> getFreelancerApplications(
            Long freelancerId) {

        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() ->
                        new RuntimeException("Freelancer not found"));

        if (!"FREELANCER".equalsIgnoreCase(freelancer.getRole())) {
            throw new RuntimeException(
                    "Only freelancers can view their applications");
        }

        return applicationRepository.findByFreelancerId(freelancerId);
    }

    // Find a gig or throw an error
    private Gig getGigOrThrow(Long gigId) {

        return gigRepository.findById(gigId)
                .orElseThrow(() ->
                        new RuntimeException("Gig not found"));
    }

    // Verify that the client owns the gig
    private void verifyClientOwnership(Gig gig, Long clientId) {

        if (gig.getClient() == null
                || !gig.getClient().getId().equals(clientId)) {
            throw new RuntimeException(
                    "You can only manage your own gigs");
        }
    }
}