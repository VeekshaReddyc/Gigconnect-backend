package com.gigconnect.gigconnectbackend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gigconnect.gigconnectbackend.entity.Gig;
import com.gigconnect.gigconnectbackend.entity.GigApplication;
import com.gigconnect.gigconnectbackend.service.GigService;
import com.gigconnect.gigconnectbackend.service.PaymentService;

@RestController
@RequestMapping("/api/gigs")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175"
})
public class GigController {

    private final GigService gigService;
    private final PaymentService paymentService;

    public GigController(
            GigService gigService,
            PaymentService paymentService) {
        this.gigService = gigService;
        this.paymentService = paymentService;
    }

    // Create a new draft gig
    @PostMapping
    public ResponseEntity<?> createGig(
            @RequestBody(required = false) Gig gig,
            @RequestParam Long clientId) {
        try {
            if (gig == null) {
                return badRequest("Gig details are required.");
            }

            if (isBlank(gig.getTitle())) {
                return badRequest("Gig title is required.");
            }

            if (isBlank(gig.getDescription())) {
                return badRequest("Gig description is required.");
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(gigService.createGig(gig, clientId));

        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Publish a draft gig
    @PutMapping("/{gigId}/publish")
    public ResponseEntity<?> publishGig(
            @PathVariable Long gigId,
            @RequestParam Long clientId) {
        try {
            return ResponseEntity.ok(
                    gigService.publishGig(gigId, clientId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // View published gigs
    @GetMapping
    public ResponseEntity<?> getPublishedGigs() {
        try {
            return ResponseEntity.ok(
                    gigService.getPublishedGigs());
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // View gigs created by a client
    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getClientGigs(
            @PathVariable Long clientId) {
        try {
            return ResponseEntity.ok(
                    gigService.getClientGigs(clientId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Legacy accept endpoint
    @PutMapping("/{gigId}/accept")
    public ResponseEntity<?> acceptGig(
            @PathVariable Long gigId,
            @RequestParam Long freelancerId) {
        try {
            return ResponseEntity.ok(
                    gigService.acceptGig(gigId, freelancerId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Freelancer applies for a gig
    @PostMapping("/{gigId}/apply")
    public ResponseEntity<?> applyForGig(
            @PathVariable Long gigId,
            @RequestParam Long freelancerId,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            if (request == null) {
                return badRequest("Application details are required.");
            }

            Object proposalValue = request.get("proposal");

            if (!(proposalValue instanceof String)) {
                return badRequest("A proposal is required.");
            }

            String proposal = ((String) proposalValue).trim();

            if (proposal.isEmpty()) {
                return badRequest("A proposal cannot be empty.");
            }

            Object budgetValue = request.get("proposedBudget");
            Double proposedBudget = null;

            if (budgetValue instanceof Number) {
                proposedBudget = ((Number) budgetValue).doubleValue();
            } else if (budgetValue instanceof String
                    && !((String) budgetValue).isBlank()) {
                try {
                    proposedBudget =
                            Double.parseDouble((String) budgetValue);
                } catch (NumberFormatException e) {
                    return badRequest(
                            "Proposed budget must be a valid number.");
                }
            }

            if (proposedBudget != null
                    && (!Double.isFinite(proposedBudget)
                    || proposedBudget <= 0)) {
                return badRequest(
                        "Proposed budget must be greater than zero.");
            }

            GigApplication application = gigService.applyForGig(
                    gigId, freelancerId, proposal, proposedBudget);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(application);

        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Client views applications for a gig
    @GetMapping("/{gigId}/applications")
    public ResponseEntity<?> getGigApplications(
            @PathVariable Long gigId,
            @RequestParam Long clientId) {
        try {
            return ResponseEntity.ok(
                    gigService.getGigApplications(gigId, clientId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Client selects a freelancer
    @PutMapping("/{gigId}/select/{applicationId}")
    public ResponseEntity<?> selectApplicant(
            @PathVariable Long gigId,
            @PathVariable Long applicationId,
            @RequestParam Long clientId) {
        try {
            return ResponseEntity.ok(
                    gigService.selectApplicant(
                            gigId, applicationId, clientId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Freelancer views assigned gigs
    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<?> getFreelancerGigs(
            @PathVariable Long freelancerId) {
        try {
            return ResponseEntity.ok(
                    gigService.getFreelancerGigs(freelancerId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Freelancer views their applications
    @GetMapping("/applications/freelancer/{freelancerId}")
    public ResponseEntity<?> getFreelancerApplications(
            @PathVariable Long freelancerId) {
        try {
            return ResponseEntity.ok(
                    gigService.getFreelancerApplications(freelancerId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Freelancer submits completed work
    @PutMapping("/{gigId}/submit")
    public ResponseEntity<?> submitWork(
            @PathVariable Long gigId,
            @RequestParam Long freelancerId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            if (request == null) {
                return badRequest("Submission details are required.");
            }

            String submissionDetails = request.get("submissionDetails");

            if (isBlank(submissionDetails)) {
                return badRequest(
                        "Please provide the submitted work details or link.");
            }

            return ResponseEntity.ok(
                    gigService.submitWork(
                            gigId, freelancerId, submissionDetails.trim()));

        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Client approves submitted work
    @PutMapping("/{gigId}/approve")
    public ResponseEntity<?> approveWork(
            @PathVariable Long gigId,
            @RequestParam Long clientId) {
        try {
            return ResponseEntity.ok(
                    gigService.approveWork(gigId, clientId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Backward-compatible completion endpoint
    @PutMapping("/{gigId}/complete")
    public ResponseEntity<?> completeGig(
            @PathVariable Long gigId,
            @RequestParam Long clientId) {
        try {
            return ResponseEntity.ok(
                    gigService.approveWork(gigId, clientId));
        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Client requests revisions from the assigned freelancer
    @PutMapping("/{gigId}/revision")
    public ResponseEntity<?> requestRevision(
            @PathVariable Long gigId,
            @RequestParam Long clientId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            if (request == null) {
                return badRequest("Revision comments are required.");
            }

            String comments = request.get("comments");

            if (isBlank(comments)) {
                return badRequest(
                        "Please explain what needs to be revised.");
            }

            comments = comments.trim();

            if (comments.length() > 2000) {
                return badRequest(
                        "Revision comments cannot exceed 2000 characters.");
            }

            return ResponseEntity.ok(
                    gigService.requestRevision(
                            gigId, clientId, comments));

        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    // Client records payment after approving work
    @PutMapping("/{gigId}/payment")
    public ResponseEntity<?> confirmPayment(
            @PathVariable Long gigId,
            @RequestParam Long clientId,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            if (request == null) {
                return badRequest("Payment details are required.");
            }

            Object amountValue = request.get("amount");

            if (!(amountValue instanceof Number)) {
                return badRequest(
                        "A valid payment amount is required.");
            }

            Double amount = ((Number) amountValue).doubleValue();

            if (!Double.isFinite(amount) || amount <= 0) {
                return badRequest(
                        "Payment amount must be greater than zero.");
            }

            return ResponseEntity.ok(
                    paymentService.recordPayment(
                            gigId, clientId, amount));

        } catch (RuntimeException e) {
            return errorResponse(e);
        }
    }

    private ResponseEntity<Map<String, String>> errorResponse(
            RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", errorMessage(e)));
    }

    private ResponseEntity<Map<String, String>> badRequest(
            String message) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", message));
    }

    private String errorMessage(RuntimeException e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return "The request could not be completed. Check the server logs.";
        }

        return e.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}