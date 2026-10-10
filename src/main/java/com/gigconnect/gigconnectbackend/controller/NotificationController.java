package com.gigconnect.gigconnectbackend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gigconnect.gigconnectbackend.entity.Notification;
import com.gigconnect.gigconnectbackend.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175"
})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<Notification>> getFreelancerNotifications(
            @PathVariable Long freelancerId) {

        return ResponseEntity.ok(
                notificationService.getFreelancerNotifications(freelancerId));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam Long freelancerId) {

        boolean updated = notificationService.markAsRead(
                notificationId, freelancerId);

        if (!updated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                Map.of("message", "Notification marked as read"));
    }
}